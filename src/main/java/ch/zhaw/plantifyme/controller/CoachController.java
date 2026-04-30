package ch.zhaw.plantifyme.controller;

import ch.zhaw.plantifyme.model.dto.CoachChatRequest;
import ch.zhaw.plantifyme.model.dto.CoachChatResponse;
import ch.zhaw.plantifyme.service.CoachService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Plantify Coach", description = "AI-basierter Chatbot für Beratung zu veganer Ernährung (Spring AI / Anthropic)")
@RestController
@RequestMapping("/api/coach")
@RequiredArgsConstructor
public class CoachController {

    private static final int MAX_MESSAGE_LENGTH = 500;

    @NonNull
    private final CoachService coachService;

    @Operation(
            summary = "Mit dem Coach chatten",
            description = "Schickt eine User-Nachricht an den AI-Coach und liefert eine Antwort plus referenzierte Wissensartikel. " +
                    "Maximale Nachrichtenlänge: 500 Zeichen. Verlauf (history) wird optional mitgeschickt für Kontext."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coach-Antwort plus referenzierte Artikel"),
            @ApiResponse(responseCode = "400", description = "Nachricht leer oder >500 Zeichen"),
            @ApiResponse(responseCode = "500", description = "Coach gerade nicht erreichbar (z.B. AI-API down)")
    })
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody CoachChatRequest request) {
        String message = request.getMessage();
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body("Bitte gib eine Nachricht ein.");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            return ResponseEntity.badRequest().body("Deine Nachricht ist zu lang (max " + MAX_MESSAGE_LENGTH + " Zeichen).");
        }

        try {
            String reply = coachService.chat(message, request.getHistory());
            return ResponseEntity.ok(new CoachChatResponse(reply, coachService.referencedArticles(message)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Der Coach ist gerade nicht erreichbar. Bitte versuch es gleich nochmal.");
        }
    }
}
