import { proxyBackend } from "@/lib/backend";

export async function GET() {
  return proxyBackend("/api/settings");
}

export async function PUT(request: Request) {
  return proxyBackend("/api/settings", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: await request.text()
  });
}
