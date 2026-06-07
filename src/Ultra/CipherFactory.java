package Ultra;
import decrypt.CeaserDecrypt;
import decrypt.DecryptionStrategy;
import decrypt.VigenereCracker;

public class CipherFactory {

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

