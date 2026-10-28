package banking.exception;

/**
 * Thrown when customer, account, or transaction input fails validation rules.
 */
public class ValidationException extends BankingException {
    private final String fieldName;

    public ValidationException(String message) {
        super(message);
        this.fieldName = null;
    }

    public ValidationException(String fieldName, String message) {
        super(message);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
