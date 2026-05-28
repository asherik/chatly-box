"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  Bot,
  Database,
  FileSearch,
  Folder,
  LogOut,
  MessageSquarePlus,
  Play,
  Send,
  ServerCog,
  UploadCloud
} from "lucide-react";
import type { SessionUser } from "@/lib/auth";

type Message = {
  id: string;
  role: string;
  content: string;
  sources?: string | Array<{ title: string; uri: string; score: number }>;
};

type Chat = {
  id: string;
  title: string;
  messages: Message[];
};

type Source = {
  id: string;
  name: string;
  type: "LOCAL_FOLDER" | "S3";
  status: string;
  lastError?: string;
  lastSyncedAt?: string;
  documents?: number;
};

type Settings = {
  runtimeProvider: "ollama" | "llama.cpp";
  embeddingModel: string;
  chatModel: string;
  topK: number;
  temperature: number;
};

export function ConsoleShell({ user }: { user: SessionUser }) {
  const [chats, setChats] = useState<Chat[]>([]);
  const [activeChatId, setActiveChatId] = useState<string>();
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [sources, setSources] = useState<Source[]>([]);
  const [settings, setSettings] = useState<Settings>({
    runtimeProvider: "ollama",
    embeddingModel: "nomic-embed-text",
    chatModel: "qwen2.5:7b-instruct",
    topK: 6,
    temperature: 0.2
  });
  const [sourceDraft, setSourceDraft] = useState({
    name: "Локальная база знаний",
    path: "C:\\\\docs"
  });

  const activeChat = useMemo(
    () => chats.find((chat) => chat.id === activeChatId) ?? chats[0],
    [chats, activeChatId]
  );

  useEffect(() => {
    void refresh();
  }, []);

  async function refresh() {
    const [chatResponse, sourceResponse, settingsResponse] = await Promise.all([
      fetch("/api/chats"),
      fetch("/api/sources"),
      fetch("/api/settings")
    ]);
    const nextChats = await chatResponse.json();
    setChats(nextChats);
    setActiveChatId((current) => current ?? nextChats[0]?.id);
    setSources(await sourceResponse.json());
    setSettings(await settingsResponse.json());
  }

  async function sendMessage(event: FormEvent) {
    event.preventDefault();
    if (!message.trim()) return;
    setBusy(true);
    const current = message;
    setMessage("");
    const response = await fetch("/api/chat", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ chatId: activeChat?.id, message: current })
    });
    setBusy(false);
    if (!response.ok) {
      setMessage(current);
      return;
    }
    await refresh();
  }

  async function createChat() {
    const response = await fetch("/api/chats", { method: "POST" });
    const chat = await response.json();
    await refresh();
    setActiveChatId(chat.id);
  }

  async function addLocalSource(event: FormEvent) {
    event.preventDefault();
    await fetch("/api/sources", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        name: sourceDraft.name,
        type: "LOCAL_FOLDER",
        config: { path: sourceDraft.path }
      })
    });
    await refresh();
  }

  async function sync(id: string) {
    await fetch(`/api/sources/${id}/sync`, { method: "POST" });
    await refresh();
  }

  async function saveSettings(event: FormEvent) {
    event.preventDefault();
    await fetch("/api/settings", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(settings)
    });
    await refresh();
  }

  async function logout() {
    await fetch("/api/auth/logout", { method: "POST" });
    window.location.href = "/login";
  }

  return (
    <main className="shell">
      <aside className="sidebar">
        <div className="brand-line">
          <Bot size={22} />
          <span>Chatly Box</span>
        </div>
        <button className="new-chat" onClick={createChat}>
          <MessageSquarePlus size={18} />
          Новый чат
        </button>
        <nav className="chat-list">
          {chats.map((chat) => (
            <button
              key={chat.id}
              className={chat.id === activeChat?.id ? "active" : ""}
              onClick={() => setActiveChatId(chat.id)}
            >
              {chat.title}
            </button>
          ))}
        </nav>
        <div className="user-box">
          <span>{user.email}</span>
          <button onClick={logout} aria-label="Выйти">
            <LogOut size={18} />
          </button>
        </div>
      </aside>

      <section className="chat-area">
        <header className="topbar">
          <div>
            <span className="eyebrow">Локальный RAG</span>
            <h1>Чат по документам</h1>
          </div>
          <div className="status-pills">
            <span>
              <Database size={15} />
              Postgres
            </span>
            <span>
              <ServerCog size={15} />
              {settings.runtimeProvider}
            </span>
          </div>
        </header>

        <div className="messages">
          {(activeChat?.messages.length ? activeChat.messages : emptyMessages).map((item, index) => (
            <article key={item.id ?? index} className={`message ${item.role}`}>
              <div>{item.content}</div>
              {normalizeSources(item.sources).length ? (
                <div className="source-links">
                  {normalizeSources(item.sources).map((source, sourceIndex) => (
                    <span key={`${source.uri}-${sourceIndex}`}>
                      [{sourceIndex + 1}] {source.title}
                    </span>
                  ))}
                </div>
              ) : null}
            </article>
          ))}
        </div>

        <form className="composer" onSubmit={sendMessage}>
          <input
            value={message}
            onChange={(event) => setMessage(event.target.value)}
            placeholder="Спросите про регламенты, договоры или сканы..."
          />
          <button disabled={busy || !message.trim()} aria-label="Отправить">
            <Send size={18} />
          </button>
        </form>
      </section>

      <aside className="control-panel">
        <section className="panel-section">
          <div className="section-title">
            <FileSearch size={18} />
            Источники
          </div>
          <form className="stack-form" onSubmit={addLocalSource}>
            <input
              value={sourceDraft.name}
              onChange={(event) => setSourceDraft({ ...sourceDraft, name: event.target.value })}
              placeholder="Название"
            />
            <input
              value={sourceDraft.path}
              onChange={(event) => setSourceDraft({ ...sourceDraft, path: event.target.value })}
              placeholder="C:\\docs"
            />
            <button type="submit">
              <Folder size={16} />
              Добавить папку
            </button>
          </form>
          <div className="source-list">
            {sources.map((source) => (
              <div className="source-row" key={source.id}>
                <div>
                  <strong>{source.name}</strong>
                  <span>
                    {source.type} · {source.status} · {source.documents ?? 0} док.
                  </span>
                </div>
                <button onClick={() => sync(source.id)} title="Индексировать">
                  <Play size={15} />
                </button>
              </div>
            ))}
          </div>
        </section>

        <section className="panel-section">
          <div className="section-title">
            <UploadCloud size={18} />
            Модели
          </div>
          <form className="stack-form" onSubmit={saveSettings}>
            <select
              value={settings.runtimeProvider}
              onChange={(event) =>
                setSettings({ ...settings, runtimeProvider: event.target.value as Settings["runtimeProvider"] })
              }
            >
              <option value="ollama">Ollama</option>
              <option value="llama.cpp">llama.cpp server</option>
            </select>
            <input
              value={settings.embeddingModel}
              onChange={(event) => setSettings({ ...settings, embeddingModel: event.target.value })}
              placeholder="Embedding model"
            />
            <input
              value={settings.chatModel}
              onChange={(event) => setSettings({ ...settings, chatModel: event.target.value })}
              placeholder="Chat model"
            />
            <div className="split-row">
              <input
                type="number"
                min={1}
                max={20}
                value={settings.topK}
                onChange={(event) => setSettings({ ...settings, topK: Number(event.target.value) })}
              />
              <input
                type="number"
                min={0}
                max={2}
                step={0.1}
                value={settings.temperature}
                onChange={(event) => setSettings({ ...settings, temperature: Number(event.target.value) })}
              />
            </div>
            <button type="submit">
              <ServerCog size={16} />
              Сохранить
            </button>
          </form>
          <div className="drop-zone">Drag-and-drop .gguf можно подключить следующим шагом через серверное хранилище моделей.</div>
        </section>
      </aside>
    </main>
  );
}

const emptyMessages: Message[] = [
  {
    id: "empty",
    role: "assistant",
    content:
      "Добавьте источник, запустите индексирование и задайте вопрос. Ответ будет построен только по найденным фрагментам документов."
  }
];

function normalizeSources(sources: Message["sources"]) {
  if (!sources) return [];
  if (Array.isArray(sources)) return sources;
  try {
    const parsed = JSON.parse(sources);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}
