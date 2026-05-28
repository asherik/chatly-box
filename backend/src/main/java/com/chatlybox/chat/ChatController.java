package com.chatlybox.chat;

import com.chatlybox.chat.dto.AskRequest;
import com.chatlybox.chat.dto.AskResponse;
import com.chatlybox.chat.dto.ChatResponse;
import com.chatlybox.chat.dto.MessageResponse;
import com.chatlybox.rag.RagFacade;
import com.chatlybox.users.AppUser;
import com.chatlybox.users.AppUserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {
  private final ChatRepository chats;
  private final ChatMessageRepository messages;
  private final AppUserRepository users;
  private final RagFacade rag;

  @GetMapping
  List<ChatResponse> list(Principal principal) {
    AppUser user = currentUser(principal);
    return chats.findByUserOrderByUpdatedAtDesc(user).stream().map(ChatResponse::from).toList();
  }

  @PostMapping
  ChatResponse create(Principal principal) {
    AppUser user = currentUser(principal);
    ChatEntity chat = new ChatEntity();
    chat.user = user;
    chat.title = "Новый чат";
    return ChatResponse.from(chats.save(chat));
  }

  @PostMapping("/ask")
  @Transactional
  AskResponse ask(Principal principal, @Valid @RequestBody AskRequest request) {
    AppUser user = currentUser(principal);
    ChatEntity chat = request.chatId() == null
        ? createChat(user, request.message())
        : chats.findByIdAndUser(request.chatId(), user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found"));

    ChatMessageEntity userMessage = new ChatMessageEntity();
    userMessage.chat = chat;
    userMessage.role = "user";
    userMessage.content = request.message();
    messages.save(userMessage);

    String answer = rag.ask(request.message());
    ChatMessageEntity assistantMessage = new ChatMessageEntity();
    assistantMessage.chat = chat;
    assistantMessage.role = "assistant";
    assistantMessage.content = answer;
    assistantMessage.sources = "[]";
    messages.save(assistantMessage);

    if ("Новый чат".equals(chat.title)) {
      chat.title = request.message().substring(0, Math.min(60, request.message().length()));
    }
    chat.updatedAt = Instant.now();
    chats.save(chat);

    return new AskResponse(chat.id, MessageResponse.from(assistantMessage));
  }

  private ChatEntity createChat(AppUser user, String message) {
    ChatEntity chat = new ChatEntity();
    chat.user = user;
    chat.title = message.substring(0, Math.min(60, message.length()));
    return chats.save(chat);
  }

  private AppUser currentUser(Principal principal) {
    if (principal == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
    return users.findByEmail(principal.getName().toLowerCase())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
  }
}
