package ch.zhaw.plantifyme.service;

import ch.zhaw.plantifyme.data.InfoArticles;
import ch.zhaw.plantifyme.data.InfoArticles.Article;
import ch.zhaw.plantifyme.model.dto.ArticleReference;
import ch.zhaw.plantifyme.model.dto.ChatMessage;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CoachService {

    private static final String SYSTEM_PROMPT_HEADER = """
            Du bist der Plantify Coach, ein freundlicher und kompetenter Berater für vegane Ernährung
            im Schweizer Kontext.

            DEINE ROLLE:
            - Hilf Menschen die vegan leben möchten oder Fragen zur pflanzlichen Ernährung haben
            - Argumentiere immer pro-vegan, aber nie belehrend oder aggressiv
            - Zu jedem typischen Gegenargument (z.B. "Mensch ist Fleischfresser", "Vegan ist Mangelernährung",
              "Pflanzen leiden auch") hast du eine fundierte, ruhige Antwort
            - Nutze ausschliesslich Fakten aus den bereitgestellten Artikeln als Basis
            - Wenn die Artikel ein Thema nicht abdecken: Sei ehrlich und sag es

            STIL:
            - Antworten MÜSSEN kurz sein: maximal 3-4 Sätze, ca. 50 Wörter
            - Pack nicht alle Argumente in eine Antwort. Gib den stärksten Punkt, lass Raum für Rückfragen
            - Wenn der User mehr wissen will, kann er nachfragen
            - Der Chat ist ein Gespräch, keine Vorlesung
            - Freundlich, persönlich, auf Augenhöhe
            - Kein Du-Sie-Mix, nutze konsequent "du"
            - Schweizer Deutsch-Begriffe okay (Migros, Coop, Znüni, etc.)
            - Keine Emojis übertreiben (max 1 pro Antwort)
            - Wenn du auf einen Artikel verweist, nenne ihn kurz: "Mehr dazu im Artikel '[Titel]' in der Info-Page"

            VERHALTEN:
            - Antworte NICHT zu Fragen ausserhalb veganer Ernährung/Lebensweise (z.B. Wetter, Sport).
              Verweise freundlich auf deinen Fokus.
            - Wenn jemand anti-vegan argumentiert: Bleib ruhig, gib Fakten
            - Erfinde keine Studien oder Zahlen die nicht in den Artikeln stehen

            WICHTIG: Halte jede Antwort unter 60 Wörtern. Lieber kurz und prägnant als ausführlich.
            Der User kann nachfragen wenn er mehr wissen will.
            """;

    private static final Set<String> STOPWORDS = Set.of(
            "der", "die", "das", "den", "dem", "des",
            "ein", "eine", "einen", "einem", "einer", "eines",
            "ist", "sind", "war", "waren", "bin", "bist", "sein", "seine",
            "und", "oder", "aber", "doch", "auch", "noch", "nur", "schon",
            "für", "mit", "von", "bei", "nach", "aus", "zu", "zum", "zur", "im", "in",
            "auf", "an", "am", "um", "über", "unter", "vor", "hinter",
            "ich", "du", "er", "sie", "es", "wir", "ihr", "mich", "dich", "mir", "dir",
            "wie", "was", "warum", "wenn", "weil", "dass", "ob",
            "nicht", "kein", "keine", "keinen", "mehr", "sehr", "viel",
            "this", "the", "and", "for", "are", "with",
            "habe", "hat", "hab", "haben", "hatte", "hätte",
            "kann", "kannst", "können", "könnte", "soll", "sollte", "muss", "müssen",
            "werde", "wird", "werden", "würde", "möchte"
    );

    private static final int MIN_REFERENCE_SCORE = 3;
    private static final int MAX_REFERENCED_ARTICLES = 2;

    public record ScoredArticle(Article article, int score) {}

    private final ChatClient chatClient;

    public CoachService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public List<ScoredArticle> findScoredArticles(String userQuery, int maxArticles) {
        if (userQuery == null || userQuery.isBlank()) return List.of();

        List<String> keywords = extractKeywords(userQuery);
        if (keywords.isEmpty()) return List.of();

        List<ScoredArticle> scored = new ArrayList<>();
        for (Article article : InfoArticles.ALL_ARTICLES) {
            int score = scoreArticle(article, keywords);
            if (score > 0) scored.add(new ScoredArticle(article, score));
        }

        return scored.stream()
                .sorted(Comparator.comparingInt(ScoredArticle::score).reversed())
                .limit(maxArticles)
                .toList();
    }

    public List<Article> findRelevantArticles(String userQuery, int maxArticles) {
        return findScoredArticles(userQuery, maxArticles).stream()
                .map(ScoredArticle::article)
                .toList();
    }

    public String chat(String message, List<ChatMessage> history) {
        List<Article> relevant = findRelevantArticles(message, 3);
        String systemPrompt = buildSystemPrompt(relevant);
        String userPrompt = buildUserPrompt(message, history);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(AnthropicChatOptions.builder().maxTokens(200).build())
                .call()
                .content();
    }

    List<String> extractKeywords(String query) {
        String normalized = query.toLowerCase().replaceAll("[^a-zäöüß0-9\\s-]", " ");
        Set<String> seen = new HashSet<>();
        List<String> keywords = new ArrayList<>();
        for (String token : normalized.split("\\s+")) {
            String t = token.trim();
            if (t.length() < 3) continue;
            if (STOPWORDS.contains(t)) continue;
            if (seen.add(t)) keywords.add(t);
        }
        return keywords;
    }

    private int scoreArticle(Article article, List<String> keywords) {
        int score = 0;
        String titleLower = article.title().toLowerCase();
        String categoryLower = article.category().toLowerCase();
        String contentLower = article.content().toLowerCase();

        for (String kw : keywords) {
            if (titleLower.contains(kw)) score += 3;
            if (categoryLower.contains(kw)) score += 2;
            int contentMatches = countOccurrences(contentLower, kw);
            score += Math.min(contentMatches, 5);
        }
        return score;
    }

    private int countOccurrences(String text, String needle) {
        if (needle.isEmpty()) return 0;
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    private String buildSystemPrompt(List<Article> relevant) {
        StringBuilder sb = new StringBuilder(SYSTEM_PROMPT_HEADER);
        sb.append("\nVERFÜGBARE ARTIKEL ALS WISSENSBASIS:\n");
        if (relevant.isEmpty()) {
            sb.append("Zu diesem Thema haben wir keine spezifischen Artikel. Antworte basierend auf allgemeinem ")
              .append("Wissen über vegane Ernährung, bleibe aber faktenbasiert.\n");
        } else {
            for (Article a : relevant) {
                sb.append("\nTitel: ").append(a.title()).append("\n");
                sb.append("Kategorie: ").append(a.category()).append("\n");
                sb.append("Inhalt: ").append(a.content()).append("\n");
            }
        }
        return sb.toString();
    }

    private String buildUserPrompt(String message, List<ChatMessage> history) {
        if (history == null || history.isEmpty()) return message;
        StringBuilder sb = new StringBuilder("Bisheriger Gesprächsverlauf:\n");
        for (ChatMessage m : history) {
            String speaker = "assistant".equalsIgnoreCase(m.getRole()) ? "Coach" : "User";
            sb.append(speaker).append(": ").append(m.getContent()).append("\n");
        }
        sb.append("\nNeue Nachricht des Users: ").append(message);
        return sb.toString();
    }

    public List<ArticleReference> referencedArticles(String userQuery) {
        return findScoredArticles(userQuery, MAX_REFERENCED_ARTICLES).stream()
                .filter(sa -> sa.score() >= MIN_REFERENCE_SCORE)
                .map(sa -> new ArticleReference(sa.article().id(), sa.article().category(), sa.article().title()))
                .toList();
    }
}
