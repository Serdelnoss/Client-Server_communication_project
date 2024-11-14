package utilz;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordHasher {
    public static String hashPassword(String password, byte [] salt) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(salt);
        byte[] hashedPassword = messageDigest.digest(password.getBytes());
        return Base64.getEncoder().encodeToString(hashedPassword);
    }

    public static byte [] generateSalt() {
        SecureRandom secureRandom = new SecureRandom();
        byte [] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return salt;
    }
}
