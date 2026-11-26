package decrypt;

import model.Message;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Concrete {@link DecryptionStrategy} that breaks polyalphabetic Vigenère ciphers
 * without prior knowledge of the encryption keyword.
 * <p>
 * Uses a two-phase classical cryptanalysis approach:
 * <ol>
 *   <li><b>Kasiski Examination ({@link #findKeyLengthWithKasiski(String)}):</b> Scans the normalized
 *       ciphertext for repeated 3-letter sequences (trigrams), measures the distances between consecutive
 *       occurrences, and tallies which candidate key lengths (2..{@link #MAX_KEY_LENGTH}) evenly divide
 *       those distances. The factor with the most votes is chosen as the estimated key length.</li>
 *   <li><b>Per-Column Frequency Analysis ({@link #findKeyWords(String, int)}):</b> Splits the ciphertext
 *       into {@code keyLength} interleaved Caesar streams (cosets). For each stream, identifies the most
 *       frequent ciphertext character and assumes it maps to {@code 'E'} (the most common letter in English)
 *       to recover each letter of the secret keyword.</li>
 * </ol>
 */
public class VigenereCracker implements DecryptionStrategy {

    /** Number of letters in the English alphabet. */
    private static final int ALPHABET_SIZE = 26;

    /** Maximum candidate keyword length evaluated during Kasiski factor voting. */
    private static final int MAX_KEY_LENGTH = 10;

    /**
     * Breaks a Vigenère-encrypted message by estimating the keyword length via Kasiski examination,
     * recovering the keyword via frequency analysis, and decrypting the original ciphertext.
     *
     * @param message the intercepted message containing Vigenère ciphertext
     * @return the decrypted English plaintext, or an empty string if the input is null/empty
     */
    @Override
    public String decrypt(Message message) {
        String rawMessage = message.getRawCiphertext();
        if (rawMessage == null || rawMessage.isEmpty()) return "";

        // Strip punctuation/spaces and normalize to uppercase for statistical analysis
        String cleanText = rawMessage.replaceAll("[^a-zA-Z]", "").toUpperCase();

        int guessedKeyLength = findKeyLengthWithKasiski(cleanText);
        String discoveredKey = findKeyWords(cleanText, guessedKeyLength);

        return decryptWithKey(rawMessage, discoveredKey);
    }

    /**
     * Estimates the Vigenère keyword length using the Kasiski Examination method.
     * <p>
     * Finds all repeated 3-character substrings (trigrams), computes the positional distances
     * between consecutive occurrences, and counts how often each candidate factor (from 2 to
     * {@link #MAX_KEY_LENGTH}) divides those distances evenly.
     *
     * @param text uppercase alphabetic-only ciphertext string
     * @return the most likely keyword length (between 1 and {@link #MAX_KEY_LENGTH})
     */
    private int findKeyLengthWithKasiski(String text) {
        Map<String, List<Integer>> sequences = new HashMap<>();

        // Record starting positions of every 3-letter trigram
        for (int i = 0; i < text.length() - 3; i++) {
            String seq = text.substring(i, i + 3);
            sequences.computeIfAbsent(seq, k -> new ArrayList<>()).add(i);
        }

        int[] keyLengthCounters = new int[MAX_KEY_LENGTH + 1];

        // For trigrams appearing more than once, factorize the distances between consecutive occurrences
        for (Map.Entry<String, List<Integer>> entry : sequences.entrySet()) {
            List<Integer> positions = entry.getValue();

            if (positions.size() > 1) {
                for (int i = 0; i < positions.size() - 1; i++) {
                    int distance = positions.get(i + 1) - positions.get(i);

                    for (int factor = 2; factor <= MAX_KEY_LENGTH; factor++) {
                        if (distance % factor == 0) {
                            keyLengthCounters[factor]++;
                        }
                    }
                }
            }
        }

        // Select the candidate key length with the highest number of divisible distance votes
        int bestKeyLength = 1;
        int maxVotes = -1;
        for (int i = 2; i <= MAX_KEY_LENGTH; i++) {
            if (keyLengthCounters[i] > maxVotes) {
                maxVotes = keyLengthCounters[i];
                bestKeyLength = i;
            }
        }

        return bestKeyLength;
    }

    /**
     * Recovers the Vigenère keyword of a given {@code keyLength} by performing monographic
     * frequency analysis on each interleaved column of the ciphertext.
     * <p>
     * Assumes that the most frequent ciphertext character in each column corresponds to the
     * English letter {@code 'E'}, and calculates the Caesar shift for that key position accordingly.
     *
     * @param text      uppercase alphabetic-only ciphertext
     * @param keyLength the estimated length of the keyword
     * @return the recovered uppercase keyword
     */
    private String findKeyWords(String text, int keyLength) {
        StringBuilder key = new StringBuilder();

        for (int i = 0; i < keyLength; i++) {
            int[] counts = new int[ALPHABET_SIZE];
            for (int j = i; j < text.length(); j += keyLength) {
                counts[text.charAt(j) - 'A']++;
            }

            int maxCount = -1;
            char mostFrequentChar = 'A';
            for (int k = 0; k < ALPHABET_SIZE; k++) {
                if (counts[k] > maxCount) {
                    maxCount = counts[k];
                    mostFrequentChar = (char) ('A' + k);
                }
            }

            // Compute shift assuming the most frequent letter encrypted 'E'
            int shift = (mostFrequentChar - 'E' + ALPHABET_SIZE) % ALPHABET_SIZE;
            key.append((char) ('A' + shift));
        }

        return key.toString();
    }

    /**
     * Decrypts the original ciphertext using the discovered keyword while preserving original
     * character casing, whitespace, and punctuation. Only alphabetic characters advance the key index.
     *
     * @param ciphertext the raw ciphertext string (including spaces and punctuation)
     * @param key        the recovered Vigenère keyword
     * @return the decrypted plaintext string
     */
    private String decryptWithKey(String ciphertext, String key) {
        StringBuilder sb = new StringBuilder();
        int keyIndex = 0;

        for (char c : ciphertext.toCharArray()) {
            if (Character.isLetter(c)) {
                char base = Character.isUpperCase(c) ? 'A' : 'a';
                int shift = Character.toUpperCase(key.charAt(keyIndex)) - 'A';

                int alphabetPosition = c - base;
                int newPosition = (alphabetPosition - shift + ALPHABET_SIZE) % ALPHABET_SIZE;

                sb.append((char) (base + newPosition));
                keyIndex = (keyIndex + 1) % key.length();
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
