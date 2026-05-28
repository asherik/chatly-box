import { proxyBackend } from "@/lib/backend";

export async function GET() {
  return proxyBackend("/api/chats");
}

export async function POST() {
  return proxyBackend("/api/chats", { method: "POST" });
}
