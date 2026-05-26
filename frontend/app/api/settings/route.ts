import { NextResponse } from "next/server";
import { z } from "zod";
import { requireSession } from "@/lib/auth";
import { getSettings, saveSettings } from "@/lib/settings";

const schema = z.object({
  runtimeProvider: z.enum(["ollama", "llama.cpp"]),
  embeddingModel: z.string().min(1),
  chatModel: z.string().min(1),
  topK: z.coerce.number().int().min(1).max(20),
  temperature: z.coerce.number().min(0).max(2)
});

export async function GET() {
  await requireSession();
  return NextResponse.json(await getSettings());
}

export async function PUT(request: Request) {
  const session = await requireSession();
  if (session.role !== "ADMIN") {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const settings = schema.parse(await request.json());
  await saveSettings(settings);
  return NextResponse.json(settings);
}
