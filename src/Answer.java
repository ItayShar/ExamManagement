import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Answer {

    public static int addAnswerToDB(Connection conn, String answer) throws SQLException {
        String sql = "INSERT INTO answer (answerText) VALUES (?) RETURNING answerID";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, answer);
        ResultSet rs = stmt.executeQuery();
        int answerID = 0;
        if (rs.next()) {
            answerID = rs.getInt("answerID");
        }
        rs.close();
        stmt.close();
        return answerID;
    }


    public static void deleteAnswerFromDB(Connection conn, int answerID) throws SQLException {
        String sql = "DELETE FROM answer WHERE answerID = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, answerID);
        stmt.executeUpdate();
        stmt.close();
    }

    public static void unlinkAnswerFromQuestion(Connection conn) throws SQLException {
        try {
            conn.setAutoCommit(false);
            if(DatabaseUtils.countTableEntries(conn, "question", "questionType = 'closed'") == 0) {
                System.out.println("There are no closed questions in the database.");
                return;
            }
            int questionID = DatabaseUtils.getValidID(conn, "question", "questionID", new String[]{"questionID", "questionText"}, "questionType = 'closed'");
            String condition = "questionID = " + questionID;
            int answerID = DatabaseUtils.getValidID(conn, "questionanswer qa JOIN answer a ON qa.answerID = a.answerID",
                    "qa.answerID", new String[]{"qa.answerID", "answerText"}, condition);
            String sql = "DELETE FROM questionanswer WHERE questionID = ? AND answerID = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, questionID);
            pstmt.setInt(2, answerID);
            pstmt.executeUpdate();
            conn.commit();
        } catch (SQLException e){
            conn.rollback();
            throw new SQLException("Failed to unlink answer from question");
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public static int getValidAnswerID(Connection conn, int questionID) throws SQLException {
        List <Integer> currAnswers = Answer.getExistingAnswers(conn, questionID);
        int answerID;
        boolean existsInCurrAnswers;
        do{
            answerID = DatabaseUtils.getValidID(conn, "answer", "answerID", new String[]{"answerID", "answerText"}, null);
            existsInCurrAnswers = currAnswers.contains(answerID);
            if(existsInCurrAnswers) System.out.println("Answer is already linked to this question.");
        } while(existsInCurrAnswers);
        return answerID;
    }

    public static List<Integer> getExistingAnswers(Connection conn, int questionID) throws SQLException {
        String sql = "SELECT answerID FROM questionanswer WHERE questionID = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, questionID);
        ResultSet rs = stmt.executeQuery();
        List <Integer> answerIDs = new ArrayList<Integer>();
        while (rs.next()) {
            answerIDs.add(rs.getInt("answerID"));
        }
        rs.close();
        stmt.close();
        return answerIDs;
    }

    public static void deleteAnswer(Connection conn) throws SQLException {
        if(DatabaseUtils.countTableEntries(conn, "answer", null) == 0) {
            System.out.println("There are no answers in the database.");
            return;
        }
        int answerID = DatabaseUtils.getValidID(conn, "answer", "answerID", new String[]{"answerID", "answerText"}, null);
        Answer.deleteAnswerFromDB(conn, answerID);
    }
}
