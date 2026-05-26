import { NextResponse } from "next/server";
import { z } from "zod";
import { Prisma } from "@prisma/client";
import { requireSession } from "@/lib/auth";
import { prisma } from "@/lib/prisma";

const schema = z.object({
  name: z.string().min(1),
  type: z.enum(["LOCAL_FOLDER", "S3"]),
  config: z.record(z.string(), z.unknown())
});

export async function GET() {
  await requireSession();
  const sources = await prisma.documentSource.findMany({
    orderBy: { createdAt: "desc" },
    include: { _count: { select: { documents: true } } }
  });
  return NextResponse.json(sources);
}

export async function POST(request: Request) {
  const session = await requireSession();
  if (session.role !== "ADMIN") {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const body = schema.parse(await request.json());
  const source = await prisma.documentSource.create({
    data: {
      name: body.name,
      type: body.type,
      config: body.config as Prisma.InputJsonValue
    }
  });
  return NextResponse.json(source);
}
