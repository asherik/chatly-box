import { getBackendToken } from "@/lib/auth";

const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080";

export function backendBasicToken(email: string, password: string) {
  return Buffer.from(`${email}:${password}`, "utf8").toString("base64");
}

export async function backendFetch(path: string, init: RequestInit = {}) {
  const token = await getBackendToken();
  if (!token) {
    return new Response(JSON.stringify({ error: "Unauthorized" }), {
      status: 401,
      headers: { "Content-Type": "application/json" }
    });
  }

  return fetch(`${backendUrl}${path}`, {
    ...init,
    headers: {
      ...init.headers,
      Authorization: `Basic ${token}`
    },
    cache: "no-store"
  });
}

export async function proxyBackend(path: string, init: RequestInit = {}) {
  const response = await backendFetch(path, init);
  const body = await response.text();
  return new Response(body, {
    status: response.status,
    headers: {
      "Content-Type": response.headers.get("Content-Type") ?? "application/json"
    }
  });
}
