package Ultra;

import decrypt.CeaserDecrypt;
import decrypt.DecryptionStrategy;
import decrypt.VigenereCracker;
import model.CipherType;

/**
 * Factory class (Factory Design Pattern) responsible for resolving and
 * instantiating the appropriate {@link DecryptionStrategy} implementation
 * based on a raw cipher identifier.
 *
 * <p>Delegates input normalization and validation to {@link CipherType#fromString(String)}
 * so case differences or surrounding whitespace are handled safely before
 * selecting a concrete cryptanalysis strategy.</p>
 */
public class CipherFactory {

    /**
     * Resolves and returns a {@link DecryptionStrategy} for the given cipher name.
     *
     * @param cipher raw cipher identifier (e.g., {@code "CAESAR"} or {@code "VIGENERE"},
     *               case-insensitive)
     * @return a concrete {@link DecryptionStrategy} instance matching {@code cipher}
     * @throws IllegalArgumentException if {@code cipher} is {@code null}, blank,
     *                                  or does not match any supported {@link CipherType}
     */
    public static DecryptionStrategy getDecryptionStrategy(String cipher) {
        CipherType type = CipherType.fromString(cipher)
                .orElseThrow(() -> new IllegalArgumentException("Invalid cipher: " + cipher));

        return switch (type) {
            case CAESAR -> new CeaserDecrypt();
            case VIGENERE -> new VigenereCracker();
        };
    }
}