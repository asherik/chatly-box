import { proxyBackend } from "@/lib/backend";

export async function POST(request: Request) {
  return proxyBackend("/api/chats/ask", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: await request.text()
  });
}
