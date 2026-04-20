package com.rappi.dashboard.dto.chat;

/**
 * Respuesta del agente al frontend.
 * toolUsed: nombre de la tool invocada, null si respondió sin tools.
 */
public record ChatResponseDto(
    String response,
    String toolUsed
) {}
