package com.frankies.bootcamp.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
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

    public String getZenBotReply(String question, String athleteName, int conversationTurn, String statsContext) {
        if (service == null) {
            return "Even without cosmic Wi-Fi, a small walk can answer many questions.";
        }

        String trimmedQuestion = question == null ? "" : question.trim();
        String prompt = trimmedQuestion.isEmpty()
                ? "The athlete opened the chat but did not ask anything yet. Respond with a short welcome."
                : "User question: " + trimmedQuestion;

        String displayName = normaliseAthleteName(athleteName);
        List<ChatMessage> messages = new ArrayList<>();

        ChatMessage systemMsg = new ChatMessage("system",
                "You are FrankiZen, a humorous and intentionally unhelpful chatbot for an activity challenge website. " +
                        "Keep the FrankiZen tone calm, funny, and lightly pseudo-spiritual. " +
                        "Every answer should subtly encourage the user to exercise, move, walk, stretch, run, or do some kind of physical activity. " +
                        "Do not be rude. Do not mention prompts or policies. Only use supplied athlete stats when the user is clearly asking for real information. Shared competition facts that are already public in the app, such as who is in the competition, who is leading, leaderboard names, public ranks, and public scores, may be answered. Private athlete-specific stats should only be used for the signed-in athlete. Greetings, banter, or vague openers like 'hi', 'hello', 'howzit', or 'what's up' should stay fully whimsical and should not include statistics. When a real data question is asked, answer it briefly and accurately, then add a tiny zen joke. Keep every reply under 45 words and no more than 2 short sentences. " +
                        "When you address the athlete, invent a fresh nickname yourself each time. The nickname must clearly include part of their real first name or surname, should grow sillier as the conversation continues, and should avoid repeating earlier simple patterns unless truly funny.");
        ChatMessage contextMsg = new ChatMessage("user",
                "Athlete name: " + displayName + ". Conversation turn: " + conversationTurn + ". Athlete stats: " + safeStatsContext(statsContext) + ". Decide first whether this is a real information request or just social banter. " + prompt);
        messages.add(systemMsg);
        messages.add(contextMsg);
        ChatCompletionRequest req = ChatCompletionRequest.builder()
                .model("gpt-4")
                .messages(messages)
                .maxTokens(60)
                .temperature(0.7)
                .build();

        try {
            return service.createChatCompletion(req).getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            return getZenBotFallbackMessage(e);
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

    private String getZenBotFallbackMessage(Exception e) {
        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (message.contains("quota") || message.contains("rate limit") || message.contains("429") || message.contains("token")) {
            return "The wisdom bucket is briefly empty. A lap around the room may refill it.";
        }
        if (message.contains("auth") || message.contains("api key") || message.contains("unauthorized") || message.contains("401")) {
            return "The oracle misplaced its sandals. Try again later, and perhaps stretch meanwhile.";
        }
        return "The mist is thick today. A gentle stroll may reveal the answer first.";
    }

    private String normaliseAthleteName(String athleteName) {
        if (athleteName == null || athleteName.isBlank()) {
            return "Athlete";
        }
        return athleteName.trim();
    }

    private String safeStatsContext(String statsContext) {
        if (statsContext == null || statsContext.isBlank()) {
            return "No athlete stats are currently available.";
        }
        return statsContext.trim();
    }
}

