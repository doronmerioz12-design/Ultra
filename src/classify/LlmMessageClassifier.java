package classify;

import model.Branch;
import model.ClassificationResult;
import model.Message;
import model.Urgency;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class LlmMessageClassifier implements MessageClassifier {

    private static final long RATE_LIMIT_DELAY_MS = 2000;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_WAIT_MS = 30000; // 30 seconds for rate limit

    @Override
    public ClassificationResult classify(Message message, String decryptedText) {
        if (decryptedText == null || decryptedText.isEmpty()) {
            return new ClassificationResult(Branch.ALL, Urgency.ROUTINE);
        }

        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("[Gemini Classifier] ERROR: GEMINI_API_KEY environment variable is missing!");
            return new ClassificationResult(Branch.ALL, Urgency.ROUTINE);
        }

        String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" +
                apiKey;

        String prompt = "Analyze the following decrypted military message: \"" + decryptedText + "\". "
                + "Determine its destination branch and its urgency level. "
                + "The 'destination' field MUST be exactly one of the following: NAVY, RAF, ARMY, PM_OFFICE, SHAEF, ALL. "
                + "The 'urgency' field MUST be exactly one of the following: LOW, MEDIUM, HIGH, CRITICAL, ROUTINE. "
                + "You must respond ONLY with a raw JSON object matching this schema: "
                + "{\"destination\": \"string\", \"urgency\": \"string\"}. "
                + "Do not include markdown formatting or backticks.";

        try {
            ObjectMapper mapper = new ObjectMapper();
            HttpClient client = HttpClient.newHttpClient();

            ObjectNode textNode = mapper.createObjectNode().put("text", prompt);
            ArrayNode partsNode = mapper.createArrayNode().add(textNode);
            ObjectNode contentNode = mapper.createObjectNode().set("parts", partsNode);
            ArrayNode contentsArray = mapper.createArrayNode().add(contentNode);
            ObjectNode rootInput = mapper.createObjectNode().set("contents", contentsArray);

            String requestBody = mapper.writeValueAsString(rootInput);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(geminiUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // Retry loop for rate limiting (429)
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {

                Thread.sleep(RATE_LIMIT_DELAY_MS);

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode rootResponse = mapper.readTree(response.body());
                    String geminiOutput = rootResponse
                            .path("candidates")
                            .path(0)
                            .path("content")
                            .path("parts")
                            .path(0)
                            .path("text")
                            .asText();

                    JsonNode innerJson = mapper.readTree(geminiOutput.trim());

                    if (innerJson.has("destination") && innerJson.has("urgency")) {
                        String destination = innerJson.get("destination").asText();
                        String urgency = innerJson.get("urgency").asText();
                        return ClassificationResult.parse(destination + "|" + urgency);
                    }
                } else if (response.statusCode() == 429 && attempt < MAX_RETRIES) {
                    System.out.println("[Gemini Classifier] Rate limited. Waiting "
                            + (RETRY_WAIT_MS / 1000) + "s before retry " + (attempt + 1) + "/" + MAX_RETRIES + "...");
                    Thread.sleep(RETRY_WAIT_MS);
                } else {
                    System.err.println("[Gemini API Error] Status Code: " + response.statusCode());
                    System.err.println("[Gemini API Error] Response Body: " + response.body());
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[Gemini Classifier] Interrupted during classification.");
        } catch (Exception e) {
            System.err.println("[Gemini Classifier] REST call failed: " + e.getMessage());
        }

        return new ClassificationResult(Branch.ALL, Urgency.ROUTINE);
    }
}
