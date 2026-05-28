package com.chatlybox.documents;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {
  long countBySourceId(UUID sourceId);

  @EntityGraph(value = "document.withChunks")
  Optional<DocumentEntity> findBySourceIdAndUri(UUID sourceId, String uri);

  @Override
  @EntityGraph(value = "document.withChunks")
  Optional<DocumentEntity> findById(UUID id);

  @EntityGraph(value = "document.withChunks")
  List<DocumentEntity> findTop50ByOrderByCreatedAtDesc();

  @EntityGraph(value = "document.withChunks")
  List<DocumentEntity> findAllByOrderByCreatedAtDesc();
}
