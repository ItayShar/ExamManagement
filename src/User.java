import java.io.IOException;
import java.sql.*;

public class User {
    private final int userID;
    private final String userRole;

    public User(int userID, String userRole) {
        this.userID = userID;
        this.userRole = userRole;
    }

    private enum teacherMenu {
        showQuestionsAndAnswers,
        addQuestion,
        addAnswer,
        addAnswerToExistingQuestion,
        updateQuestion,
        unlinkAnswerFromAQuestion,
        deleteQuestion,
        deleteAnswer,
        createExam,
        generateExamFiles,
        gradeExam,
        exit
    }

    static String[] teacherMenuOptions = {
            "Show questions and answers",
            "Add a new question to the database",
            "Add a new answer to the database",
            "Add an answer to an existing question in the database",
            "Update an existing question",
            "Unlink an answer from a question in the database (without deleting it)",
            "Delete a question from the database",
            "Delete an answer from the database",
            "Create an exam",
            "Generate exam files",
            "Grade an exam",
            "Exit"
    };

    private enum studentMenu {
        showGrades,
        checkGPA,
        exit
    }

    static String[] studentMenuOptions = {
            "Show my grades",
            "Check my GPA",
            "Exit"
    };

    private enum adminMenu {
        showAllUsers,
        showAllSubjects,
        addSubject,
        deleteSubject,
        addUser,
        deleteUser,
        exit
    }

    static String[] adminMenuOptions = {
            "Show all users",
            "Show all subjects",
            "Add a new subject",
            "Delete a subject",
            "Add a new user",
            "Delete a user",
            "Exit"
    };


    public static User login(Connection conn) throws SQLException {
        String username = DatabaseUtils.getString("Enter your username: ");
        String password = DatabaseUtils.getString("Enter your password: ");
        String sql = "SELECT UserID, UserRole FROM Users WHERE UserName = ? AND Password = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, username);
        stmt.setString(2, password);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            int userID = rs.getInt("UserID");
            String userRole = rs.getString("UserRole");
            System.out.println("Login successful!");
            return new User(userID, userRole);
        } else {
            System.out.println("Invalid username or password.");
            return null;
        }
    }

    public String getUserRole() {
        return userRole;
    }

    public void teacherMenu(Connection conn) {
        int choice = -1;
        while (choice != teacherMenu.exit.ordinal()) {
            try {
                choice = DatabaseUtils.getValidChoice(teacherMenuOptions);
                teacherMenu selectedOption = teacherMenu.values()[choice];
                switch (selectedOption) {
                    case showQuestionsAndAnswers:
                        DatabaseUtils.showAllQuestionsAndAnswers(conn, null);
                        break;
                    case addQuestion:
                        Question.addNewQuestionToDB(conn);
                        break;
                    case addAnswer:
                        String answerText = DatabaseUtils.getString("Enter answer text: ");
                        Answer.addAnswerToDB(conn, answerText);
                        break;
                    case updateQuestion:
                        Question.updateOpenQuestion(conn);
                        break;
                    case addAnswerToExistingQuestion:
                        Question.addAnswerToExistingClosedQuestion(conn);
                        break;
                    case unlinkAnswerFromAQuestion:
                        Answer.unlinkAnswerFromQuestion(conn);
                        break;
                    case deleteQuestion:
                        Question.deleteQuestionFromDB(conn);
                        break;
                    case deleteAnswer:
                        Answer.deleteAnswer(conn);
//                        int answerID = DatabaseUtils.getValidID(conn, "answer", "answerID", new String[]{"answerID", "answerText"}, null);
//                        Answer.deleteAnswerFromDB(conn, answerID);
                        break;
                    case createExam:
                        Exam.createExam(conn, this.userID);
                        break;
                    case generateExamFiles:
                        Exam.generateExamFiles(conn, this.userID);
                        break;
                    case gradeExam:
                        gradeExam(conn, this.userID);
                        break;
                    case exit:
                        System.out.println("Exiting...");
                        break;
                    default:
                        System.out.println("Invalid option.");
                        break;
                }
            } catch (SQLException | IOException e) {
                System.out.println(e.getMessage() + "\n");
            }
        }

    }

    public void studentMenu(Connection conn) {
        int choice = -1;
        while (choice != studentMenu.exit.ordinal()) {
            try {
                choice = DatabaseUtils.getValidChoice(studentMenuOptions);
                studentMenu selectedOption = studentMenu.values()[choice];
                switch (selectedOption) {
                    case showGrades:
                        showStudentGrades(conn, this.userID);
                        break;
                    case checkGPA:
                        calcGPA(conn, this.userID);
                        break;
                    case exit:
                        System.out.println("Exiting...");
                        break;
                    default:
                        System.out.println("Invalid option.");
                        break;
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public void adminMenu(Connection conn) {
        int choice = -1;
        while (choice != adminMenu.exit.ordinal()) {
            try {
                choice = DatabaseUtils.getValidChoice(adminMenuOptions);
                adminMenu selectedOption = adminMenu.values()[choice];
                switch (selectedOption) {
                    case showAllUsers:
                        DatabaseUtils.printTable(conn, "users", new String[]{"username", "userRole"}, null);
                        System.out.println();
                        break;
                    case showAllSubjects:
                        DatabaseUtils.printTable(conn, "subject", new String[]{"subjectName"}, null);
                        System.out.println();
                        break;
                    case addSubject:
                        Subject.addSubjectToDB(conn);
                        break;
                    case deleteSubject:
                        Subject.deleteSubjectFromDB(conn);
                        break;
                    case addUser:
                        addNewUser(conn);
                        break;
                    case deleteUser:
                        deleteUser(conn);
                        break;
                    case exit:
                        System.out.println("Exiting...");
                        break;
                    default:
                        System.out.println("Invalid option.");
                        break;
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }

        private static void addNewUser (Connection conn){
            try {
                String userName = DatabaseUtils.getString("Enter username: ");
                String password = DatabaseUtils.getString("Enter password: ");
                String userRole = DatabaseUtils.getStrFromList(new String[] {"admin", "teacher", "student"});
                String sql = "INSERT INTO Users (UserName, Password, UserRole) VALUES (?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, userName);
                stmt.setString(2, password);
                stmt.setString(3, userRole);
                stmt.executeUpdate();

            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }

        private static void deleteUser (Connection conn) throws SQLException {
        try{
            if(DatabaseUtils.countTableEntries(conn, "users", "userrole != 'admin'") == 0) {
                System.out.println("There are no users in the database.");
                return;
            }
            conn.setAutoCommit(false);
            int userID = DatabaseUtils.getValidID(conn, "users", "userID", new String[]{"userID", "username"}, "userrole != 'admin'");
            String sql = "DELETE FROM users WHERE userID = " + userID;
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e){
            conn.rollback();
            throw e;
        }
        finally {
            conn.setAutoCommit(true);
            }
        }

        private static void gradeExam (Connection conn, int userID) throws SQLException {
            try {
                conn.setAutoCommit(false);
                if(DatabaseUtils.countTableEntries(conn, "exam", "userID = " + userID) == 0) {
                    System.out.println("Please create an exam before using this option.");
                    return;
                }
                int examID = DatabaseUtils.getValidID(conn, "exam", "examID", new String[]{"examID", "title"}, "userID = " + userID);
                int studentID = DatabaseUtils.getValidID(conn, "users", "userID", new String[]{"userID", "userName"}, "userRole = 'student'");
                int grade = DatabaseUtils.getIntInRange("Enter new grade: ", 0, 100);
                String sql = "INSERT INTO studentgrades (userid, examID, grade) VALUES (?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, studentID);
                stmt.setInt(2, examID);
                stmt.setInt(3, grade);
                stmt.executeUpdate();
                stmt.close();
                conn.commit();
            } catch (SQLException e){
                conn.rollback();
                throw e;
                } finally {
                conn.setAutoCommit(true);
            }
        }

    private void showStudentGrades(Connection conn, int userID) throws SQLException {
        DatabaseUtils.printTable(conn, "studentgrades sg JOIN exam e ON sg.ExamID = e.ExamID", new String[]{"e.title", "sg.grade"}, "sg.userID = " + userID);
    }

    private void calcGPA (Connection conn, int userID) throws SQLException {
        String sql = "SELECT AVG(grade) FROM studentgrades WHERE userID = " + userID;
        PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            System.out.println("Your GPA is: " + rs.getDouble(1));
        }
    }
}

