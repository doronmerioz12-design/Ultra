package classify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.Branch;
import model.ClassificationResult;
import model.Message;
import model.Urgency;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * AI-powered implementation of {@link MessageClassifier} that uses the Google Gemini REST API
 * ({@value #MODEL_NAME}) to semantically classify decrypted military intelligence messages into a
 * destination {@link Branch} and {@link Urgency} level.
 *
 * <p>Key behaviors:
 * <ul>
 *   <li>Builds a structured prompt enforcing a raw JSON object with {@code destination} and
 *       {@code urgency} fields, then parses it via {@link ClassificationResult#parse(String)}.</li>
 *   <li>Applies request pacing ({@link #RATE_LIMIT_DELAY_MS}) and retries HTTP {@code 429}
 *       rate-limit responses with backoff ({@link #RETRY_WAIT_MS}).</li>
 *   <li>Falls back safely to {@code Branch.ALL | Urgency.ROUTINE} if the API key is absent,
 *       a network/timeout error occurs, or response parsing fails.</li>
 * </ul>
 */
public class LlmMessageClassifier implements MessageClassifier {

    /** Target Gemini model identifier used for intelligence triage. */
    private static final String MODEL_NAME = "gemini-2.5-flash";

    /** Base delay (in milliseconds) before each API request to smooth burst traffic. */
    private static final long RATE_LIMIT_DELAY_MS = 2000;

    /** Maximum number of attempts when retrying rate-limited ({@code 429}) requests. */
    private static final int MAX_RETRIES = 3;

    /** Backoff duration (in milliseconds) to wait before retrying after an HTTP {@code 429}. */
    private static final long RETRY_WAIT_MS = 30000;

    /** Shared JSON serializer/deserializer instance. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Shared HTTP client configured with explicit connect timeout and HTTP/1.1. */
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /**
     * Classifies a decrypted plaintext message into a target military branch and urgency level.
     *
     * @param message       the original intercepted message metadata
     * @param decryptedText the recovered plaintext message to analyze
     * @return a {@link ClassificationResult} containing branch and urgency, or
     *         {@code ALL | ROUTINE} when classification cannot be completed
     */
    @Override
    public ClassificationResult classify(Message message, String decryptedText) {
        if (decryptedText == null || decryptedText.isBlank()) {
            return new ClassificationResult(Branch.ALL, Urgency.ROUTINE);
        }

        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("[Gemini Classifier] ERROR: GEMINI_API_KEY environment variable is missing!");
            return new ClassificationResult(Branch.ALL, Urgency.ROUTINE);
        }

        String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/"
                + MODEL_NAME + ":generateContent?key=" + apiKey;

        String prompt = "Analyze the following decrypted military message: \"" + decryptedText + "\". "
                + "Determine its destination branch and its urgency level. "
                + "The 'destination' field MUST be exactly one of the following: NAVY, RAF, ARMY, PM_OFFICE, SHAEF, ALL. "
                + "The 'urgency' field MUST be exactly one of the following: LOW, MEDIUM, HIGH, CRITICAL, ROUTINE. "
                + "You must respond ONLY with a raw JSON object matching this schema: "
                + "{\"destination\": \"string\", \"urgency\": \"string\"}. "
                + "Do not include markdown formatting or backticks.";

        try {
            ObjectNode textNode = MAPPER.createObjectNode().put("text", prompt);
            ArrayNode partsNode = MAPPER.createArrayNode().add(textNode);
            ObjectNode contentNode = MAPPER.createObjectNode().set("parts", partsNode);
            ArrayNode contentsArray = MAPPER.createArrayNode().add(contentNode);
            ObjectNode rootInput = MAPPER.createObjectNode().set("contents", contentsArray);

            String requestBody = MAPPER.writeValueAsString(rootInput);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(geminiUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                Thread.sleep(RATE_LIMIT_DELAY_MS);

                HttpResponse<String> response = HTTP_CLIENT.send(
                        request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode rootResponse = MAPPER.readTree(response.body());
                    String geminiOutput = rootResponse
                            .path("candidates")
                            .path(0)
                            .path("content")
                            .path("parts")
                            .path(0)
                            .path("text")
                            .asText();

                    JsonNode innerJson = MAPPER.readTree(geminiOutput.trim());

                    if (innerJson.has("destination") && innerJson.has("urgency")) {
                        String destination = innerJson.get("destination").asText();
                        String urgency = innerJson.get("urgency").asText();
                        return ClassificationResult.parse(destination + "|" + urgency);
                    }
                } else if (response.statusCode() == 429 && attempt < MAX_RETRIES) {
                    System.out.println("[Gemini Classifier] Rate limited. Waiting "
                            + (RETRY_WAIT_MS / 1000) + "s before retry "
                            + (attempt + 1) + "/" + MAX_RETRIES + "...");
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
            System.err.println("[Gemini Classifier] REST call failed ("
                    + e.getClass().getSimpleName() + "): " + e.getMessage());
        }

        return new ClassificationResult(Branch.ALL, Urgency.ROUTINE);
    }
}