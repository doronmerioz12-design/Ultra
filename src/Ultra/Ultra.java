package Ultra;

import branches.Observer;
import classify.LlmMessageClassifier;
import classify.MessageClassifier;
import decrypt.DecryptionStrategy;
import model.Branch;
import model.ClassificationResult;
import model.Message;
import model.Urgency;
import InputAndProcess.Repository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Ultra {

    private final Map<Branch, Observer> observers = new EnumMap<>(Branch.class);
    private final Repository repository = new Repository();
    private final MessageClassifier classifier = new LlmMessageClassifier();

    public void registerObserver(Branch branch, Observer o) {
        observers.put(branch, o);
    }

    public void unregisterObserver(Branch branch) {
        observers.remove(branch);
    }

    /**
     * Main processing loop: fetches pending messages, decrypts, classifies,
     * and routes to the appropriate branch observer(s).
     */
    public void processMessages() {
        List<Message> pending = repository.fetchPendingMessages();

        if (pending.isEmpty()) {
            System.out.println("[Ultra] No pending messages to process.");
            return;
        }

        for (Message msg : pending) {
            try {
                // 1. Pick decryption strategy based on cipher type
                DecryptionStrategy strategy =
                        CipherFactory.getDecryptionStrategy(msg.getCipherType());

                // 2. Decrypt
                String decryptedText = strategy.decrypt(msg);

                // 3. Classify (destination + urgency)
                ClassificationResult result = classifier.classify(msg, decryptedText);

                // 4. Route to the right observer(s)
                notifyObservers(result.getBranch(), decryptedText, result.getUrgency());

                // 5. Mark as done
                repository.markAsDone(msg.getId());

                System.out.println("[Ultra] Message " + msg.getId()
                        + " processed → " + result.getBranch() + " | " + result.getUrgency());

            } catch (Exception e) {
                System.err.println("[Ultra] Failed to process message "
                        + msg.getId() + ": " + e.getMessage());
                repository.markAsFailed(msg.getId());
            }
        }
    }

    private void notifyObservers(Branch branch, String decryptedText, Urgency urgency) {
        if (branch == Branch.ALL) {
            // Broadcast to every registered observer
            for (Observer o : observers.values()) {
                o.update(decryptedText, urgency);
            }
        } else {
            Observer target = observers.get(branch);
            if (target != null) {
                target.update(decryptedText, urgency);
            } else {
                System.err.println("[Ultra] No observer registered for branch: " + branch);
            }
        }
    }
}
