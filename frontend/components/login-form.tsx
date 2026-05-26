"use client";

import { FormEvent, useState } from "react";
import { LockKeyhole, ShieldCheck } from "lucide-react";

export function LoginForm() {
  const [email, setEmail] = useState("admin@company.local");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError("");
    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password })
    });
    setLoading(false);
    if (!response.ok) {
      setError("Проверьте email и пароль.");
      return;
    }
    window.location.href = "/";
  }

  return (
    <main className="login-screen">
      <section className="login-copy">
        <div className="brand-mark">
          <ShieldCheck size={22} />
          Chatly Box
        </div>
        <h1>Закрытый контур для вопросов по корпоративным документам</h1>
        <p>
          Next.js-панель для локального RAG: Postgres, источники S3/папка,
          OCR и работа с моделями без внешних API.
        </p>
      </section>
      <form className="login-panel" onSubmit={onSubmit}>
        <div className="login-icon">
          <LockKeyhole size={24} />
        </div>
        <h2>Вход администратора</h2>
        <label>
          Email
          <input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            autoComplete="email"
          />
        </label>
        <label>
          Пароль
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
            placeholder="admin12345 после seed"
          />
        </label>
        {error && <div className="form-error">{error}</div>}
        <button type="submit" disabled={loading}>
          {loading ? "Проверяем..." : "Войти"}
        </button>
      </form>
    </main>
  );
}
