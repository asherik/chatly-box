import crypto from "node:crypto";
import fs from "node:fs/promises";
import path from "node:path";
import { execFile } from "node:child_process";
import { promisify } from "node:util";
import { GetObjectCommand, ListObjectsV2Command, S3Client } from "@aws-sdk/client-s3";
import mammoth from "mammoth";
import { PDFParse } from "pdf-parse";
import { prisma } from "@/lib/prisma";
import { embedText } from "@/lib/ai";
import { getSettings } from "@/lib/settings";

const execFileAsync = promisify(execFile);
const textExtensions = new Set([".txt", ".md", ".csv", ".json", ".log", ".html"]);
const imageExtensions = new Set([".png", ".jpg", ".jpeg", ".tif", ".tiff", ".bmp"]);

type LocalConfig = { path: string };
type S3Config = {
  endpoint?: string;
  region?: string;
  bucket: string;
  prefix?: string;
  accessKeyId?: string;
  secretAccessKey?: string;
  forcePathStyle?: boolean;
};

export async function syncSource(sourceId: string) {
  const source = await prisma.documentSource.findUniqueOrThrow({ where: { id: sourceId } });
  await prisma.documentSource.update({
    where: { id: sourceId },
    data: { status: "RUNNING", lastError: null }
  });

  try {
    const files =
      source.type === "LOCAL_FOLDER"
        ? await loadLocalFiles(source.config as LocalConfig)
        : await loadS3Files(source.config as S3Config);

    const settings = await getSettings();
    for (const file of files) {
      const checksum = crypto.createHash("sha256").update(file.buffer).digest("hex");
      const text = await extractText(file.name, file.buffer);
      if (!text.trim()) continue;

      const document = await prisma.document.upsert({
        where: { sourceId_uri: { sourceId, uri: file.uri } },
        update: {
          title: file.name,
          mimeType: file.mimeType,
          checksum
        },
        create: {
          sourceId,
          title: file.name,
          uri: file.uri,
          mimeType: file.mimeType,
          checksum
        }
      });

      await prisma.documentChunk.deleteMany({ where: { documentId: document.id } });
      const chunks = chunkText(text);
      for (let index = 0; index < chunks.length; index += 1) {
        const embedding = await embedText(chunks[index], settings);
        await prisma.documentChunk.create({
          data: {
            documentId: document.id,
            content: chunks[index],
            embedding,
            ordinal: index
          }
        });
      }
    }

    await prisma.documentSource.update({
      where: { id: sourceId },
      data: { status: "DONE", lastSyncedAt: new Date() }
    });
  } catch (error) {
    await prisma.documentSource.update({
      where: { id: sourceId },
      data: { status: "FAILED", lastError: error instanceof Error ? error.message : String(error) }
    });
    throw error;
  }
}

async function loadLocalFiles(config: LocalConfig) {
  const root = path.resolve(config.path);
  const entries: Array<{ name: string; uri: string; mimeType?: string; buffer: Buffer }> = [];

  async function walk(current: string) {
    const children = await fs.readdir(current, { withFileTypes: true });
    for (const child of children) {
      const fullPath = path.join(current, child.name);
      if (child.isDirectory()) {
        await walk(fullPath);
      } else if (isSupported(fullPath)) {
        entries.push({
          name: path.basename(fullPath),
          uri: fullPath,
          buffer: await fs.readFile(fullPath)
        });
      }
    }
  }

  await walk(root);
  return entries;
}

async function loadS3Files(config: S3Config) {
  const client = new S3Client({
    endpoint: config.endpoint,
    region: config.region ?? "ru-central1",
    forcePathStyle: config.forcePathStyle ?? true,
    credentials:
      config.accessKeyId && config.secretAccessKey
        ? {
            accessKeyId: config.accessKeyId,
            secretAccessKey: config.secretAccessKey
          }
        : undefined
  });

  const entries: Array<{ name: string; uri: string; mimeType?: string; buffer: Buffer }> = [];
  let token: string | undefined;
  do {
    const listed = await client.send(
      new ListObjectsV2Command({
        Bucket: config.bucket,
        Prefix: config.prefix,
        ContinuationToken: token
      })
    );
    token = listed.NextContinuationToken;
    for (const object of listed.Contents ?? []) {
      if (!object.Key || !isSupported(object.Key)) continue;
      const downloaded = await client.send(
        new GetObjectCommand({ Bucket: config.bucket, Key: object.Key })
      );
      const bytes = await downloaded.Body?.transformToByteArray();
      if (!bytes) continue;
      entries.push({
        name: path.basename(object.Key),
        uri: `s3://${config.bucket}/${object.Key}`,
        buffer: Buffer.from(bytes)
      });
    }
  } while (token);

  return entries;
}

function isSupported(filePath: string) {
  const ext = path.extname(filePath).toLowerCase();
  return (
    textExtensions.has(ext) ||
    imageExtensions.has(ext) ||
    ext === ".pdf" ||
    ext === ".docx"
  );
}

async function extractText(fileName: string, buffer: Buffer) {
  const ext = path.extname(fileName).toLowerCase();
  if (textExtensions.has(ext)) return buffer.toString("utf8");
  if (ext === ".pdf") {
    const parser = new PDFParse({ data: buffer });
    const result = await parser.getText();
    await parser.destroy();
    return result.text;
  }
  if (ext === ".docx") return (await mammoth.extractRawText({ buffer })).value;
  if (imageExtensions.has(ext)) return ocrImage(buffer, ext);
  return "";
}

async function ocrImage(buffer: Buffer, ext: string) {
  const tempDir = await fs.mkdtemp(path.join(process.cwd(), "storage", "ocr-"));
  const imagePath = path.join(tempDir, `image${ext}`);
  await fs.writeFile(imagePath, buffer);
  try {
    const { stdout } = await execFileAsync("tesseract", [imagePath, "stdout", "-l", "rus+eng"]);
    return stdout;
  } finally {
    await fs.rm(tempDir, { recursive: true, force: true });
  }
}

function chunkText(input: string) {
  const cleaned = input.replace(/\s+/g, " ").trim();
  const chunks: string[] = [];
  const size = 1200;
  const overlap = 180;
  for (let start = 0; start < cleaned.length; start += size - overlap) {
    chunks.push(cleaned.slice(start, start + size));
  }
  return chunks;
}
