package users_database_service;

import utilz.PasswordHasher;
import utilz.Utilz;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

public class AuthProcess {
        private final String url;

        public AuthProcess(String url) {
            this.url = url;
        }

        public void validateUser(String login, String password, PrintWriter out) {
            String sql = "SELECT * FROM users WHERE login = ?";
            List<String[]> config = Utilz.readConfig();
            String dbUser = Utilz.getDbUser(config);
            String dbPassword = Utilz.getDbPassword(config);
            try (Connection connection = DriverManager.getConnection(url, dbUser, dbPassword);
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                 statement.setString(1, login);
                 try (ResultSet resultSet = statement.executeQuery()) {
                    printResult(resultSet, out, password);
                 } catch (NoSuchAlgorithmException e) {
                     throw new RuntimeException(e);
                 }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        private void printResult(ResultSet resultSet, PrintWriter out, String password) throws SQLException, NoSuchAlgorithmException {
            if (resultSet.next()) {
                String storedHashedPassword = resultSet.getString("password");
                String storedHash = resultSet.getString("salt");
                byte [] salt = Base64.getDecoder().decode(storedHash);
                String hashedPassword = PasswordHasher.hashPassword(password, salt);
                sendIfPasswordOK(storedHashedPassword, hashedPassword, resultSet, out);
            } else {
                out.println("status: ERROR;message: User with this login or password not found.");
            }
        }

        private void sendIfPasswordOK(String storedHashedPassword, String hashedPassword, ResultSet resultSet,
                                      PrintWriter out) throws SQLException {
            if (storedHashedPassword.equals(hashedPassword)) {
                Utilz.log(LocalDateTime.now() +
                        ": User '" + resultSet.getString("name") + "' has logged-in.");
                out.println(
                        "status: OK;" +
                                resultSet.getInt("id") + ";" +
                                resultSet.getString("name") + ";" +
                                resultSet.getString("login"));
            } else {
                out.println("status: ERROR;message: User with this login or password not found.");
            }
        }
}
