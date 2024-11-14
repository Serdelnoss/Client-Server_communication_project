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

public class RegistrationProcess {
    private final String url;

    public RegistrationProcess(String url) {
        this.url = url;
    }

    public void registerUser(String name, String login, String password, PrintWriter out) {
        List<String[]> config = Utilz.readConfig();
        String dbUser = Utilz.getDbUser(config);
        String dbPassword = Utilz.getDbPassword(config);
        try {
            if (checkIfUserDoesntExists(dbUser, dbPassword, name)) {
                String sql = "INSERT INTO users (name, login, password, salt) VALUES (?, ?, ?, ?);";
                prepareStatement(dbUser, dbPassword, sql, password, name, login, out);
            } else {
                out.println("status: ERROR;User already exists.");
                out.flush();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void prepareStatement(String dbUser, String dbPassword, String sql, String password,
                                       String name, String login, PrintWriter out) {
        try (Connection connection = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            sendRequest(password, statement, name, login, out);
        } catch (SQLException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendRequest(String password, PreparedStatement statement, String name, String login, PrintWriter out) throws NoSuchAlgorithmException, SQLException {
        byte [] salt = PasswordHasher.generateSalt();
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashedPassword = PasswordHasher.hashPassword(password, salt);
        statement.setString(1, name);
        statement.setString(2, login);
        statement.setString(3, hashedPassword);
        statement.setString(4, saltBase64);
        executeStatement(statement, name, out);
    }

    private void executeStatement(PreparedStatement statement, String name, PrintWriter out) throws SQLException {
        statement.execute();
        Utilz.log(LocalDateTime.now() + ": Successfully registered user with name "
                + name + ".");
        out.println("status: OK;Success.");
        out.flush();
    }

    private boolean checkIfUserDoesntExists(String dbUser, String dbPassword, String name) throws SQLException {
        String sql = "SELECT id FROM users WHERE name = ?";
        try (Connection connection = DriverManager.getConnection(url, dbUser, dbPassword)) {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return false;
                } else {
                    return true;
                }
            }
        }
    }
}
