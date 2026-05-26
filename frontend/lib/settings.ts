import { prisma } from "@/lib/prisma";

export type RuntimeSettings = {
  runtimeProvider: "ollama" | "llama.cpp";
  embeddingModel: string;
  chatModel: string;
  topK: number;
  temperature: number;
};

export const defaultSettings: RuntimeSettings = {
  runtimeProvider: "ollama",
  embeddingModel: "nomic-embed-text",
  chatModel: "qwen2.5:7b-instruct",
  topK: 6,
  temperature: 0.2
};

export async function getSettings(): Promise<RuntimeSettings> {
  const rows = await prisma.appSetting.findMany();
  const values = Object.fromEntries(rows.map((row) => [row.key, row.value]));
  return {
    ...defaultSettings,
    ...values,
    topK: Number(values.topK ?? defaultSettings.topK),
    temperature: Number(values.temperature ?? defaultSettings.temperature)
  } as RuntimeSettings;
}

export async function saveSettings(settings: RuntimeSettings) {
  await Promise.all(
    Object.entries(settings).map(([key, value]) =>
      prisma.appSetting.upsert({
        where: { key },
        update: { value },
        create: { key, value }
      })
    )
  );
}
