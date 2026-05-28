package com.chatlybox.documents;

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
@Table(name = "document_chunk")
public class DocumentChunkEntity {
  @Id
  public UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "document_id", nullable = false)
  public DocumentEntity document;

  public int ordinal;

  @Column(columnDefinition = "text")
  public String content;

  @Column(columnDefinition = "jsonb")
  public String embedding;

  public Instant createdAt;
}
