import { NextResponse } from "next/server";
import { z } from "zod";
import type { Document, DocumentChunk } from "@prisma/client";
import { cosineSimilarity, embedText, generateAnswer } from "@/lib/ai";
import { requireSession } from "@/lib/auth";
import { prisma } from "@/lib/prisma";
import { getSettings } from "@/lib/settings";

const schema = z.object({
  chatId: z.string().optional(),
  message: z.string().min(1)
});

export async function POST(request: Request) {
  const session = await requireSession();
  const body = schema.parse(await request.json());
  const settings = await getSettings();

  const chat =
    body.chatId
      ? await prisma.chat.findFirstOrThrow({
          where: { id: body.chatId, userId: session.id }
        })
      : await prisma.chat.create({
          data: {
            userId: session.id,
            title: body.message.slice(0, 60)
          }
        });

  await prisma.message.create({
    data: { chatId: chat.id, role: "user", content: body.message }
  });

  const questionEmbedding = await embedText(body.message, settings);
  const chunks = await prisma.documentChunk.findMany({
    include: { document: true },
    take: 2000,
    orderBy: { createdAt: "desc" }
  });

  const ranked = chunks
    .map((chunk: DocumentChunk & { document: Document }) => ({
      chunk,
      score: cosineSimilarity(questionEmbedding, chunk.embedding as number[])
    }))
    .sort((a: { score: number }, b: { score: number }) => b.score - a.score)
    .slice(0, settings.topK);

  const sources = ranked.map(({ chunk, score }: { chunk: DocumentChunk & { document: Document }; score: number }) => ({
    title: chunk.document.title,
    uri: chunk.document.uri,
    excerpt: chunk.content,
    score
  }));

  const answer = await generateAnswer(body.message, sources, settings);
  const assistantMessage = await prisma.message.create({
    data: {
      chatId: chat.id,
      role: "assistant",
      content: answer,
      sources
    }
  });

  await prisma.chat.update({
    where: { id: chat.id },
    data: { updatedAt: new Date(), title: chat.title === "Новый чат" ? body.message.slice(0, 60) : chat.title }
  });

  return NextResponse.json({ chatId: chat.id, message: assistantMessage });
}
