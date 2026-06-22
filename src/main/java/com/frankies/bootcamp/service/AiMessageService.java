package com.frankies.bootcamp.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class AiMessageService {
    private static final Logger log = Logger.getLogger(AiMessageService.class);
    private final String apiKey;
    private final OpenAiService service;
    private final OkHttpClient httpClient;

    public AiMessageService() {
        this.apiKey = System.getProperty("OPENAI_API_KEY");
        this.httpClient = new OkHttpClient();
        if (apiKey != null && !apiKey.isEmpty()) {
            this.service = new OpenAiService(apiKey);
        } else {
            this.service = null;
        }
    }

    public String getMotivationalMessage(String name, double distance, double goal, boolean hitGoal, Integer leaderboardRank,
                                         String favouriteSports, String suggestedSport) {
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
            prompt.append(String.format("If there is still time left this week, encourage them to get moving. " +
                    "Only suggest trying %s next week as a joke if the week is over or the goal is hit. ", suggestedSport));
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

    public String getCompetitionRecapMessage(String name,
                                             String competitionName,
                                             int weeksCompleted,
                                             double totalDistance,
                                             double totalScore,
                                             Integer finalRank,
                                             String favouriteSports) {
        if (service == null) {
            String rankText = finalRank == null ? "" : " and finished #" + finalRank;
            return "Nice work, " + name + " — you completed " + String.format("%.2f", totalDistance)
                    + " km across " + weeksCompleted + " weeks" + rankText + ". Historical glory still counts.";
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format(
                "Generate a short, funny, and inspiring recap for athlete %s looking back at a completed bootcamp competition", name));
        if (competitionName != null && !competitionName.isBlank()) {
            prompt.append(String.format(" named %s", competitionName));
        }
        prompt.append(". ");
        prompt.append(String.format("They completed %.2f km across %d weeks and scored %.2f total points. ",
                totalDistance, weeksCompleted, totalScore));
        if (finalRank != null) {
            prompt.append(String.format("Their final leaderboard rank was #%d. ", finalRank));
        }
        if (favouriteSports != null && !favouriteSports.isEmpty()) {
            prompt.append(String.format("Their main sports were: %s. ", favouriteSports));
        }
        prompt.append("Make it about the whole competition, not the current week. Keep it under 2 short sentences.");

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

    public String generateAthleteProfileBlurb(String name, String profileContext) {
        String displayName = normaliseAthleteName(name);
        if (service == null) {
            return displayName + " appears to be a committed bootcamp athlete with a suspiciously healthy relationship with moving around. " +
                    "Probably serious about progress, but not above pretending a recovery walk is a strategic masterstroke.";
        }

        String prompt = "Write a short athlete profile blurb for " + displayName + ". " +
                "Tone: mostly serious, warm, and very slightly funny. " +
                "It should sound like a human personal profile summary that could be accepted permanently, not a leaderboard report or competition update. " +
                "Do not mention ranks, points, weeks, exact distances, private kilometres, prompts, or that you are an AI. " +
                "Do not guess gender from the name. Use they/them pronouns, or avoid pronouns, unless explicit pronouns are supplied. " +
                "Use only broad public context if useful: " + safeStatsContext(profileContext) + ". " +
                "You may invent harmless, obviously light-hearted personality colour, but do not invent sensitive personal facts. " +
                "Keep it to 2 sentences, under 55 words.";

        ChatMessage systemMsg = new ChatMessage("system",
                "You write concise athlete profile blurbs for a friendly activity challenge app.");
        ChatMessage userMsg = new ChatMessage("user", prompt);
        ChatCompletionRequest req = ChatCompletionRequest.builder()
                .model("gpt-4")
                .messages(List.of(systemMsg, userMsg))
                .maxTokens(80)
                .temperature(0.9)
                .build();
        try {
            return service.createChatCompletion(req).getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            log.warn("Could not generate athlete profile blurb", e);
            return displayName + " appears to be a committed bootcamp athlete with a suspiciously healthy relationship with moving around. " +
                    "Probably serious about progress, but not above pretending a recovery walk is a strategic masterstroke.";
        }
    }

    public String generateAthletePerformanceSummary(String name, String profileContext) {
        String displayName = normaliseAthleteName(name);
        if (service == null) {
            return displayName + " is putting together a solid bootcamp campaign, with enough points on the board to look intentional. " +
                    "The stats suggest effort, consistency, and at least one moment where the couch probably felt betrayed.";
        }

        String prompt = "Write a short performance summary for athlete " + displayName + ". " +
                "Tone: mostly serious, warm, and very slightly funny. " +
                "This is a current competition performance note, not a permanent personal profile. " +
                "Do not mention exact distances, private kilometres, prompts, or that you are an AI. " +
                "Do not guess gender from the name. Use they/them pronouns, or avoid pronouns, unless explicit pronouns are supplied. " +
                "Use this public points/rank context: " + safeStatsContext(profileContext) + ". " +
                "Keep it to 2 sentences, under 55 words.";

        ChatMessage systemMsg = new ChatMessage("system",
                "You write concise athlete performance summaries for a friendly activity challenge app.");
        ChatMessage userMsg = new ChatMessage("user", prompt);
        ChatCompletionRequest req = ChatCompletionRequest.builder()
                .model("gpt-4")
                .messages(List.of(systemMsg, userMsg))
                .maxTokens(80)
                .temperature(0.8)
                .build();
        try {
            return service.createChatCompletion(req).getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            log.warn("Could not generate athlete performance summary", e);
            return displayName + " is putting together a solid bootcamp campaign, with enough points on the board to look intentional. " +
                    "The stats suggest effort, consistency, and at least one moment where the couch probably felt betrayed.";
        }
    }

    public String rewriteCompetitionInviteMessage(String competitionName, String currentMessage) {
        String trimmedMessage = currentMessage == null ? "" : currentMessage.trim();
        String trimmedCompetitionName = competitionName == null ? "" : competitionName.trim();
        if (service == null) {
            if (trimmedMessage.isBlank()) {
                return "Frankies Bootcamp is all sports, all effort, all welcome. Come join the fun.";
            }
            return trimmedMessage;
        }

        String prompt;
        if (trimmedMessage.isBlank()) {
            prompt = "Write a fresh invitation message for a Frankies Bootcamp competition. " +
                    "Use the motto: all sports are welcome, all sports are valid effort. " +
                    "Keep it warm, inviting, and concise. " +
                    (trimmedCompetitionName.isBlank() ? "" : "The competition is named " + trimmedCompetitionName + ". ") +
                    "Do not mention AI. Keep it under 45 words.";
        } else {
            prompt = "Refine this invitation message without changing its intent or personality too much. " +
                    "Keep the user's voice, tighten grammar and flow, and keep it warm and inviting. " +
                    "If the message is already specific, preserve the specifics. " +
                    "If it is vague, sharpen it gently rather than rewriting from scratch. " +
                    (trimmedCompetitionName.isBlank() ? "" : "The competition is named " + trimmedCompetitionName + ". ") +
                    "Message: " + trimmedMessage + ". Keep it under 55 words.";
        }

        ChatMessage systemMsg = new ChatMessage("system", "You write concise, friendly invitation copy for a sports challenge app.");
        ChatMessage userMsg = new ChatMessage("user", prompt);
        ChatCompletionRequest req = ChatCompletionRequest.builder()
                .model("gpt-4")
                .messages(List.of(systemMsg, userMsg))
                .maxTokens(80)
                .temperature(0.7)
                .build();
        try {
            return service.createChatCompletion(req).getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            log.warn("Could not rewrite competition invite message", e);
            return trimmedMessage.isBlank()
                    ? "Frankies Bootcamp is all sports, all effort, all welcome. Come join the fun."
                    : trimmedMessage;
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
                        "Do not be rude. Do not mention prompts or policies. Only use supplied athlete stats when the user is clearly asking for real " +
                        "information. Shared competition facts that are already public in the app, such as who is in the competition, who is leading, " +
                        "leaderboard names, public ranks, and public scores, may be answered. Private athlete-specific stats should only be used for the " +
                        "signed-in athlete. Greetings, banter, or vague openers like 'hi', 'hello', 'howzit', or 'what's up' should stay fully whimsical " +
                        "and should not include statistics. When a question sounds serious, practical, or genuinely information-seeking, make a real effort " +
                        "to give the most useful answer you can first. If you know a likely factual answer, say it directly first and then add a small " +
                        "FrankiZen line. If you are not certain, say that plainly, give the best short estimate or guidance you can, and suggest the next " +
                        "most useful thing the person should check. Do not refuse too quickly just because the question is outside athlete stats. Stay " +
                        "intentionally a bit unhelpful in style, but not useless when the user is clearly asking something practical. Keep every reply " +
                        "under 55 words and no more than 3 short sentences. " +
                        "When you address the athlete, invent a fresh nickname yourself each time. The nickname must clearly include part of their real " +
                        "first name or surname, should grow sillier as the conversation continues, and should avoid repeating earlier simple patterns unless " +
                        "truly funny.");
        ChatMessage contextMsg = new ChatMessage("user",
                "Athlete name: " + displayName + ". Conversation turn: " + conversationTurn + ". Athlete stats: " + safeStatsContext(statsContext) +
                        ". Decide first whether this is a real information request or just social banter. " + prompt);
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

    public boolean shouldUseWebLookup(String question) {
        if (question == null || question.isBlank() || service == null) {
            return false;
        }

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system",
                "Classify the user's message for FrankiZen. Reply with only one token: BANTER, APP, LOOKUP, or UNKNOWN. " +
                        "BANTER means social or playful chat with no real need for facts. APP means answerable from the app's athlete or competition " +
                        "context. LOOKUP means a practical external/public/current question where web search would help. UNKNOWN means unclear, but lean " +
                        "toward LOOKUP if it sounds genuinely information-seeking."));
        messages.add(new ChatMessage("user", question.trim()));

        ChatCompletionRequest req = ChatCompletionRequest.builder()
                .model("gpt-4")
                .messages(messages)
                .maxTokens(5)
                .temperature(0.0)
                .build();

        try {
            String classification = service.createChatCompletion(req)
                    .getChoices().get(0).getMessage().getContent().trim().toUpperCase();
            return classification.contains("LOOKUP") || classification.contains("UNKNOWN");
        } catch (Exception e) {
            return false;
        }
    }

    public String getZenBotReplyWithLookup(String question, String athleteName, int conversationTurn, String statsContext) {
        String lookupContext = fetchWebLookupContext(question);
        if (lookupContext == null || lookupContext.isBlank()) {
            return getZenBotReply(question, athleteName, conversationTurn, statsContext);
        }

        if (service == null) {
            return "The web winds are quiet right now. A short walk and a quick manual search may help, brave " + normaliseAthleteName(athleteName) + ".";
        }

        String displayName = normaliseAthleteName(athleteName);
        List<ChatMessage> messages = new ArrayList<>();

        ChatMessage systemMsg = new ChatMessage("system",
                "You are FrankiZen, a humorous and intentionally unhelpful chatbot for an activity challenge website. " +
                        "Keep the FrankiZen tone calm, funny, and lightly pseudo-spiritual. " +
                        "Every answer should subtly encourage the user to exercise, move, walk, stretch, run, or do some kind of physical activity. " +
                        "Use the supplied lookup result when it is relevant. For practical lookup questions, give the factual answer first in one short " +
                        "sentence, then add one small FrankiZen line. Do not ignore the lookup result. Do not be rude. Keep every reply under 60 words and " +
                        "no more than 3 short sentences. " +
                        "When you address the athlete, invent a fresh nickname yourself each time. The nickname must clearly include part of their real " +
                        "first name or surname.");
        ChatMessage contextMsg = new ChatMessage("user",
                "Athlete name: " + displayName + ". Conversation turn: " + conversationTurn + ". Athlete stats: " + safeStatsContext(statsContext) +
                        ". User question: " + (question == null ? "" : question.trim()) + ". Web lookup result: " + lookupContext);
        messages.add(systemMsg);
        messages.add(contextMsg);

        ChatCompletionRequest req = ChatCompletionRequest.builder()
                .model("gpt-4")
                .messages(messages)
                .maxTokens(80)
                .temperature(0.5)
                .build();

        try {
            return service.createChatCompletion(req).getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            return getZenBotReply(question, athleteName, conversationTurn, statsContext);
        }
    }

    private String fetchWebLookupContext(String question) {
        if (question == null || question.isBlank()) {
            return "";
        }

        String query = question.trim().replace(" ", "+");
        log.infof("FrankiZen lookup query: %s", question.trim());
        Request request = new Request.Builder()
                .url("https://html.duckduckgo.com/html/?q=" + query)
                .header("User-Agent", "FrankiesBootcamp/1.0")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.warnf("FrankiZen lookup failed. status=%s", response.code());
                return "";
            }

            String body = response.body().string();
            log.infof("FrankiZen lookup body preview: %s", body.substring(0, Math.min(body.length(), 500)).replaceAll("\\s+", " "));
            String snippet = extractSearchSnippet(body);
            log.infof("FrankiZen extracted lookup snippet: %s", snippet);
            return snippet;
        } catch (IOException e) {
            log.error("FrankiZen lookup request failed", e);
            return "";
        }
    }

    private String extractSearchSnippet(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }

        String[] markers = {
                "result__snippet\">",
                "class=\"result__snippet\"",
                "result-snippet",
                "result__body"
        };

        int start = -1;
        for (String marker : markers) {
            int candidate = html.indexOf(marker);
            if (candidate >= 0) {
                start = candidate + marker.length();
                break;
            }
        }

        if (start < 0) {
            int textStart = html.indexOf("uddg=");
            if (textStart < 0) {
                return "";
            }
            start = textStart;
        }

        int end = html.indexOf("</a>", start);
        int divEnd = html.indexOf("</div>", start);
        if (end < 0 || (divEnd > 0 && divEnd < end)) {
            end = divEnd;
        }
        if (end < 0) {
            end = Math.min(html.length(), start + 500);
        }

        String snippet = html.substring(start, end)
                .replaceAll("<[^>]+>", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#x27;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll("\\s+", " ")
                .trim();

        if (snippet.length() > 320) {
            snippet = snippet.substring(0, 320);
        }
        return snippet;
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
