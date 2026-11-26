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

/**
 * AI-powered implementation of {@link MessageClassifier} that uses the Google Gemini REST API
 * ({@code gemini-3-flash-preview}) to semantically analyze decrypted military messages.
 * <p>
 * Features:
 * <ul>
 *   <li>Constructs a structured prompt enforcing a strict JSON response schema with
 *       {@code destination} ({@link Branch}) and {@code urgency} ({@link Urgency}) fields.</li>
 *   <li>Includes built-in request pacing ({@link #RATE_LIMIT_DELAY_MS}) and automatic retry
 *       with backoff ({@link #RETRY_WAIT_MS}) when HTTP {@code 429} rate-limit responses occur.</li>
 *   <li>Gracefully falls back to {@code Branch.ALL | Urgency.ROUTINE} if the API key is missing,
 *       a network error occurs, or the response cannot be parsed.</li>
 * </ul>
 */
public class LlmMessageClassifier implements MessageClassifier {

    /** Base delay (in milliseconds) before each API call to respect free-tier rate limits. */
    private static final long RATE_LIMIT_DELAY_MS = 2000;

    /** Maximum number of attempts when retrying rate-limited (HTTP 429) requests. */
    private static final int MAX_RETRIES = 3;

    /** Backoff duration (in milliseconds) to wait after receiving an HTTP 429 status code. */
    private static final long RETRY_WAIT_MS = 30000; // 30 seconds for rate limit

    /**
     * Sends the decrypted plaintext to the Google Gemini API for semantic classification
     * into a target military branch and urgency level.
     *
     * @param message       the original intercepted message object
     * @param decryptedText the recovered plaintext to analyze
     * @return the parsed {@link ClassificationResult}, or {@code ALL | ROUTINE} on fallback
     */
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

        String geminiUrl =
                "https://generativelanguage.googleapis.com/v1beta" +
                        "/models/gemini-3-flash-preview:generateContent?key=" + apiKey;

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

            // Build the Gemini generateContent JSON request payload: {"contents": [{"parts": [{"text": "..."}]}]}
            ObjectNode textNode = mapper.createObjectNode().put("text", prompt);
            ArrayNode partsNode = mapper.createArrayNode().add(textNode);
            ObjectNode contentNode = mapper.createObjectNode().set("parts", partsNode);
            ArrayNode contentsArray = mapper.createArrayNode().add(contentNode);
            ObjectNode rootInput = mapper.createObjectNode().set("contents", contentsArray);

            String requestBody = mapper.writeValueAsString(rootInput);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(geminiUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // Retry loop for rate limiting (HTTP 429)
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

        // Fail-safe default routing when classification is unavailable
        return new ClassificationResult(Branch.ALL, Urgency.ROUTINE);
    }
}
