import { NextResponse } from "next/server";
import { z } from "zod";
import { createSession, findLoginUser, verifyPassword } from "@/lib/auth";

const schema = z.object({
  email: z.string().email(),
  password: z.string().min(1)
});

export async function POST(request: Request) {
  const body = schema.parse(await request.json());
  const user = await findLoginUser(body.email);
  if (!user || !(await verifyPassword(body.password, user.passwordHash))) {
    return NextResponse.json({ error: "Неверный email или пароль" }, { status: 401 });
  }

  await createSession({
    id: user.id,
    email: user.email,
    name: user.name,
    role: user.role
  });

  return NextResponse.json({ ok: true });
}
