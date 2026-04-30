package ch.zhaw.plantifyme.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Test & Diagnostik", description = "Diagnose-Endpoints für Entwicklung — nicht für Produktivnutzung")
@RestController
@RequestMapping("/api/ai")
public class AiTestController {

    private final ChatClient chatClient;

    public AiTestController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Operation(
            summary = "Spring AI Verbindungstest",
            description = "Schickt einen Prompt direkt an den ChatClient (Anthropic) und liefert die Rohantwort. Diagnostik-Endpoint."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI-Antwort"),
            @ApiResponse(responseCode = "400", description = "Prompt fehlt")
    })
    @PostMapping("/test")
    public ResponseEntity<String> testAi(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest().body("Missing 'prompt' field");
        }

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return ResponseEntity.ok(response);
    }
}
