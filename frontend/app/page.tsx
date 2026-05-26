import { redirect } from "next/navigation";
import { getSession } from "@/lib/auth";
import { ConsoleShell } from "@/components/console-shell";

export default async function Home() {
  const session = await getSession();
  if (!session) redirect("/login");
  return <ConsoleShell user={session} />;
}
