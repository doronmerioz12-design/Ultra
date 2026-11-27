package InputAndProcess;

import model.CipherType;
import model.Progress;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Scanner;

/**
 * Simulates a British radio interception station capturing enemy transmissions.
 *
 * <p>Prompts the operator via the command line to enter intercepted ciphertexts and
 * validates the suspected cipher type against {@link CipherType} before inserting
 * each transmission into the PostgreSQL {@code INPUT} table with status
 * {@link Progress#PENDING}.</p>
 */
public class RadioStationSimulator implements InputReciver {

    private final String dbUserName = System.getenv().getOrDefault("ULTRA_DB_USER", "ULTRA");
    private final String dbPassword = System.getenv().getOrDefault("ULTRA_DB_PASSWORD", "change_me");
    private final String dbUrl = System.getenv().getOrDefault("ULTRA_DB_URL",
            "jdbc:postgresql://localhost:5432/my_database");

    /**
     * Runs an interactive console loop to capture encrypted messages and store them
     * in the {@code INPUT} database table until the user enters {@code -1} or a blank line.
     *
     * <p>Invalid cipher types are rejected immediately with a message and re-prompted
     * without writing bad records to the database.</p>
     */
    @Override
    public void receive() {
        Scanner sc = new Scanner(System.in);
        String createTableSql = "CREATE TABLE IF NOT EXISTS INPUT (" +
                "id SERIAL PRIMARY KEY, " +
                "encrypted_text TEXT, " +
                "cipher_type VARCHAR(50), " +
                "process VARCHAR(50), " +
                "received_at TIMESTAMP" +
                ");";

        String insertMessageSql =
                "INSERT INTO INPUT (encrypted_text, cipher_type, process, received_at) " +
                        "VALUES (?, ?, ?, ?);";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword);
             Statement stmt = conn.createStatement()) {

            // Ensure the central INPUT queue table exists before inserting records
            stmt.execute(createTableSql);

            String input = cliInput(sc);

            while (!input.equals("-1") && !input.trim().isEmpty()) {
                String cipherType = promptValidCipherType(sc);

                try (PreparedStatement pstmt = conn.prepareStatement(insertMessageSql)) {
                    pstmt.setString(1, input);
                    pstmt.setString(2, cipherType);
                    pstmt.setString(3, Progress.PENDING.name());
                    pstmt.setObject(4, LocalDateTime.now());

                    pstmt.executeUpdate();
                    System.out.println("[Radio Simulator] Intercept successfully logged into DB!");
                } catch (Exception e) {
                    System.err.println("[Radio Simulator] Failed to insert message: "
                            + e.getMessage());
                }

                input = cliInput(sc);
            }

        } catch (Exception e) {
            System.err.println("[Radio Simulator] Database connection failed: " + e.getMessage());
        }
    }

    /**
     * Prompts the user for the next encrypted message string.
     *
     * @param sc the active console {@link Scanner}
     * @return the raw line entered by the user
     */
    private String cliInput(Scanner sc) {
        System.out.print("Enter the encrypted message (-1 to exit): ");
        return sc.nextLine();
    }

    /**
     * Prompts repeatedly until the operator enters a valid {@link CipherType}.
     *
     * <p>Writes validation feedback to {@code System.out} so the error message appears
     * immediately above the next prompt in console output, and ignores blank lines
     * without printing an extra error.</p>
     *
     * @param sc the active console {@link Scanner}
     * @return the canonical name of the validated {@link CipherType}
     */
    private String promptValidCipherType(Scanner sc) {
        while (true) {
            System.out.print("Enter cipher type (" + CipherType.availableNames() + "): ");
            String rawCipher = sc.nextLine();

            if (rawCipher == null || rawCipher.trim().isEmpty()) {
                continue;
            }

            Optional<CipherType> cipherType = CipherType.fromString(rawCipher);
            if (cipherType.isPresent()) {
                return cipherType.get().name();
            }

            System.out.println("[Radio Simulator] Invalid cipher type '" + rawCipher.trim()
                    + "'. Supported ciphers: " + CipherType.availableNames() + ". Please try again.");
        }
    }
}