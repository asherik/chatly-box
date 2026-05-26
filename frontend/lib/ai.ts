import { RuntimeSettings } from "@/lib/settings";

type Source = {
  title: string;
  uri: string;
  excerpt: string;
};

function baseUrl(settings: RuntimeSettings) {
  if (settings.runtimeProvider === "llama.cpp") {
    return process.env.LLAMA_CPP_BASE_URL ?? "http://localhost:8080";
  }
  return process.env.OLLAMA_BASE_URL ?? "http://localhost:11434";
}

export async function embedText(input: string, settings: RuntimeSettings) {
  if (settings.runtimeProvider === "llama.cpp") {
    const response = await fetch(`${baseUrl(settings)}/v1/embeddings`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ model: settings.embeddingModel, input })
    });
    if (!response.ok) throw new Error(await response.text());
    const payload = await response.json();
    return payload.data?.[0]?.embedding as number[];
  }

  const response = await fetch(`${baseUrl(settings)}/api/embeddings`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ model: settings.embeddingModel, prompt: input })
  });
  if (!response.ok) throw new Error(await response.text());
  const payload = await response.json();
  return payload.embedding as number[];
}

export async function generateAnswer(
  question: string,
  sources: Source[],
  settings: RuntimeSettings
) {
  const context = sources
    .map(
      (source, index) =>
        `[${index + 1}] ${source.title}\nURI: ${source.uri}\n${source.excerpt}`
    )
    .join("\n\n");

  const system =
    "Ты корпоративный RAG-ассистент. Отвечай только по найденному контексту. Если данных недостаточно, скажи об этом. В конце добавь раздел 'Источники' со списком номеров источников.";
  const prompt = `${system}\n\nКонтекст:\n${context || "Нет найденного контекста."}\n\nВопрос: ${question}`;

  if (settings.runtimeProvider === "llama.cpp") {
    const response = await fetch(`${baseUrl(settings)}/v1/chat/completions`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        model: settings.chatModel,
        temperature: settings.temperature,
        messages: [
          { role: "system", content: system },
          { role: "user", content: `Контекст:\n${context}\n\nВопрос: ${question}` }
        ]
      })
    });
    if (!response.ok) throw new Error(await response.text());
    const payload = await response.json();
    return payload.choices?.[0]?.message?.content ?? "";
  }

  const response = await fetch(`${baseUrl(settings)}/api/generate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      model: settings.chatModel,
      prompt,
      stream: false,
      options: { temperature: settings.temperature }
    })
  });
  if (!response.ok) throw new Error(await response.text());
  const payload = await response.json();
  return payload.response ?? "";
}

export function cosineSimilarity(a: number[], b: number[]) {
  let dot = 0;
  let normA = 0;
  let normB = 0;
  const length = Math.min(a.length, b.length);
  for (let index = 0; index < length; index += 1) {
    dot += a[index] * b[index];
    normA += a[index] * a[index];
    normB += b[index] * b[index];
  }
  return dot / (Math.sqrt(normA) * Math.sqrt(normB) || 1);
}
