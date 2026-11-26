package classify;

import model.ClassificationResult;
import model.Message;

/**
 * Strategy interface for intelligence classifiers that analyze decrypted plaintext
 * and determine the target military branch and urgency level.
 */
public interface MessageClassifier {

    /**
     * Analyzes a decrypted message and produces a type-safe {@link ClassificationResult}
     * specifying its destination branch and urgency.
     *
     * @param message       the original intercepted message metadata
     * @param decryptedText the plaintext recovered by the cryptanalysis engine
     * @return a {@link ClassificationResult} containing the target {@link model.Branch} and {@link model.Urgency}
     */
    ClassificationResult classify(Message message, String decryptedText);
}
