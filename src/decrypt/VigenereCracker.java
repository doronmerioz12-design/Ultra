package decrypt;

import model.Message;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VigenereCracker implements DecryptionStrategy {

    private static final int ALPHABET_SIZE = 26;
    private static final int MAX_KEY_LENGTH = 10;

    @Override
    public String decrypt(Message message) {
        String rawMessage = message.getRawCiphertext();
        if (rawMessage == null || rawMessage.isEmpty()) return "";

        String cleanText = rawMessage.replaceAll("[^a-zA-Z]", "").toUpperCase();

        int guessedKeyLength = findKeyLengthWithKasiski(cleanText);
        String discoveredKey = findKeyWords(cleanText, guessedKeyLength);

        return decryptWithKey(rawMessage, discoveredKey);
    }

    private int findKeyLengthWithKasiski(String text) {
        Map<String, List<Integer>> sequences = new HashMap<>();

        for (int i = 0; i < text.length() - 3; i++) {
            String seq = text.substring(i, i + 3);
            sequences.computeIfAbsent(seq, k -> new ArrayList<>()).add(i);
        }

        int[] keyLengthCounters = new int[MAX_KEY_LENGTH + 1];

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

            int shift = (mostFrequentChar - 'E' + ALPHABET_SIZE) % ALPHABET_SIZE;
            key.append((char) ('A' + shift));
        }

        return key.toString();
    }

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