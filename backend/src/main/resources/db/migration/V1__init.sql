create table app_user (
  id uuid primary key,
  email text not null unique,
  password_hash text not null,
  role text not null,
  created_at timestamptz not null default now()
);

create table document_source (
  id uuid primary key,
  name text not null,
  type text not null,
  config jsonb not null,
  status text not null default 'IDLE',
  last_error text,
  last_synced_at timestamptz,
  created_at timestamptz not null default now()
);

create table document (
  id uuid primary key,
  source_id uuid not null references document_source(id) on delete cascade,
  title text not null,
  uri text not null,
  checksum text not null,
  created_at timestamptz not null default now(),
  unique(source_id, uri)
);

create table document_chunk (
  id uuid primary key,
  document_id uuid not null references document(id) on delete cascade,
  ordinal integer not null,
  content text not null,
  embedding jsonb not null,
  created_at timestamptz not null default now()
);

create table chat (
  id uuid primary key,
  user_id uuid not null references app_user(id) on delete cascade,
  title text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table chat_message (
  id uuid primary key,
  chat_id uuid not null references chat(id) on delete cascade,
  role text not null,
  content text not null,
  sources jsonb,
  created_at timestamptz not null default now()
);

create table model_setting (
  key text primary key,
  value text not null,
  updated_at timestamptz not null default now()
);

insert into model_setting(key, value) values
  ('embeddingModel', 'intfloat/multilingual-e5-small'),
  ('chatModel', 'qwen2.5-7b-instruct-q4_k_m.gguf'),
  ('topK', '6'),
  ('temperature', '0.2')
on conflict do nothing;
