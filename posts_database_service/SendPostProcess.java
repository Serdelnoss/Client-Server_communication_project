package posts_database_service;

import utilz.Utilz;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class SendPostProcess {
    private final String url;
    private final PrintWriter out;

    public SendPostProcess(String url, PrintWriter out) {
        this.out = out;
        this.url = url;
    }

    public void sendPost(int userId, String content) {
        String sql = "INSERT INTO posts (user_id, content) VALUES (?, ?);";
        List<String[]> config = Utilz.readConfig();
        String dbUser = Utilz.getDbUser(config);
        String dbPassword = Utilz.getDbPassword(config);

        try (Connection connection = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql);) {
            statement.setInt(1, userId);
            statement.setString(2, content);
            executeStatement(statement, content, userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void executeStatement(PreparedStatement statement, String content, int userId) throws SQLException {
        statement.execute();
        Utilz.log("Post with content: '" + content +
              "' has sent by user " + userId);
        out.println("status: OK;Success.");
    }
}