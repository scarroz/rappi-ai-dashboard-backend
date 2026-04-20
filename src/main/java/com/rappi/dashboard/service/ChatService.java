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
 * El system prompt se construye cargando los archivos Markdown del workspace
 * del agente (patrón tomado de Hercules/OpenClaw):
 *   - IDENTITY.md  → quién es el agente
 *   - SOUL.md      → reglas de comportamiento
 *   - ORCHESTRATOR.md → flujo de decisión
 *   - TOOLS.md     → referencia de las tools disponibles
 *
 * Las tools se registran pasando directamente el objeto con @Tool —
 * Spring AI 1.0.0 requiere .defaultTools(Object...) con la instancia,
 * no un ToolCallbackProvider en el builder.
 */
@Service
public class ChatService {

    private static final Logger log = Logger.getLogger(ChatService.class.getName());

    private final ChatClient chatClient;

    public ChatService(
            OllamaChatModel         ollamaChatModel,
            StoreVisibilityMcpTools storeVisibilityMcpTools) throws IOException {

        String systemPrompt = buildSystemPrompt();

        // .defaultTools() acepta instancias de objetos con métodos @Tool
        // NO acepta ToolCallbackProvider — ese es para el MCP Server, no el ChatClient
        this.chatClient = ChatClient.builder(ollamaChatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(storeVisibilityMcpTools)
                .build();

        log.info("ChatService: inicializado. System prompt: "
                + systemPrompt.length() + " chars");
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