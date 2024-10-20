import javax.xml.crypto.Data;
import java.sql.Connection;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseUtils.getConnection();
            if(conn == null) {System.out.println("Could not connect to database."); return;}
            User user;
            do {user = User.login(conn);} while (user == null); //get valid user
            switch (user.getUserRole()) {
                case "admin":
                    user.adminMenu(conn);
                    break;
                case "teacher":
                    user.teacherMenu(conn);
                    break;
                case "student":
                    user.studentMenu(conn);
                    break;
            }
            DatabaseUtils.closeConnection(conn);
        } catch (SQLException e){
            System.out.println(e.getMessage());
            return;
        }
    }
}
