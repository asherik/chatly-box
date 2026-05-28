package com.chatlybox.documents;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "document")
@NamedEntityGraph(name = "document.withChunks", attributeNodes = @NamedAttributeNode("chunks"))
public class DocumentEntity {
  @Id
  public UUID id;

  @Column(name = "source_id", nullable = false)
  public UUID sourceId;

  public String title;
  public String uri;
  public String checksum;
  public Instant createdAt;

  @OneToMany(mappedBy = "document", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("ordinal asc")
  public List<DocumentChunkEntity> chunks = new ArrayList<>();
}
