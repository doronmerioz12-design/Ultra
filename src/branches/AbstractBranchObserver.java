package branches;

import model.Urgency;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;

/**
 * Abstract base class for all military branch observers, centralizing JDBC persistence logic.
 * <p>
 * Each concrete branch subclass configures its dedicated PostgreSQL table name, display label,
 * and branch-specific RBAC database credentials. When notified via {@link #update(String, Urgency)},
 * this class ensures the branch table exists and inserts the decrypted message, urgency level,
 * and interception timestamp.
 */
public abstract class AbstractBranchObserver implements Observer {

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final String tableName;
    private final String branchLabel;

    /**
     * Initializes a branch observer with its dedicated database table, console log label,
     * and database role credentials.
     *
     * @param tableName   the name of the branch-specific PostgreSQL table (e.g., {@code "NavyMes"})
     * @param branchLabel human-readable branch name for console logging (e.g., {@code "Navy HQ"})
     * @param dbUser      the PostgreSQL RBAC role username for this branch
     * @param dbPassword  the PostgreSQL password for this branch role
     */
    protected AbstractBranchObserver(String tableName, String branchLabel,
                                     String dbUser, String dbPassword) {
        this.dbUrl = System.getenv().getOrDefault("ULTRA_DB_URL",
                "jdbc:postgresql://localhost:5432/my_database");
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.tableName = tableName;
        this.branchLabel = branchLabel;
    }

    /**
     * Persists a routed intelligence message into this branch's dedicated database table.
     * Automatically creates the table if it does not already exist.
     *
     * @param decryptedText the recovered plaintext message (ignored if {@code null})
     * @param urgencyLevel  the urgency classification assigned to the message
     */
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
