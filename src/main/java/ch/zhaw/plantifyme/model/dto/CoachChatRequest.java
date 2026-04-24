package ch.zhaw.plantifyme.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CoachChatRequest {
    private String message;
    private List<ChatMessage> history;
}
