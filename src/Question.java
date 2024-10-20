import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Question {
    private static final Scanner s = new Scanner(System.in);
    public static final int MAX_ANSWERS = 4;

    public enum eQuestionType {open, closed}
    public enum eDifficulty  {easy, medium, hard}

    public static void printQuestion(Connection conn, int questionID) throws SQLException {
        String sql = "SELECT * FROM question WHERE QuestionID = " + questionID;
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next())
            System.out.println(rs.getInt("questionID") + ") " + rs.getString("questionText"));
    }

    public static eDifficulty getQuestionDifficulty() {
        int difficulty = -1;
        while (difficulty < 0 || difficulty > 2) {
            difficulty = DatabaseUtils.getValidChoice(new String[] {"easy","medium","hard"});
        }
        if(difficulty == 0) return eDifficulty.easy;
        if(difficulty == 1) return eDifficulty.medium;
        else return eDifficulty.hard;
    }

    public static int getNumOfAnswers(Connection conn) throws SQLException {
        final int minAnswers = 2;
        int numOfAnswers = 0;
        while (numOfAnswers < minAnswers )//|| numOfAnswers > maxAnswers) {
        {
            numOfAnswers = DatabaseUtils.getInt("Enter the number of answers (minimum: " + minAnswers + ")");
            if (numOfAnswers < minAnswers )//|| numOfAnswers > maxAnswers) {
                System.out.println("Invalid input");
        }
        return numOfAnswers;
    }


    public static void addNewQuestionToDB(Connection conn) throws SQLException {
        try {
            conn.setAutoCommit(false);
            if(DatabaseUtils.countTableEntries(conn, "subject", null) == 0){
                System.out.println("There are no subjects in the database. Please ask the admin to add a subject before adding a question.");
                return;
            }
            eQuestionType type = getQuestionType();
            int subjectID = DatabaseUtils.getValidID(conn, "subject", "subjectID", new String[] {"subjectid", "subjectname"}, null);
            String questionText = DatabaseUtils.getString("Enter question text: ");
            eDifficulty difficulty = getQuestionDifficulty();
            int questionID = addQuestionEntry(conn, subjectID, questionText, type, difficulty);
            if (type == eQuestionType.closed) addClosedQuestion(conn, questionID);
            if (type == eQuestionType.open) addOpenQuestion(conn, questionID);
            conn.commit();
            System.out.println("Question added to the database successfully!");
        } catch(SQLException e) {
            conn.rollback();
            throw e;
        }
        finally{
            conn.setAutoCommit(true);
        }
    }

    private static int addQuestionEntry(Connection conn, int subjectID, String questionText, eQuestionType type, eDifficulty difficulty) throws SQLException {
        String sql = "INSERT INTO question (subjectid, questiontext, questiontype, questiondifficulty) VALUES (?,?,?,?) RETURNING questionID";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, subjectID);
        stmt.setString(2, questionText);
        stmt.setString(3, type.name());
        stmt.setString(4, difficulty.name());
        ResultSet rs = stmt.executeQuery();
        int questionID = 0;
        if (rs.next()) questionID = rs.getInt("questionID");
        rs.close();
        stmt.close();
        return questionID;
    }

    private static void addClosedQuestion(Connection conn, int questionID) throws SQLException {
        int numOfAnswers = getNumOfAnswers(conn), i = 0;
        List<Integer> answerIDs = new ArrayList<>();
        while (i < numOfAnswers) {
            int choice = DatabaseUtils.getInt("Press 0 to add a new Answer.\nPress 1 to add an existing answer from the database.\n");
            switch (choice) {
                case 0:
                    createNewAnswer(conn, questionID, answerIDs);
                    i++;
                    break;
                case 1:
                    int totalAnswers = DatabaseUtils.countTableEntries(conn,"answer", null);
                    if(totalAnswers == 0 || answerIDs.size() == totalAnswers) {
                        System.out.println("There are no available answers in the database, please add a new answer.");
                        break;
                    }
                    addExistingAnswer(conn, questionID, answerIDs);
                    i++;
                    break;
                default:
                    System.out.println("Invalid input, please try again:\n");
                    break;
            }
        }
    }


    public static void updateOpenQuestion(Connection conn) throws SQLException {
        try{
        conn.setAutoCommit(false);
        if(DatabaseUtils.countTableEntries(conn, "question", null) == 0){
            System.out.println("There are no questions in the database.");
            return;
        }
        int questionID = DatabaseUtils.getValidID(conn, "question", "questionID",
                new String[]{"questionID", "questionText"}, null);
        String newText = DatabaseUtils.getString("Enter new question text: ");
        eDifficulty newDifficulty = getQuestionDifficulty();
        int newSubjectID = DatabaseUtils.getValidID(conn, "subject", "subjectID", new String[] {"subjectid", "subjectname"}, null);
        String sql = "UPDATE Question SET questiontext = ?, questiondifficulty = ?, subjectID = ? WHERE questionID = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newText);
            pstmt.setString(2, newDifficulty.name());
            pstmt.setInt(3, newSubjectID);
            pstmt.setInt(4, questionID);
            pstmt.executeUpdate();
            conn.commit();
            System.out.println("Question was updated successfully.");
        }  catch (SQLException e) {
            conn.rollback();
            throw e;
        }
        finally {
            conn.setAutoCommit(true);
        }
    }


    private static void createNewAnswer(Connection conn,int questionID, List<Integer> answerIDs) throws SQLException {
        boolean isCorrect;
        String answer = DatabaseUtils.getString("Enter answer text:");
        int answerID = Answer.addAnswerToDB(conn, answer);
        answerIDs.add(answerID);
        isCorrect = isCorrectAnswer();
        addQuestionAnswerEntry(conn, questionID, answerID, isCorrect);
    }


    public static void addExistingAnswer(Connection conn, int questionID, List<Integer> answerIDs) throws SQLException {
        int curAnswerID = DatabaseUtils.getValidID(conn, "answer", "answerID",new String[] {"answerID", "answerText"},null);
        boolean isCorrect;
        answerIDs.add(curAnswerID);
        isCorrect = isCorrectAnswer();
        addQuestionAnswerEntry(conn, questionID, curAnswerID, isCorrect);
    }

    private static void addQuestionAnswerEntry(Connection conn, int questionID, int answerID, boolean isCorrect) throws SQLException {
        String sql = "INSERT INTO questionanswer (questionid, answerid, iscorrect) VALUES (?,?,?) RETURNING answerID";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, questionID);
        stmt.setInt(2, answerID);
        stmt.setBoolean(3, isCorrect);
        ResultSet rs = stmt.executeQuery();
    }

    private static boolean isCorrectAnswer() {
        int choice = DatabaseUtils.getInt("Is this a correct Answer? 0 - Yes, 1 - No");
        while (choice != 0 && choice != 1) {
            System.out.println("Invalid input, please try again:");
            choice = s.nextInt();
            s.nextLine(); // flush
        }
        return choice == 0;
    }


    private static void addOpenQuestion(Connection conn, int questionID) throws SQLException {
        int choice = DatabaseUtils.getValidChoice(new String[] {"New Answer", "Existing Answer"});
        switch (choice) {
            case 0:
                createNewAnswer(conn, questionID);
                break;
            case 1:
                chooseExistingAnswer(conn, questionID);
                break;
        }
    }
    private static void createNewAnswer(Connection conn, int questionID) throws SQLException {
        System.out.println("Enter answer text:");
        String answer = s.nextLine();
        int answerID = Answer.addAnswerToDB(conn, answer);
        addQuestionAnswerEntry(conn, questionID, answerID, true);
    }

    private static void chooseExistingAnswer(Connection conn, int questionID) throws SQLException {
        int answerID;
        DatabaseUtils.printTable(conn, "answer", new String[]{"answerID", "answerText"}, null);
        boolean isValid;
        do {
            System.out.println("Choose the answer you want to add to current question:\n");
            answerID = s.nextInt();
            s.nextLine(); // flush
            // makes sure answer exists in table and not linked to question
            isValid = DatabaseUtils.checkIntExistsInCol(conn, "answer", "answerid", answerID, null);
            if (!isValid) System.out.println("Invalid input, please try again:\n");
        } while (!isValid);
        addQuestionAnswerEntry(conn, questionID, answerID, true);
    }

    public static eQuestionType getQuestionType() {
        int type = DatabaseUtils.getValidChoice(new String[] {"Open", "Closed"});

        if (type == 0) {
            return eQuestionType.open;
        } else {
            return eQuestionType.closed;
        }
    }

    public static void addAnswerToExistingClosedQuestion(Connection conn) throws SQLException {
        if(DatabaseUtils.countTableEntries(conn, "question", null) == 0) {
            System.out.println("There are no questions in the database. Please add a question before choosing this option.");
            return;
        }
        try {
            conn.setAutoCommit(false);
            int questionID = DatabaseUtils.getValidID(conn, "question", "questionID",
                    new String[]{"questionID", "questionText"}, "questiontype = 'closed'");
            int choice;
            if (DatabaseUtils.countTableEntries(conn, "answer", null) == 0) choice = 0; // if there are no answers, a new answer should be added
            else choice = DatabaseUtils.getValidChoice(new String [] {"Add a new answer", "Add an existing answer"});
            int answerID = -1;
            if(choice == 0){ //new Answer
                String answerText = DatabaseUtils.getString("Enter answer text:");
                answerID = Answer.addAnswerToDB(conn, answerText);
            }
            else{ //existing answer
                answerID = Answer.getValidAnswerID(conn, questionID);
            }
            boolean isCorrect = isCorrectAnswer();
            addQuestionAnswerEntry(conn, questionID, answerID, isCorrect);
            conn.commit();
            System.out.println("Answer added to question successfully!");
        } catch(SQLException e){
            conn.rollback();
            throw e;
        }
        finally{
            conn.setAutoCommit(true);
        }
    }

    public static void deleteQuestionFromDB(Connection conn) throws SQLException {
        try {
            conn.setAutoCommit(false);
            if(DatabaseUtils.countTableEntries(conn, "question", null) == 0) {
                System.out.println("There are no questions in the database.");
                return;
            }
            int questionID = DatabaseUtils.getValidID(conn, "question", "questionID", new String[]{"questionID", "questionText"}, null);
            String sql = "DELETE FROM question WHERE questionID = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, questionID);
            pstmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}
