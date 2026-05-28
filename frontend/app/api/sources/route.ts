import { proxyBackend } from "@/lib/backend";

export async function GET() {
  return proxyBackend("/api/sources");
}

export async function POST(request: Request) {
  return proxyBackend("/api/sources", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: await request.text()
  });
}
