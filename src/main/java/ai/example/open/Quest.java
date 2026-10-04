package ai.example.open;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Quest extends ai.example.open.dto.Quest {

    public Quest(String question, String conversationId) {
        super(question, conversationId);
    }
}
