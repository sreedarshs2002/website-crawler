package com.example.demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private final ChatClient chatClient;

    public AiService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String ask(String knowledge, String question) {

        String prompt = """
                You are answering questions about a website.

                Use only the information provided in the knowledge document.

                If the answer is not available in the knowledge document,
                say that the information was not found in the website content.

                Knowledge Document:
                %s

                User Question:
                %s

                Give a clear and concise answer.
                """.formatted(knowledge, question);

        return chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
    }
}