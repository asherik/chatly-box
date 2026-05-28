import { proxyBackend } from "@/lib/backend";

type Params = { params: Promise<{ id: string }> };

export async function POST(_request: Request, { params }: Params) {
  const { id } = await params;
  return proxyBackend(`/api/ingestion/sources/${id}/sync`, { method: "POST" });
}
