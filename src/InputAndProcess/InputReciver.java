package InputAndProcess;

/**
 * Contract for signal interception sources capable of receiving encrypted transmissions
 * and logging them into the system for downstream processing.
 */
public interface InputReciver {

    /**
     * Captures one or more encrypted messages from an input source and persists them.
     */
    public void receive();
}
