import { NextResponse } from "next/server";
import { requireSession } from "@/lib/auth";
import { prisma } from "@/lib/prisma";

export async function GET() {
  const session = await requireSession();
  const chats = await prisma.chat.findMany({
    where: { userId: session.id },
    orderBy: { updatedAt: "desc" },
    include: { messages: { orderBy: { createdAt: "asc" } } }
  });
  return NextResponse.json(chats);
}

export async function POST() {
  const session = await requireSession();
  const chat = await prisma.chat.create({
    data: { userId: session.id, title: "Новый чат" },
    include: { messages: true }
  });
  return NextResponse.json(chat);
}
