import javax.xml.crypto.Data;
import java.sql.*;

public class Subject {
    public static void addSubjectToDB(Connection conn) throws SQLException {
        String subjectName = DatabaseUtils.getString("Enter the new subject's name: ");
        String sql = "INSERT INTO subject (subjectName) VALUES (?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, subjectName);
        pstmt.executeUpdate();
        pstmt.close();
    }

    public static void deleteSubjectFromDB(Connection conn) throws SQLException {
        if(DatabaseUtils.countTableEntries(conn, "subject", null) == 0) {
            System.out.println("There are no subjects in the database.");
            return;
        }
        try {
            conn.setAutoCommit(false);
            int subjectID = DatabaseUtils.getValidID(conn, "subject", "subjectID", new String[]{"subjectID", "subjectName"}, null);
            String sql = "DELETE FROM subject WHERE subjectID = " + subjectID;
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.executeUpdate();
            pstmt.close();
            conn.commit();
        } catch (SQLException e){
            conn.rollback();
            throw e;
        }
        finally {
            conn.setAutoCommit(true);
        }
    }
}
