package decrypt;

import model.Message;

public class CeaserDecrypt implements DecryptionStrategy {

    private static final int CYCLE = 26;

    // English letter frequency (A-Z), used for scoring candidate decryptions
    private static final double[] ENGLISH_FREQ = {
            0.0817, 0.0150, 0.0278, 0.0425, 0.1270, 0.0223, 0.0202,
            0.0609, 0.0697, 0.0015, 0.0077, 0.0403, 0.0241, 0.0675,
            0.0751, 0.0193, 0.0010, 0.0599, 0.0633, 0.0906, 0.0276,
            0.0098, 0.0236, 0.0015, 0.0197, 0.0007
    };

    @Override
    public String decrypt(Message message) {
        String rawMessage = message.getRawCiphertext();

        if (rawMessage == null || rawMessage.isEmpty()) {
            return "";
        }

        String[] answers = new String[CYCLE];
        for (int shift = 0; shift < CYCLE; shift++) {
            answers[shift] = decryptWithShift(rawMessage, shift);
        }

        return selectBestByFrequency(answers);
    }

    /**
     * Scores each candidate decryption against known English letter frequencies
     * using chi-squared statistic, and returns the best match.
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
