import { NextResponse } from "next/server";
import { requireSession } from "@/lib/auth";
import { syncSource } from "@/lib/indexing";

type Params = { params: Promise<{ id: string }> };

export async function POST(_request: Request, { params }: Params) {
  const session = await requireSession();
  if (session.role !== "ADMIN") {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const { id } = await params;
  await syncSource(id);
  return NextResponse.json({ ok: true });
}
