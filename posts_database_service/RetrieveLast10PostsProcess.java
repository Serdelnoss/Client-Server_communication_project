package posts_database_service;

import utilz.Utilz;

import java.io.PrintWriter;
import java.sql.*;
import java.util.List;

public class RetrieveLast10PostsProcess {
    private final String postsUrl;
    private final String usersUrl;
    private final PrintWriter out;

    public RetrieveLast10PostsProcess(String postsUrl, String usersUrl, PrintWriter out) {
        this.out = out;
        this.postsUrl = postsUrl;
        this.usersUrl = usersUrl;
    }

    public void fetchPosts() {
        String postsQuery = "SELECT id, content, user_id, created_at FROM posts ORDER BY created_at " +
                "DESC LIMIT 10;";
        List<String[]> config = Utilz.readConfig();
        String dbUser = Utilz.getDbUser(config);
        String dbPassword = Utilz.getDbPassword(config);

        try (Connection postsConnection = DriverManager.getConnection(postsUrl, dbUser, dbPassword);
             PreparedStatement postsStatement = postsConnection.prepareStatement(postsQuery)) {
             StringBuilder response = getResponse(postsStatement);
             out.println(response);
             out.println("status: OK;Success.");
             out.flush();
        } catch (SQLException e) {
            out.println("status: ERROR;Error in database connection.");
            Utilz.log("Error in database connection: " + e);
            throw new RuntimeException(e);
        }
    }

    private StringBuilder getResponse(PreparedStatement postsStatement) throws SQLException {
        StringBuilder response = new StringBuilder("status: OK;POSTS_LIST");
        try (ResultSet postsResult = postsStatement.executeQuery()) {
            while (postsResult.next()) {
                int postId = postsResult.getInt("id");
                String content = postsResult.getString("content");
                int userId = postsResult.getInt("user_id");
                Timestamp createdAt = postsResult.getTimestamp("created_at");
                String authorName = fetchAuthorName(userId);
                response.append(";POST_BEGIN")
                        .append(";").append(postId)
                        .append(";").append(content)
                        .append(";").append(authorName != null ? authorName : "unknown")
                        .append(";").append(createdAt)
                        .append(";POST_END");
            }
        }
        return response;
    }

    private String fetchAuthorName(int userId) {
        String authorName = null;
        String usersQuery = "SELECT name FROM users WHERE id = ?";
        List<String[]> config = Utilz.readConfig();
        String dbUser = Utilz.getDbUser(config);
        String dbPassword = Utilz.getDbPassword(config);
        try (Connection usersConnection = DriverManager.getConnection(usersUrl, dbUser, dbPassword);
            PreparedStatement usersStmt = usersConnection.prepareStatement(usersQuery)) {
            usersStmt.setInt(1, userId);
            try (ResultSet usersResult = usersStmt.executeQuery()) {
                if (usersResult.next()) {
                    authorName = usersResult.getString("name");
                }
            }
        } catch (SQLException e) {
            out.println("status: Error;Error in database connection.");
            Utilz.log("Error in database connection: " + e);
            e.printStackTrace();
        }
        return authorName;
    }
}
