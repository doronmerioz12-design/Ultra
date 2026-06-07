package InputAndProcess;

import model.Message;
import model.Progress;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class Repository {
    private final String dbUrl = "jdbc:postgresql://localhost:5432/my_database";
    private final String dbUser = "ULTRA";
    private final String dbPassword = "ULTRA123";

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


