import javax.xml.crypto.Data;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.sql.*;

public class Exam {
    private static final int MAX_QUESTIONS = 10;
    private static final int MIN_QUESTIONS = 1;

    private static int addExamEntry(Connection conn, String title, int userID, int subjectID) throws SQLException {
        String sql = "INSERT INTO exam (title, userID, subjectID) VALUES (?, ?, ?) RETURNING examid";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, title);
        pstmt.setInt(2, userID);
        pstmt.setInt(3, subjectID);
        ResultSet rs = pstmt.executeQuery();
        rs.next();
        int examID = rs.getInt(1);
        pstmt.close();
        return examID;
    }


    public static void createExam(Connection conn, int userID) throws SQLException {
        try {
            conn.setAutoCommit(false);
            if(DatabaseUtils.countTableEntries(conn, "subject", null) == 0) {
                System.out.println("There are no subjects in the database. A subject should be added before creating an exam.");
                return;
            }
            final int AUTO = 0;
            int subjectID = DatabaseUtils.getValidID(conn, "subject", "subjectID", new String[]{"subjectID", "subjectName"}, null);
            int questionsInDB = DatabaseUtils.countTableEntries(conn, "question", "subjectID = " + subjectID);
            if (questionsInDB == 0) {
                System.out.println("There are no questions in the database about this subject.");
                return;
            }
            String title = DatabaseUtils.getString("Enter the title of the exam: ");
            int examID = addExamEntry(conn, title, userID, subjectID);
            int choice = DatabaseUtils.getValidChoice(new String[]{"Auto", "Manual"});
            if (choice == AUTO) createAutoExam(conn, subjectID, questionsInDB, examID);
            else createManualExam(conn, subjectID, examID);
            conn.commit();
        } catch (SQLException e){
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
    private static void createAutoExam(Connection conn, int subjectID, int questionsInDB, int examID) throws SQLException {
        int maxQuestions = Math.min(MAX_QUESTIONS, questionsInDB);
        Random random = new Random();
        int questionAmount = random.nextInt(maxQuestions - MIN_QUESTIONS + 1) + MIN_QUESTIONS;
        List<Integer> questionIDs = DatabaseUtils.getAllQuestionIDs(conn, "subjectID = " + subjectID);
        Set <Integer> selectedQuestions = new HashSet<>();
        while(selectedQuestions.size() < questionAmount)
            selectedQuestions.add(questionIDs.get(random.nextInt(questionIDs.size())));
        for(int questionID : selectedQuestions){
            addExamQuestionEntry(conn, examID, questionID);
        }
    }

    private static void addExamQuestionEntry(Connection conn, int examID, int questionID) throws SQLException {
        String sql = "INSERT INTO examquestion (examID, questionID) VALUES (?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, examID);
        pstmt.setInt(2, questionID);
        pstmt.executeUpdate();
        pstmt.close();
    }

    private static void createManualExam(Connection conn, int subjectID, int examID) throws SQLException {
        ArrayList<Integer> examQuestionsIDs = createExamQuestionsList(conn, examID, subjectID);
        for (Integer examQuestionsID : examQuestionsIDs) {
            addExamQuestionEntry(conn, examID, examQuestionsID);
        }

    }
    private static ArrayList <Integer> createExamQuestionsList(Connection conn,int examID, int subjectID) throws SQLException {
        int questionID;
        boolean isQuestionIDValid;
        boolean isQuestionIDUnique;
        int numOfQuestions = getQuestionsCountOnExam(conn, examID,subjectID);
        String condition = "subjectID = " + subjectID;
        ArrayList <Integer>  examQuestionsIDs = new ArrayList<>();
        List <Integer> subjectQuestions = DatabaseUtils.getAllQuestionIDs(conn, condition);

        for(int i=0; i<numOfQuestions; i++) {
            DatabaseUtils.showAllQuestionsAndAnswers(conn, condition);
            do {
                questionID = DatabaseUtils.getInt("Enter the question ID for question " + (i + 1));
                isQuestionIDValid = subjectQuestions.contains(questionID);
                isQuestionIDUnique = !examQuestionsIDs.contains(questionID);
                if(!isQuestionIDUnique) System.out.println("Question is already in exam");
                if(!isQuestionIDValid) System.out.println("Invalid question ID");
            } while (!isQuestionIDValid || !isQuestionIDUnique);
            examQuestionsIDs.add(questionID);

        }
        conn.setAutoCommit(false);
        return examQuestionsIDs;
    }
    private static int getQuestionsCountOnExam(Connection conn, int examID, int subjectID) throws SQLException{
        String condition = "subjectID = " + subjectID;
        int totalQuestions = DatabaseUtils.countTableEntries(conn, "question", condition);
        int maxQuestions = Math.min(MAX_QUESTIONS, totalQuestions);
        int numOfQuestions;
        do {
            System.out.println("Enter a number between " + MIN_QUESTIONS + " and " + maxQuestions);
            numOfQuestions = DatabaseUtils.getInt("How many questions do you want on the test?");
        } while (numOfQuestions > maxQuestions || numOfQuestions < MIN_QUESTIONS);
        return  numOfQuestions;
    }

//    public static void generateExamFiles(Connection conn, int userID) throws SQLException, IOException {
//        try {
//        if(DatabaseUtils.countTableEntries(conn, "exam", "userID = " + userID) == 0){
//            System.out.println("Please create an exam before using this option");
//            return;
//        }
//        conn.setAutoCommit(false);
//        int examID = DatabaseUtils.getValidID(conn, "exam", "examID", new String[]{"examID", "title","createdat"}, "userID = " + userID);
//        generateExamForm(conn, examID);
//        generateExamSolution(conn, examID);
//        } catch (SQLException e){
//            conn.rollback();
//            throw e;
//        }
//        finally {
//            conn.setAutoCommit(true);
//        }
//    }

    private static String sanitizeFileName(String title) {
        // Remove or replace characters that are not valid in filenames
        return title.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    public static void generateExamFiles(Connection conn, int userID) throws SQLException, IOException {
        if(DatabaseUtils.countTableEntries(conn, "exam", "userID = " + userID) == 0){
            System.out.println("Please create an exam before using this option");
            return;
        }
        int examID = DatabaseUtils.getValidID(conn, "exam", "examID", new String[]{"examID", "title"}, "userID = " + userID);
        String sql = "SELECT Title FROM Exam WHERE ExamID = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, examID);
        ResultSet rs = pstmt.executeQuery();
        if (!rs.next()) return;
        String title = rs.getString("Title");
        rs.close();
        pstmt.close();

        generateExamForm(conn, examID, title);
        generateExamSolution(conn, examID, title);
    }

    private static void generateExamForm(Connection conn, int examID, String title) throws SQLException, IOException {
        String formFilename = title + "_form.txt";
        try (FileWriter writer = new FileWriter(formFilename)) {
            writer.write(title + "\n\n");

            String sql = "SELECT q.QuestionID, q.QuestionText, q.QuestionType FROM ExamQuestion eq " +
                    "JOIN Question q ON eq.QuestionID = q.QuestionID WHERE eq.ExamID = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, examID);
            ResultSet rs = pstmt.executeQuery();

            int questionNumber = 1;
            while (rs.next()) {
                String questionText = rs.getString("QuestionText");
                String questionType = rs.getString("QuestionType");
                int questionID = rs.getInt("QuestionID");

                writer.write(questionNumber + ". " + questionText + "\n");

                if (questionType.equals("closed")) {
                    writeClosedQuestionAnswers(conn, writer, questionID);
                } else if (questionType.equals("open")) {
                    writer.write("____________________________\n");
                }
                writer.write("\n");
                questionNumber++;
            }
            rs.close();
            pstmt.close();
        }
    }

    private static void writeClosedQuestionAnswers(Connection conn, FileWriter writer, int questionID) throws SQLException, IOException {
        String sql = "SELECT a.AnswerText FROM QuestionAnswer qa " +
                "JOIN Answer a ON qa.AnswerID = a.AnswerID WHERE qa.QuestionID = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, questionID);
        ResultSet rs = pstmt.executeQuery();

        char answerOption = 'a';
        int correctAnswerCount = 0;
        while (rs.next()) {
            String answerText = rs.getString("AnswerText");
            writer.write("   " + answerOption + ". " + answerText + "\n");
            answerOption++;
        }
        rs.close();
        pstmt.close();
        writer.write("   " + answerOption + ". No correct answers\n");
        answerOption++;
        writer.write("   " + answerOption + ". More than one correct answer\n");

    }

    private static void generateExamSolution(Connection conn, int examID, String title) throws SQLException, IOException {
        String solutionFilename = title + "_solution.txt";
        try (FileWriter writer = new FileWriter(solutionFilename)) {
            writer.write("Exam Title: " + title + "\n\n");

            String sql = "SELECT q.QuestionID, q.QuestionText, q.QuestionType FROM ExamQuestion eq " +
                    "JOIN Question q ON eq.QuestionID = q.QuestionID WHERE eq.ExamID = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, examID);
            ResultSet rs = pstmt.executeQuery();

            int questionNumber = 1;
            while (rs.next()) {
                String questionText = rs.getString("QuestionText");
                String questionType = rs.getString("QuestionType");
                int questionID = rs.getInt("QuestionID");

                writer.write(questionNumber + ". " + questionText + "\n");

                if (questionType.equals("closed")) {
                    writeCorrectClosedAnswers(conn, writer, questionID);
                } else if (questionType.equals("open")) {
                    writer.write("Correct answer: " + getOpenQuestionAnswer(conn, questionID) + "\n");
                }
                writer.write("\n");
                questionNumber++;
            }
            rs.close();
            pstmt.close();
        }
    }

    private static void writeCorrectClosedAnswers(Connection conn, FileWriter writer, int questionID) throws SQLException, IOException {
        int res = checkMoreOrNoCorrectAnswers(conn, questionID);
        if(res == 0) {
            writer.write("There's no correct answer\n");
        }
        else if(res > 1){
            writer.write("More than one correct answer\n");
        }
        else{ // one correct answer
            String sql = "SELECT a.AnswerText FROM QuestionAnswer qa " +
                    "JOIN Answer a ON qa.AnswerID = a.AnswerID WHERE qa.QuestionID = ? AND qa.IsCorrect = TRUE";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, questionID);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String answerText = rs.getString("AnswerText");
                writer.write("   Correct answer: " + answerText + "\n");
            }
            rs.close();
            pstmt.close();
        }
    }

    private static int checkMoreOrNoCorrectAnswers(Connection conn, int questionID) throws SQLException {
        int correctAnswerCount = -1;
        String sql = "SELECT COUNT(*) FROM QuestionAnswer WHERE QuestionID = ? AND IsCorrect = TRUE";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, questionID);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            correctAnswerCount = rs.getInt(1);
        }
        rs.close();
        pstmt.close();
        return correctAnswerCount;
    }

    private static String getOpenQuestionAnswer(Connection conn, int questionID) throws SQLException {
        String sql = "SELECT a.AnswerText FROM QuestionAnswer qa JOIN Answer a ON qa.AnswerID = a.AnswerID WHERE qa.QuestionID = ? AND qa.IsCorrect = TRUE";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, questionID);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            return rs.getString("AnswerText");
        }
        rs.close();
        pstmt.close();
        return "There's No correct answer";
    }
}

