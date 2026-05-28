package com.chatlybox.ingestion;

import com.chatlybox.nativebridge.LlamaLib;
import com.chatlybox.sources.DocumentSource;
import com.chatlybox.sources.DocumentSourceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
public class DocumentIngestionService {
  private final DocumentSourceRepository sources;
  private final LlamaLib llamaLib;
  private final ObjectMapper objectMapper;
  private final IngestionProperties properties;

  public DocumentIngestionService(
      DocumentSourceRepository sources,
      LlamaLib llamaLib,
      ObjectMapper objectMapper,
      IngestionProperties properties) {
    this.sources = sources;
    this.llamaLib = llamaLib;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  public SyncResult sync(UUID sourceId) {
    DocumentSource source = sources.findById(sourceId).orElseThrow();
    source.status = "RUNNING";
    source.lastError = null;
    sources.save(source);

    try {
      List<LoadedDocument> documents =
          "S3".equalsIgnoreCase(source.type) ? loadS3(source) : loadLocal(source);
      int indexed = 0;
      for (LoadedDocument document : documents) {
        String text = extractText(document);
        if (text.isBlank()) {
          continue;
        }
        List<String> chunks = chunk(text);
        String request =
            objectMapper.writeValueAsString(
                Map.of(
                    "dbPath", properties.lancedbUri(),
                    "embeddingModelPath", properties.embeddingModelPath(),
                    "documentId", sha256(document.uri()),
                    "title", document.title(),
                    "uri", document.uri(),
                    "chunks", chunks));
        llamaLib.index(request);
        indexed += chunks.size();
      }
      source.status = "DONE";
      source.lastSyncedAt = Instant.now();
      sources.save(source);
      return new SyncResult(documents.size(), indexed);
    } catch (Exception error) {
      source.status = "FAILED";
      source.lastError = error.getMessage();
      sources.save(source);
      throw new IllegalStateException("Document sync failed", error);
    }
  }

  private List<LoadedDocument> loadLocal(DocumentSource source) throws IOException {
    JsonNode config = objectMapper.readTree(source.config);
    Path root = Path.of(config.path("path").asText()).toAbsolutePath().normalize();
    try (Stream<Path> paths = Files.walk(root)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(path -> isSupported(path.getFileName().toString()))
          .map(path -> readLocal(root, path))
          .toList();
    }
  }

  private LoadedDocument readLocal(Path root, Path file) {
    try {
      return new LoadedDocument(
          file.getFileName().toString(),
          root.relativize(file).toString(),
          file.toString(),
          Files.readAllBytes(file));
    } catch (IOException error) {
      throw new IllegalStateException("Cannot read " + file, error);
    }
  }

  private List<LoadedDocument> loadS3(DocumentSource source) throws IOException {
    JsonNode config = objectMapper.readTree(source.config);
    String bucket = config.path("bucket").asText();
    String prefix = config.path("prefix").asText("");
    S3Client client = s3Client(config);
    List<LoadedDocument> documents = new ArrayList<>();
    String token = null;
    do {
      var listed =
          client.listObjectsV2(
              ListObjectsV2Request.builder()
                  .bucket(bucket)
                  .prefix(prefix)
                  .continuationToken(token)
                  .build());
      token = listed.nextContinuationToken();
      for (S3Object object : listed.contents()) {
        if (object.key().endsWith("/") || !isSupported(object.key())) {
          continue;
        }
        ResponseBytes<?> bytes =
            client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(object.key()).build());
        documents.add(
            new LoadedDocument(
                Path.of(object.key()).getFileName().toString(),
                object.key(),
                "s3://" + bucket + "/" + object.key(),
                bytes.asByteArray()));
      }
    } while (token != null);
    return documents;
  }

  private S3Client s3Client(JsonNode config) {
    var builder = S3Client.builder().region(Region.of(config.path("region").asText("us-east-1")));
    if (config.hasNonNull("endpoint")) {
      builder.endpointOverride(java.net.URI.create(config.path("endpoint").asText()));
      builder.forcePathStyle(true);
    }
    if (config.hasNonNull("accessKeyId") && config.hasNonNull("secretAccessKey")) {
      builder.credentialsProvider(
          StaticCredentialsProvider.create(
              AwsBasicCredentials.create(
                  config.path("accessKeyId").asText(), config.path("secretAccessKey").asText())));
    }
    return builder.build();
  }

  private String extractText(LoadedDocument document) throws Exception {
    String lower = document.title().toLowerCase();
    if (lower.endsWith(".pdf")) {
      try (var pdf = Loader.loadPDF(document.bytes())) {
        return new PDFTextStripper().getText(pdf);
      }
    }
    if (lower.endsWith(".docx")) {
      try (var doc = new XWPFDocument(new ByteArrayInputStream(document.bytes()));
          var extractor = new XWPFWordExtractor(doc)) {
        return extractor.getText();
      }
    }
    if (isImage(lower)) {
      return ocr(document);
    }
    return new String(document.bytes(), StandardCharsets.UTF_8);
  }

  private String ocr(LoadedDocument document) throws Exception {
    Path temp = Files.createTempFile("chatly-ocr-", suffix(document.title()));
    Files.write(temp, document.bytes());
    try {
      Process process =
          new ProcessBuilder("tesseract", temp.toString(), "stdout", "-l", properties.ocrLanguage())
              .redirectErrorStream(true)
              .start();
      byte[] output = process.getInputStream().readAllBytes();
      int exit = process.waitFor();
      if (exit != 0) {
        throw new IllegalStateException("tesseract failed: " + new String(output, StandardCharsets.UTF_8));
      }
      return new String(output, StandardCharsets.UTF_8);
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  private List<String> chunk(String text) {
    String cleaned = text.replaceAll("\\s+", " ").trim();
    List<String> chunks = new ArrayList<>();
    int size = Math.max(properties.chunkSize(), 200);
    int overlap = Math.min(Math.max(properties.chunkOverlap(), 0), size / 2);
    for (int start = 0; start < cleaned.length(); start += size - overlap) {
      chunks.add(cleaned.substring(start, Math.min(cleaned.length(), start + size)));
    }
    return chunks;
  }

  private static boolean isSupported(String name) {
    String lower = name.toLowerCase();
    return lower.endsWith(".txt")
        || lower.endsWith(".md")
        || lower.endsWith(".csv")
        || lower.endsWith(".json")
        || lower.endsWith(".html")
        || lower.endsWith(".pdf")
        || lower.endsWith(".docx")
        || isImage(lower);
  }

  private static boolean isImage(String lower) {
    return lower.endsWith(".png")
        || lower.endsWith(".jpg")
        || lower.endsWith(".jpeg")
        || lower.endsWith(".tif")
        || lower.endsWith(".tiff")
        || lower.endsWith(".bmp");
  }

  private static String suffix(String name) {
    int dot = name.lastIndexOf('.');
    return dot >= 0 ? name.substring(dot) : ".bin";
  }

  private static String sha256(String value) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
  }

  record LoadedDocument(String title, String key, String uri, byte[] bytes) {}

  public record SyncResult(int documents, int chunks) {}
}
