import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;

public class DatabaseUtils {
    private static final Scanner s = new Scanner(System.in);

    public static Connection getConnection() throws SQLException {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
            String dbUrl = properties.getProperty("db.url");
            String dbUser = properties.getProperty("db.user");
            String dbPassword = properties.getProperty("db.password");
            // Connect to the database
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Closes the connection to the database.
     *
     * @param conn	a Connection object representing the database connection
     * @throws SQLException	if a database access error occurs
     */
    public static void closeConnection(Connection conn) throws SQLException {
        conn.close();
    }

    public static List<Integer> getAllQuestionIDs(Connection conn, String condition) throws SQLException {
        List<Integer> questionIDs = new ArrayList<>();
        String sql = "SELECT QuestionID FROM Question";
        if(condition != null) sql += " WHERE " + condition;
        PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next())
            questionIDs.add(rs.getInt("QuestionID"));

        return questionIDs;
}

     //Method to show all questions and their corresponding answers
    public static void showAllQuestionsAndAnswers(Connection conn, String condition) throws SQLException {
            conn.setAutoCommit(false);
            List<Integer> questionIDs = getAllQuestionIDs(conn, condition);
            questionIDs.sort(Integer::compareTo);
            for (int questionID : questionIDs) {
                Question.printQuestion(conn, questionID);
                printQuestionAnswers(conn, questionID);
                System.out.println();
            }
            conn.commit();
            conn.setAutoCommit(true);
    }

    public static String getStrFromList(String [] list){
        int choice = getValidChoice(list);
        return list[choice];
    }

    public static void printQuestionAnswers(Connection conn, int questionID) throws SQLException {
        String sql = "SELECT answerText, iscorrect FROM questionanswer" +
                " JOIN answer ON questionanswer.AnswerID = answer.AnswerID" +
                " WHERE QuestionID = " + questionID;
        PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery();
        while(rs.next()){
            System.out.println("\t" + rs.getString("answerText") + "\t(" + rs.getBoolean("iscorrect") +")");
        }
    }


    public static boolean checkIntExistsInCol(Connection conn, String table, String column, int val, String condition) throws SQLException {
        String sql = "SELECT EXISTS (SELECT 1 FROM " + table + " WHERE " + column + " = ?";
        if (condition != null) sql += " AND " + condition;
        sql += ")";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, val);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            return rs.getBoolean(1);
        }
        return false;
    }

    public static int getValidID(Connection conn, String table, String Column, String [] colsToPrint, String condition) throws SQLException {
        printTable(conn, table, colsToPrint, condition);
        int id = getInt("Choose ID:");

        while(!checkIntExistsInCol(conn, table, Column, id, condition)){
            System.out.println("Invalid ID, please try again:");
            id = getInt(null);
        }
        return id;
    }



    public static void printTable(Connection conn, String table, String[] columns, String condition) throws SQLException {
        String sql = "SELECT " + String.join(", ", columns) + " FROM " + table;
        if (condition != null) sql += " WHERE " + condition;
        PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            for (int i = 0; i < columns.length; i++) {
                System.out.print(rs.getString(i + 1));
                if (i < columns.length - 1) {
                    System.out.print("\t");
                }
            }
            System.out.println();
        }
    }


    public static int countTableEntries(Connection conn, String table, String condition) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + table;
        if (condition != null) {
            sql += " WHERE " + condition;
        }
        PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery();

        // Return the count if available
        if (rs.next()) {
            return rs.getInt(1);
        }
        return 0; // Default return if ResultSet is empty
    }

    public static String getString(String msg){
        System.out.println(msg);
        return s.nextLine();
    }

    public static int getInt(String msg){
        int num;
        if(msg != null) System.out.println(msg);
        while(true){
            try {
                num = s.nextInt();
                s.nextLine(); //flush
                break;
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a valid integer.");
                s.nextLine();
            }
        }
        return num;
    }

    public static int getValidChoice(String[] options) {
        int choice;
            for (int i = 0; i < options.length; i++) {
                System.out.println((i + 1) + ". " + options[i]);
            }
            choice = getInt("Enter your choice (1 - " + (options.length) + "): ");
            while(choice <= 0 || choice > options.length) {
                System.out.println("Invalid choice. Please enter a number between 1 and " + (options.length) + ".");
                choice = getInt(null);
            }
        return choice - 1;
        }

        public static int getIntInRange(String msg, int min, int max){
            int num;
            if(msg != null) System.out.println(msg);
            do{
                num = getInt(null);
                if(num < min || num > max) System.out.println("Invalid input. Please enter a number between " + min + " to " + max);
            } while(num < min || num > max);
            return num;
        }

}