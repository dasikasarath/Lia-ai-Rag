package ai.example.open.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import ai.example.open.dto.Quest;

import java.util.List;

@Service
public class AIservice {

    private static final Logger log = LoggerFactory.getLogger(AIservice.class);

    private final VectorStore vs;
    private final ChatClient cc;
    private final ChatMemory chatMemory;

    public AIservice(ChatClient.Builder chatClientBuilder, VectorStore vs, ChatMemory chatMemory) {
        this.vs = vs;
        this.chatMemory = chatMemory;
        this.cc = chatClientBuilder.build();
    }

    public String asku(Quest quest) {
        String convId = resolveConversationId(quest != null ? quest.getConversationId() : null);
        String question = quest != null ? quest.getQuestion() : "";
        return searchres(question, convId);
    }

    public String searchres(String que) {
        return searchres(que, "default-session");
    }

    public String searchres(String que, String conversationId) {
        String convId = resolveConversationId(conversationId);

        StringBuilder contextBuilder = new StringBuilder();
        try {
            List<Document> db = vs.similaritySearch(SearchRequest.builder().query(que).topK(5).build());
            if (db != null) {
                for (Document doc : db) {
                    if (doc != null && doc.getText() != null && !doc.getText().isBlank()) {
                        String source = (String) doc.getMetadata().getOrDefault("source", "Uploaded Document");
                        contextBuilder.append("[").append(source).append("]:\n")
                                      .append(doc.getText().trim())
                                      .append("\n\n---\n\n");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Vector search warning: {}", e.getMessage());
        }

        String retrievedContext = contextBuilder.toString().trim();
        String contextSnippet = retrievedContext.isEmpty()
                ? "No specific documentation snippets were found matching this query in the vector store."
                : retrievedContext;

        String systemPrompt = """
                You are Lia AI, an intelligent, helpful, and versatile AI Knowledge and Document Assistant.

                Core Responsibilities:
                1. Prioritize context from the uploaded documents and knowledge base provided below to answer the user's questions accurately and thoroughly.
                2. When relevant context from the documentation is present, formulate a comprehensive, well-structured response directly referencing details from the documents.
                3. If the user's question is not directly covered in the documentation context, synthesize a helpful, professional, and knowledgeable answer while clarifying whether the information was found in the uploaded documents.
                4. Maintain conversational context across previous interactions seamlessly using chat memory.
                5. Keep responses engaging, structured (using markdown bullet points, bold highlights, or code blocks where appropriate), clear, and professional.

                Retrieved Documentation Context:
                %s
                """.formatted(contextSnippet);

        return cc.prompt()
                .system(systemPrompt)
                .user(que)
                .advisors(advisorSpec -> advisorSpec
                        .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                        .param("chat_memory_conversation_id", convId))
                .call()
                .content();
    }

    public void clearMemory(String conversationId) {
        String convId = resolveConversationId(conversationId);
        chatMemory.clear(convId);
    }

    private String resolveConversationId(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return "default-session";
        }
        return conversationId.trim();
    }
}
