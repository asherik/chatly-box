import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Chatly Box",
  description: "On-premise RAG platform for private document chat"
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ru">
      <body>{children}</body>
    </html>
  );
}
