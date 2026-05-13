package com.frankies.bootcamp.service;

import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class AiMessageService {
    private final String apiKey;
    private final OpenAiService service;

    public AiMessageService() {
        this.apiKey = System.getProperty("OPENAI_API_KEY");
        if (apiKey != null && !apiKey.isEmpty()) {
            this.service = new OpenAiService(apiKey);
        } else {
            this.service = null;
        }
    }

    public String getMotivationalMessage(String name, double distance, double goal, boolean hitGoal, Integer leaderboardRank, String favouriteSports, String suggestedSport) {
        if (service == null) {
            return "Your progress looks great so far. AI motivation is unavailable right now.";
        }
        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format(
            "Generate a short, funny, and inspiring message for an athlete named %s. ", name));
        prompt.append(String.format("They have completed %.2f km out of a %.2f km goal this week. ", distance, goal));
        prompt.append(hitGoal ? "They hit their goal! " : "They are still working towards it. ");
        if (leaderboardRank != null) {
            prompt.append(String.format("They are currently ranked #%d on the leaderboard. ", leaderboardRank));
        }
        if (favouriteSports != null && !favouriteSports.isEmpty()) {
            prompt.append(String.format("Their favourite sports are: %s. ", favouriteSports));
        }
        if (suggestedSport != null && !suggestedSport.isEmpty()) {
            prompt.append(String.format("If there is still time left this week, encourage them to get moving. Only suggest trying %s next week as a joke if the week is over or the goal is hit. ", suggestedSport));
        }
        prompt.append("Make it personal, use their name, and add a light joke. Keep it under 2 short sentences.");
        ChatMessage systemMsg = new ChatMessage("system", "You are a witty and motivational sports coach.");
        ChatMessage userMsg = new ChatMessage("user", prompt.toString());
        ChatCompletionRequest req = ChatCompletionRequest.builder()
            .model("gpt-4")
            .messages(List.of(systemMsg, userMsg))
            .maxTokens(80)
            .temperature(0.8)
            .build();
        try {
            return service.createChatCompletion(req).getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            return getFallbackMessage(e);
        }
    }

    private String getFallbackMessage(Exception e) {
        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (message.contains("quota") || message.contains("rate limit") || message.contains("429") || message.contains("token")) {
            return "You are putting in the work. AI motivation has hit its limit for now, so check back a little later.";
        }
        if (message.contains("auth") || message.contains("api key") || message.contains("unauthorized") || message.contains("401")) {
            return "You are making steady progress. AI motivation is not configured correctly right now.";
        }
        return "You are making solid progress this week. AI motivation is temporarily unavailable.";
    }
}

