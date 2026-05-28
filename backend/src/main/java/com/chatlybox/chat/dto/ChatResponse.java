package com.chatlybox.chat.dto;

import com.chatlybox.chat.ChatEntity;

import java.util.List;
import java.util.UUID;

public record ChatResponse(UUID id, String title, List<MessageResponse> messages) {
    public static ChatResponse from(ChatEntity chat) {
        return new ChatResponse(
                chat.id,
                chat.title,
                chat.messages.stream().map(MessageResponse::from).toList());
    }
}