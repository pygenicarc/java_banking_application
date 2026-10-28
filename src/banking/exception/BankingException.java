package banking.exception;

/**
 * Base checked exception for banking application operations.
 */
public class BankingException extends Exception {
    public BankingException(String message) {
        super(message);
    }

    public BankingException(String message, Throwable cause) {
        super(message, cause);
    }
}
