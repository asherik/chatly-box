package com.chatlybox.chat.dto;

import com.chatlybox.chat.ChatMessageEntity;

import java.util.UUID;

public record MessageResponse(UUID id, String role, String content, String sources) {
    public static MessageResponse from(ChatMessageEntity message) {
        return new MessageResponse(message.id, message.role, message.content, message.sources);
    }
}