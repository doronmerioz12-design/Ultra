package classify;

import model.ClassificationResult;
import model.Message;

public interface MessageClassifier {
    ClassificationResult classify(Message message, String decryptedText);
}
