package exceptions;

public class NotFoundInConfigurationException extends RuntimeException {
    public NotFoundInConfigurationException(String message) {
        super(message);
    }
}
