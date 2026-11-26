package decrypt;

import model.Message;

/**
 * Concrete {@link DecryptionStrategy} that breaks Caesar-ciphered messages without
 * prior knowledge of the shift key using statistical Chi-Squared frequency analysis.
 * <p>
 * Algorithm overview:
 * <ol>
 *   <li>Generates all 26 possible shift candidate strings.</li>
 *   <li>Scores each candidate against the standard English letter frequency distribution
 *       (A–Z) using the Chi-Squared statistic: {@code sum((observed - expected)^2 / expected)}.</li>
 *   <li>Selects and returns the candidate with the lowest Chi-Squared score (closest fit to natural English).</li>
 * </ol>
 */
public class CeaserDecrypt implements DecryptionStrategy {

    /** Size of the standard English alphabet (number of possible Caesar shifts). */
    private static final int CYCLE = 26;

    /** Standard relative frequencies of English letters (A through Z). */
    private static final double[] ENGLISH_FREQ = {
            0.0817, 0.0150, 0.0278, 0.0425, 0.1270, 0.0223, 0.0202,
            0.0609, 0.0697, 0.0015, 0.0077, 0.0403, 0.0241, 0.0675,
            0.0751, 0.0193, 0.0010, 0.0599, 0.0633, 0.0906, 0.0276,
            0.0098, 0.0236, 0.0015, 0.0197, 0.0007
    };

    /**
     * Breaks a Caesar-encrypted message by testing all 26 shifts and returning
     * the candidate that best matches English letter frequencies.
     *
     * @param message the intercepted message containing the Caesar ciphertext
     * @return the most likely decrypted English plaintext, or an empty string if the input is null/empty
     */
    @Override
    public String decrypt(Message message) {
        String rawMessage = message.getRawCiphertext();

        if (rawMessage == null || rawMessage.isEmpty()) {
            return "";
        }

        // Generate all 26 shift candidates
        String[] answers = new String[CYCLE];
        for (int shift = 0; shift < CYCLE; shift++) {
            answers[shift] = decryptWithShift(rawMessage, shift);
        }

        return selectBestByFrequency(answers);
    }

    /**
     * Evaluates each candidate decryption against known English letter frequencies
     * using the Chi-Squared statistic and returns the candidate with the minimum score.
     *
     * @param candidates array of 26 candidate plaintext strings
     * @return the candidate string with the lowest Chi-Squared deviation from English
     */
    private String selectBestByFrequency(String[] candidates) {
        double bestScore = Double.MAX_VALUE;
        int bestIndex = 0;

        for (int i = 0; i < candidates.length; i++) {
            double score = chiSquaredScore(candidates[i]);
            if (score < bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }

        return candidates[bestIndex];
    }

    /**
     * Computes the Chi-Squared goodness-of-fit score comparing the observed letter distribution
     * in {@code text} against the expected English letter distribution ({@link #ENGLISH_FREQ}).
     * Lower values indicate a closer match to natural English.
     *
     * @param text the candidate string to score
     * @return the Chi-Squared score, or {@link Double#MAX_VALUE} if the string contains no letters
     */
    private double chiSquaredScore(String text) {
        int[] counts = new int[CYCLE];
        int totalLetters = 0;

        for (char c : text.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                counts[c - 'a']++;
                totalLetters++;
            } else if (c >= 'A' && c <= 'Z') {
                counts[c - 'A']++;
                totalLetters++;
            }
        }

        if (totalLetters == 0) return Double.MAX_VALUE;

        double score = 0.0;
        for (int i = 0; i < CYCLE; i++) {
            double expected = ENGLISH_FREQ[i] * totalLetters;
            double diff = counts[i] - expected;
            score += (diff * diff) / expected;
        }

        return score;
    }

    /**
     * Shifts all alphabetic characters in {@code rawMessage} backward by {@code shift} positions
     * modulo 26, preserving original uppercase/lowercase casing and non-alphabetic characters.
     *
     * @param rawMessage the ciphertext string
     * @param shift      the backward shift offset (0–25)
     * @return the shifted candidate string
     */
    private String decryptWithShift(String rawMessage, int shift) {
        char[] chars = rawMessage.toCharArray();
        StringBuilder sb = new StringBuilder();
        for (char c : chars) {
            if (Character.isLetter(c)) {
                char base = Character.isUpperCase(c) ? 'A' : 'a';
                int alphabetPosition = c - base;
                int newPosition = (alphabetPosition - shift + CYCLE) % CYCLE;
                c = (char) (base + newPosition);
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
