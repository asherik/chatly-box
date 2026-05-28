package com.chatlybox.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_message")
public class ChatMessageEntity {
  @Id
  public UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chat_id", nullable = false)
  public ChatEntity chat;

  public String role;

  @Column(columnDefinition = "text")
  public String content;

  @Column(columnDefinition = "jsonb")
  public String sources;

  public Instant createdAt = Instant.now();
}
