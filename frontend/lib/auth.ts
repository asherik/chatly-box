import { cookies } from "next/headers";
import { SignJWT, jwtVerify } from "jose";

const cookieName = "chatly_session";
const secret = new TextEncoder().encode(
  process.env.AUTH_SECRET ?? "dev-secret-change-me"
);

export type SessionUser = {
  id: string;
  email: string;
  name: string | null;
  role: "ADMIN" | "USER";
};

type SessionPayload = SessionUser & { backendToken: string };

export async function createSession(user: SessionUser, backendToken: string) {
  const token = await new SignJWT({ ...user, backendToken })
    .setProtectedHeader({ alg: "HS256" })
    .setIssuedAt()
    .setExpirationTime("8h")
    .sign(secret);

  const cookieStore = await cookies();
  cookieStore.set(cookieName, token, {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: 60 * 60 * 8
  });
}

export async function destroySession() {
  const cookieStore = await cookies();
  cookieStore.delete(cookieName);
}

export async function getSession(): Promise<SessionUser | null> {
  const payload = await readSessionPayload();
  if (!payload) return null;

  return {
    id: payload.id,
    email: payload.email,
    name: payload.name ?? null,
    role: payload.role
  };
}

export async function getBackendToken() {
  return (await readSessionPayload())?.backendToken ?? null;
}

export async function requireSession() {
  const session = await getSession();
  if (!session) {
    throw new Error("Unauthorized");
  }
  return session;
}

async function readSessionPayload() {
  const cookieStore = await cookies();
  const token = cookieStore.get(cookieName)?.value;
  if (!token) return null;

  try {
    const result = await jwtVerify(token, secret);
    return result.payload as SessionPayload;
  } catch {
    return null;
  }
}
