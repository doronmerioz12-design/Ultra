package branches;

import model.Urgency;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;

public abstract class AbstractBranchObserver implements Observer {

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final String tableName;
    private final String branchLabel;

    protected AbstractBranchObserver(String tableName, String branchLabel,
                                     String dbUser, String dbPassword) {
        this.dbUrl = System.getenv().getOrDefault("ULTRA_DB_URL",
                "jdbc:postgresql://localhost:5432/my_database");
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.tableName = tableName;
        this.branchLabel = branchLabel;
    }

    @Override
    public void update(String decryptedText, Urgency urgencyLevel) {
        if (decryptedText == null) return;

        String createTableSql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id SERIAL PRIMARY KEY, " +
                "decrypted_text TEXT, " +
                "urgency_level VARCHAR(50), " +
                "intercepted_at TIMESTAMP" +
                ");";

        String insertMessageSql = "INSERT INTO " + tableName +
                " (decrypted_text, urgency_level, intercepted_at) " +
                "VALUES (?, ?, ?);";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement();
             PreparedStatement pstmt = conn.prepareStatement(insertMessageSql)) {

            stmt.execute(createTableSql);

            pstmt.setString(1, decryptedText);
            pstmt.setString(2, urgencyLevel.name());
            pstmt.setObject(3, LocalDateTime.now());

            pstmt.executeUpdate();

            System.out.println("[" + branchLabel + "] Successfully secured message in database.");

        } catch (Exception e) {
            System.err.println("[" + branchLabel + "] Database operation failed: "
                    + e.getMessage());
        }
    }
}
