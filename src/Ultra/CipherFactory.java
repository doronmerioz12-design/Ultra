package Ultra;

import decrypt.CeaserDecrypt;
import decrypt.DecryptionStrategy;
import decrypt.VigenereCracker;

/**
 * Factory class (Factory Design Pattern) responsible for instantiating the appropriate
 * {@link DecryptionStrategy} based on the cipher type tagged on an intercepted message.
 */
public class CipherFactory {

    /**
     * Returns a concrete {@link DecryptionStrategy} implementation capable of breaking
     * the specified cipher type.
     *
     * @param cipher the cipher identifier (e.g., {@code "CAESAR"} or {@code "VIGENERE"})
     * @return the corresponding {@link DecryptionStrategy} instance
     * @throws IllegalArgumentException if the cipher type is unrecognized or unsupported
     */
    public static DecryptionStrategy getDecryptionStrategy(String cipher) {
        switch (cipher) {
            case "CAESAR":
                return new CeaserDecrypt();
            case "VIGENERE":
                return new VigenereCracker();
        }
        throw new IllegalArgumentException("Invalid cipher");
    }
}
