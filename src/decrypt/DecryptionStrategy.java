package decrypt;

import model.Message;

/**
 * Strategy interface (Strategy Design Pattern) for cryptanalysis algorithms
 * capable of breaking an encrypted {@link Message} and recovering its plaintext.
 */
public interface DecryptionStrategy {

    /**
     * Breaks the cipher on the provided message and returns the recovered English plaintext.
     *
     * @param message the intercepted encrypted message
     * @return the decrypted plaintext string
     */
    String decrypt(Message message);
}
