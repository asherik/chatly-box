package com.chatlybox.chat;

import com.chatlybox.users.AppUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<ChatEntity, UUID> {
  @EntityGraph(attributePaths = "messages")
  List<ChatEntity> findByUserOrderByUpdatedAtDesc(AppUser user);

  @EntityGraph(attributePaths = "messages")
  Optional<ChatEntity> findByIdAndUser(UUID id, AppUser user);
}
