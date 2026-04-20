package com.rappi.dashboard.controller;

import com.rappi.dashboard.dto.chat.ChatRequestDto;
import com.rappi.dashboard.dto.chat.ChatResponseDto;
import com.rappi.dashboard.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@Tag(
    name        = "Chat",
    description = "Agente conversacional potenciado por Gemma via Ollama con Spring AI. " +
                  "El agente decide autonomamente qué tool invocar según la pregunta. "

)
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(
        summary     = "Enviar mensaje al agente",
        description = """
            Envía una pregunta en lenguaje natural sobre los datos de disponibilidad.
            
            El agente (Gemma via Ollama) sigue el flujo IDENTIFY → EXECUTE → DELIVER:
            - Identifica si necesita datos reales (invoca tool) o puede responder directo
            - Si necesita datos, Spring AI ejecuta la tool automáticamente
            - Genera la respuesta final con el dato exacto
            
            Ejemplos de preguntas:
            - ¿Cuál fue el día con más tiendas activas?
            - ¿A qué hora hay más tiendas disponibles?
            - Dame el resumen del 6 de febrero
            - ¿Cuántos datos tiene el dataset?
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "Respuesta del agente",
            content      = @Content(schema = @Schema(implementation = ChatResponseDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Mensaje vacío")
    })
    @RequestBody(
        required = true,
        content  = @Content(
            schema   = @Schema(implementation = ChatRequestDto.class),
            examples = {
                @ExampleObject(name = "Día pico",
                    value = "{\"message\": \"¿Cuál fue el día con más tiendas activas?\"}"),
                @ExampleObject(name = "Hora pico",
                    value = "{\"message\": \"¿A qué hora hay más tiendas disponibles?\"}"),
                @ExampleObject(name = "Fecha específica",
                    value = "{\"message\": \"Dame el resumen del 6 de febrero de 2026\"}")
            }
        )
    )
    @PostMapping
    public ResponseEntity<ChatResponseDto> chat(
            @org.springframework.web.bind.annotation.RequestBody ChatRequestDto request) {

        if (request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(chatService.chat(request));
    }
}
