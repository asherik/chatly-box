package com.chatlybox.chat.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record AskRequest(UUID chatId, @NotBlank String message) {}

