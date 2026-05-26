package com.chatlybox.sources;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentSourceRepository extends JpaRepository<DocumentSource, UUID> {}
