package InputAndProcess;

import model.Progress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Scanner;

public class RadioStationSimulator implements InputReciver {

    private final String dbUserName = System.getenv().getOrDefault("ULTRA_DB_USER", "ULTRA");
    private final String dbPassword = System.getenv().getOrDefault("ULTRA_DB_PASSWORD", "change_me");
    private final String dbUrl = System.getenv().getOrDefault("ULTRA_DB_URL",
            "jdbc:postgresql://localhost:5432/my_database");

    @Override
    public void receive() {
        Scanner sc = new Scanner(System.in);
        String createTableSql = "CREATE TABLE IF NOT EXISTS INPUT (" +
                "id SERIAL PRIMARY KEY, " +
                "encrypted_text TEXT, " +
                "cipher_type VARCHAR(50), "+
                "process VARCHAR(50), " +
                "received_at TIMESTAMP" +
                ");";

        String insertMessageSql =
                "INSERT INTO INPUT (encrypted_text, cipher_type, process, received_at) " +
                        "VALUES (?, ?, ?, ?);";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword);
             Statement stmt = conn.createStatement()) {

            stmt.execute(createTableSql);

            String input = cliInput(sc);

            while (!input.equals("-1") && !input.trim().isEmpty()) {

                System.out.print("Enter cipher type (CAESAR / VIGENERE): ");
                String cipherType = sc.nextLine().trim().toUpperCase();

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

    private String cliInput(Scanner sc) {
        System.out.print("Enter the encrypted message (-1 to exit): ");
        return sc.nextLine();
    }
}