package ch.zhaw.plantifyme.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CoachChatResponse {
    private String reply;
    private List<ArticleReference> referencedArticles;
}
