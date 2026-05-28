import { NextResponse } from "next/server";
import { z } from "zod";
import { createSession } from "@/lib/auth";
import { backendBasicToken } from "@/lib/backend";

const schema = z.object({
  email: z.string().email(),
  password: z.string().min(1)
});

export async function POST(request: Request) {
  const body = schema.parse(await request.json());
  const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080";
  const response = await fetch(`${backendUrl}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
    cache: "no-store"
  });

  if (!response.ok) {
    return NextResponse.json({ error: "Неверный email или пароль" }, { status: 401 });
  }

  const user = await response.json();
  await createSession(
    {
      id: user.id,
      email: user.email,
      name: user.name,
      role: user.role
    },
    backendBasicToken(body.email.toLowerCase(), body.password)
  );

  return NextResponse.json({ ok: true });
}
