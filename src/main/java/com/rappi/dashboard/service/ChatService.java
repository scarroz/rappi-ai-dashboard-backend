package com.rappi.dashboard.service;

import com.rappi.dashboard.dto.chat.ChatRequestDto;
import com.rappi.dashboard.dto.chat.ChatResponseDto;
import com.rappi.dashboard.mcp.StoreVisibilityMcpTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Orquesta la conversación con Gemma via Spring AI + Ollama.
 *
 * Optimizaciones de latencia:
 *
 * 1. SYSTEM PROMPT EN CACHÉ: los archivos Markdown se leen una sola vez
 *    en el constructor. Cero I/O en cada request.
 *
 * 2. TOOLS CON CACHÉ: StoreVisibilityMcpTools cachea los resultados de BD,
 *    eliminando latencia de PostgreSQL en tool calls repetidos.
 */
@Service
public class ChatService {

    private static final Logger log = Logger.getLogger(ChatService.class.getName());

    private final ChatClient chatClient;

    public ChatService(
            OllamaChatModel         ollamaChatModel,
            StoreVisibilityMcpTools storeVisibilityMcpTools) throws IOException {

        String systemPrompt = buildSystemPrompt();

        this.chatClient = ChatClient.builder(ollamaChatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(storeVisibilityMcpTools)
                .build();

        log.info("ChatService: listo. System prompt: " + systemPrompt.length() + " chars");
    }

    // ── Chat ──────────────────────────────────────────────────

    public ChatResponseDto chat(ChatRequestDto request) {
        if (request.message() == null || request.message().isBlank()) {
            return new ChatResponseDto("Por favor escribe una pregunta.", null);
        }

        try {
            String response = chatClient.prompt()
                    .user(request.message())
                    .call()
                    .content();

            return new ChatResponseDto(response, null);

        } catch (Exception e) {
            log.severe("ChatService error: " + e.getMessage());
            return new ChatResponseDto(
                    "Ocurrió un error procesando tu consulta. Intenta de nuevo.", null
            );
        }
    }

    // ── System Prompt Builder ─────────────────────────────────

    private String buildSystemPrompt() throws IOException {
        return loadMarkdown("agent/IDENTITY.md")     + "\n\n"
                + loadMarkdown("agent/SOUL.md")         + "\n\n"
                + loadMarkdown("agent/ORCHESTRATOR.md") + "\n\n"
                + loadMarkdown("agent/TOOLS.md");
    }

    private String loadMarkdown(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            log.warning("Prompt file not found: " + path);
            return "";
        }
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}