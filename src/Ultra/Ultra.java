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

/**
 * Central orchestrator of the Bletchley Park intelligence pipeline and the Subject
 * in the Observer design pattern.
 * <p>
 * Coordinates the end-to-end processing of intercepted transmissions:
 * <ol>
 *   <li>Retrieves pending encrypted messages from the {@link Repository}.</li>
 *   <li>Selects the appropriate {@link DecryptionStrategy} via {@link CipherFactory}.</li>
 *   <li>Cracks the ciphertext into readable plaintext.</li>
 *   <li>Classifies the plaintext's target military branch and urgency using {@link MessageClassifier}.</li>
 *   <li>Dispatches the decrypted intelligence to the matching {@link Observer} (or broadcasts to all).</li>
 *   <li>Updates the message status in the database to {@code DONE} or {@code FAILED}.</li>
 * </ol>
 */
public class Ultra {

    /** Registry mapping each military branch enum to its subscribed Observer instance. */
    private final Map<Branch, Observer> observers = new EnumMap<>(Branch.class);

    /** Data access layer for polling and updating the INPUT message queue table. */
    private final Repository repository = new Repository();

    /** AI-backed classifier that determines the destination branch and urgency of decrypted text. */
    private final MessageClassifier classifier = new LlmMessageClassifier();

    /**
     * Subscribes a military branch observer to receive decrypted intelligence routed to that branch.
     *
     * @param branch the military branch identifier
     * @param o      the observer instance responsible for handling messages for that branch
     */
    public void registerObserver(Branch branch, Observer o) {
        observers.put(branch, o);
    }

    /**
     * Unsubscribes a military branch observer from receiving further intelligence updates.
     *
     * @param branch the military branch identifier to remove
     */
    public void unregisterObserver(Branch branch) {
        observers.remove(branch);
    }

    /**
     * Main processing loop: fetches a batch of pending messages, decrypts each one,
     * classifies its content, routes it to the appropriate branch observer(s),
     * and records the outcome in the database.
     */
    public void processMessages() {
        List<Message> pending = repository.fetchPendingMessages();

        if (pending.isEmpty()) {
            System.out.println("[Ultra] No pending messages to process.");
            return;
        }

        for (Message msg : pending) {
            try {
                // 1. Pick decryption strategy based on cipher type (Factory + Strategy patterns)
                DecryptionStrategy strategy =
                        CipherFactory.getDecryptionStrategy(msg.getCipherType());

                // 2. Decrypt ciphertext into plaintext
                String decryptedText = strategy.decrypt(msg);

                // 3. Classify plaintext (destination branch + urgency level)
                ClassificationResult result = classifier.classify(msg, decryptedText);

                // 4. Route to the right observer(s)
                notifyObservers(result.getBranch(), decryptedText, result.getUrgency());

                // 5. Mark message as DONE in the INPUT table
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

    /**
     * Dispatches a decrypted message and its urgency level to the target branch observer.
     * If the target branch is {@link Branch#ALL}, broadcasts the message to every registered observer.
     *
     * @param branch        the destination branch determined by classification
     * @param decryptedText the recovered plaintext message
     * @param urgency       the assessed urgency level of the message
     */
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
