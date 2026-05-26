package com.chatlybox.sources;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "document_source")
public class DocumentSource {
  @Id
  public UUID id = UUID.randomUUID();

  public String name;
  public String type;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  public String config;

  public String status = "IDLE";
  public String lastError;
  public Instant lastSyncedAt;
  public Instant createdAt = Instant.now();
}
