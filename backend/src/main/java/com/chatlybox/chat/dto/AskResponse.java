package com.chatlybox.chat.dto;

import java.util.UUID;

public record AskResponse(UUID chatId, MessageResponse message) {}
