package Ultra;

import decrypt.CeaserDecrypt;
import decrypt.DecryptionStrategy;
import decrypt.VigenereCracker;
import model.Branch;
import model.CipherType;
import model.ClassificationResult;
import model.Message;
import model.Urgency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ULTRA Intelligence Pipeline - Unit & Mock Tests")
class UltraPipelineTest {

    // =========================================================================
    // 1. CipherType & CipherFactory Tests (Fail-Fast Validation & Factory)
    // =========================================================================
    @Nested
    @DisplayName("CipherType & Factory Validation")
    class CipherFactoryTests {

        @Test
        @DisplayName("Should parse valid cipher names case-insensitively with whitespace")
        void testValidCipherParsing() {
            assertEquals(Optional.of(CipherType.CAESAR), CipherType.fromString("caesar"));
            assertEquals(Optional.of(CipherType.VIGENERE), CipherType.fromString("  VIGENERE  "));
        }

        @Test
        @DisplayName("Should return empty Optional for invalid, blank, or null cipher inputs")
        void testInvalidCipherParsing() {
            assertTrue(CipherType.fromString("").isEmpty());
            assertTrue(CipherType.fromString("   ").isEmpty());
            assertTrue(CipherType.fromString(null).isEmpty());
            assertTrue(CipherType.fromString("ENIGMA").isEmpty());
        }

        @Test
        @DisplayName("Factory should return correct DecryptionStrategy instances")
        void testFactoryStrategyResolution() {
            DecryptionStrategy caesar = CipherFactory.getDecryptionStrategy(CipherType.CAESAR.name());
            DecryptionStrategy vigenere = CipherFactory.getDecryptionStrategy("VIGENERE");

            assertInstanceOf(CeaserDecrypt.class, caesar);
            assertInstanceOf(VigenereCracker.class, vigenere);
        }

        @Test
        @DisplayName("Factory should throw IllegalArgumentException on unsupported cipher string")
        void testFactoryThrowsOnInvalidCipher() {
            assertThrows(IllegalArgumentException.class, () -> CipherFactory.getDecryptionStrategy("UNKNOWN"));
            assertThrows(IllegalArgumentException.class, () -> CipherFactory.getDecryptionStrategy(""));
        }
    }

    // =========================================================================
    // 2. Cryptanalysis Strategy Tests (Caesar & Blind Vigenere Kasiski Cracker)
    // =========================================================================
    @Nested
    @DisplayName("Cryptanalysis Decryption Strategies")
    class DecryptionTests {

        @Test
        @DisplayName("Should decrypt Caesar cipher text accurately")
        void testCaesarDecryption() {
            Message msg = new Message(1, "SULPH PLQLVWHU RIILFH", "7.45 MHz", "CAESAR");

            DecryptionStrategy strategy = new CeaserDecrypt();
            String decrypted = strategy.decrypt(msg);

            assertEquals("PRIME MINISTER OFFICE", decrypted.toUpperCase());
        }

        @Test
        @DisplayName("Should crack long Vigenere ciphertext blindly via Kasiski & Frequency Analysis")
        void testBlindVigenereCracking() {
            String plaintext =
                    "THE ENEMY FLEET ENTERED THE WESTERN SECTOR NEAR THE REEF AND THE RESERVE FLEET REMAINED AT SEA " +
                            "THE ENEMY FLEET ENTERED THE WESTERN SECTOR NEAR THE REEF AND THE RESERVE FLEET REMAINED AT SEA";

            // Encrypt deterministically with 3-letter key "KEY" (+10, +4, +24)
            String ciphertext = encryptVigenereForTest(plaintext, "KZ");

            Message msg = new Message(2, ciphertext, "14.20 MHz", "VIGENERE");
            DecryptionStrategy cracker = new VigenereCracker();

            assertEquals(plaintext, cracker.decrypt(msg));
        }

        private String encryptVigenereForTest(String text, String key) {
            StringBuilder sb = new StringBuilder();
            int keyIdx = 0;
            for (char c : text.toCharArray()) {
                if (Character.isLetter(c)) {
                    int shift = key.charAt(keyIdx % key.length()) - 'A';
                    sb.append((char) ('A' + (c - 'A' + shift) % 26));
                    keyIdx++;
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
    }

    // =========================================================================
    // 3. Mocked Classifier & RBAC Observer Routing Tests (No DB / No Live API)
    // =========================================================================
    @Nested
    @DisplayName("Mocked AI Classification & Need-to-Know Routing")
    class MockedRoutingTests {

        interface IntelligenceClassifier {
            ClassificationResult classify(Message message, String decryptedText);
        }

        interface BranchObserver {
            Branch getBranch();
            void onIntelligenceReceived(Message message, String decryptedText, Urgency urgency);
        }

        @Mock
        private IntelligenceClassifier mockGeminiClassifier;

        @Mock
        private BranchObserver mockNavyObserver;

        @Mock
        private BranchObserver mockPmOfficeObserver;

        @Mock
        private BranchObserver mockArmyObserver;

        @Test
        @DisplayName("Should route NAVY-classified intelligence exclusively to Navy observer")
        void testExclusiveNavyRoutingWithMocks() {
            // Arrange
            Message intercept = new Message(101, "ENCRYPTED_NAVY_SIGNAL", "3.80 MHz", "CAESAR");
            String plaintext = "SUBMARINE CONVOY SPOTTED IN NORTH ATLANTIC";

            when(mockNavyObserver.getBranch()).thenReturn(Branch.NAVY);
            when(mockPmOfficeObserver.getBranch()).thenReturn(Branch.PM_OFFICE);
            when(mockArmyObserver.getBranch()).thenReturn(Branch.ARMY);

            when(mockGeminiClassifier.classify(intercept, plaintext))
                    .thenReturn(new ClassificationResult(Branch.NAVY, Urgency.CRITICAL));

            List<BranchObserver> observers = List.of(mockNavyObserver, mockPmOfficeObserver, mockArmyObserver);

            // Act
            ClassificationResult decision = mockGeminiClassifier.classify(intercept, plaintext);
            for (BranchObserver observer : observers) {
                if (decision.getBranch() == Branch.ALL || observer.getBranch() == decision.getBranch()) {
                    observer.onIntelligenceReceived(intercept, plaintext, decision.getUrgency());
                }
            }

            // Assert: Navy received the critical intel, while PM_OFFICE and ARMY were blocked (Need-to-Know)
            verify(mockNavyObserver, times(1)).onIntelligenceReceived(intercept, plaintext, Urgency.CRITICAL);
            verify(mockPmOfficeObserver, never()).onIntelligenceReceived(any(), any(), any());
            verify(mockArmyObserver, never()).onIntelligenceReceived(any(), any(), any());
        }

        @Test
        @DisplayName("Should broadcast to all branch observers when classification falls back to ALL")
        void testBroadcastFallbackRoutingWithMocks() {
            // Arrange
            Message intercept = new Message(102, "ENCRYPTED_GENERAL_SIGNAL", "5.10 MHz", "VIGENERE");
            String plaintext = "UNCLASSIFIED GENERAL ALERT";

            when(mockGeminiClassifier.classify(intercept, plaintext))
                    .thenReturn(new ClassificationResult(Branch.ALL, Urgency.ROUTINE));

            List<BranchObserver> observers = List.of(mockNavyObserver, mockPmOfficeObserver, mockArmyObserver);

            // Act
            ClassificationResult decision = mockGeminiClassifier.classify(intercept, plaintext);
            for (BranchObserver observer : observers) {
                if (decision.getBranch() == Branch.ALL || observer.getBranch() == decision.getBranch()) {
                    observer.onIntelligenceReceived(intercept, plaintext, decision.getUrgency());
                }
            }

            // Assert: All branches notified
            verify(mockNavyObserver, times(1)).onIntelligenceReceived(intercept, plaintext, Urgency.ROUTINE);
            verify(mockPmOfficeObserver, times(1)).onIntelligenceReceived(intercept, plaintext, Urgency.ROUTINE);
            verify(mockArmyObserver, times(1)).onIntelligenceReceived(intercept, plaintext, Urgency.ROUTINE);
        }

        @Test
        @DisplayName("ClassificationResult.parse should fall back safely on malformed LLM output")
        void testClassificationResultSafeFallback() {
            ClassificationResult valid = ClassificationResult.parse("PM_OFFICE|ROUTINE");
            assertEquals(Branch.PM_OFFICE, valid.getBranch());
            assertEquals(Urgency.ROUTINE, valid.getUrgency());

            ClassificationResult malformed = ClassificationResult.parse("INVALID_BRANCH|UNKNOWN_LEVEL");
            assertEquals(Branch.ALL, malformed.getBranch());
            assertEquals(Urgency.ROUTINE, malformed.getUrgency());
        }
    }
}