package InputAndProcess;

import model.Message;
import model.Progress;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (Repository) managing the lifecycle of intercepted messages
 * in the central {@code INPUT} PostgreSQL table.
 * <p>
 * Provides methods to poll batches of {@link Progress#PENDING} messages, transition them
 * to {@link Progress#IN_PROGRESS} while being analyzed, and finalize their state as
 * {@link Progress#DONE} or {@link Progress#FAILED}.
 */
public class Repository {
    private final String dbUrl = System.getenv().getOrDefault("ULTRA_DB_URL",
            "jdbc:postgresql://localhost:5432/my_database");
    private final String dbUser = System.getenv().getOrDefault("ULTRA_DB_USER", "ULTRA");
    private final String dbPassword = System.getenv().getOrDefault("ULTRA_DB_PASSWORD", "change_me");

    /**
     * Retrieves up to 5 oldest {@link Progress#PENDING} messages from the {@code INPUT} table
     * (ordered by {@code id ASC}) and immediately updates their status to {@link Progress#IN_PROGRESS}
     * to claim them for processing.
     *
     * @return a list of up to 5 {@link Message} domain objects ready for decryption
     */
    public List<Message> fetchPendingMessages() {
        List<Message> messagesToProcess = new ArrayList<>();
        String selectSql =
                "SELECT id, encrypted_text,cipher_type FROM INPUT WHERE process" +
                        " = 'PENDING' ORDER BY id ASC LIMIT 5;";
        String updateSql = "UPDATE INPUT SET process = ? WHERE id = ?;";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement selectStmt = conn.prepareStatement(selectSql);
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)){

            ResultSet rs = selectStmt.executeQuery();

            while(rs.next()){
                int id = rs.getInt("id");
                String ciphertext = rs.getString("encrypted_text");
                String cipherType = rs.getString("cipher_type");

                // Claim the message by marking it as IN_PROGRESS
                updateStmt.setString(1, Progress.IN_PROGRESS.name());
                updateStmt.setInt(2, id);
                updateStmt.executeUpdate();

                Message msg
                        = new Message(id, ciphertext, "Unknown Frequency",cipherType);

                messagesToProcess.add(msg);

            }
        } catch (Exception e) {
            System.err.println("[Repository] Error fetching pending messages: " + e.getMessage());
        }
        return messagesToProcess;
    }

    /**
     * Updates the processing status of a message in the {@code INPUT} table to {@link Progress#DONE}
     * once decryption, classification, and branch routing have succeeded.
     *
     * @param messageId the primary key ID of the processed message
     */
    public void markAsDone(int messageId){
        String updateSql = "UPDATE INPUT SET process = ? WHERE id = ?;";
        try (Connection conn = DriverManager.getConnection(dbUrl,dbUser,dbPassword);
             PreparedStatement pstmt = conn.prepareStatement(updateSql)){
            pstmt.setString(1, Progress.DONE.name());
            pstmt.setInt(2, messageId);
            pstmt.executeUpdate();
            System.out.println("[Repository] Message ID " + messageId + " marked as DONE.");
        }
        catch (Exception e) {
            System.err.println("[Repository] Error marking message as DONE: " + e.getMessage());
        }
    }

    /**
     * Updates the processing status of a message in the {@code INPUT} table to {@link Progress#FAILED}
     * when an unrecoverable error occurs during decryption or routing.
     *
     * @param messageId the primary key ID of the failed message
     */
    public void markAsFailed(int messageId) {
        String updateSql = "UPDATE INPUT SET process = ? WHERE id = ?;";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {

            pstmt.setString(1, Progress.FAILED.name());
            pstmt.setInt(2, messageId);
            pstmt.executeUpdate();
            System.out.println("[Repository] Message ID " + messageId + " marked as FAILED.");

        } catch (Exception e) {
            System.err.println("[Repository] Failed to update status to FAILED: " + e.getMessage());
        }
    }
}
