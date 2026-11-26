package branches;

import model.Urgency;

/**
 * Observer interface (Observer Design Pattern) implemented by all military branch endpoints
 * that subscribe to receive decrypted and classified intelligence from {@link Ultra.Ultra}.
 */
public interface Observer {

    /**
     * Called by the {@link Ultra.Ultra} orchestrator when a decrypted message is routed
     * to this military branch.
     *
     * @param decryptedText the recovered plaintext intelligence message
     * @param urgencyLevel  the assessed urgency level of the message
     */
    void update(String decryptedText, Urgency urgencyLevel);
}
