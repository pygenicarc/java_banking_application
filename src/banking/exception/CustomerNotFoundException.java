package banking.exception;

/**
 * Thrown when a customer profile cannot be located.
 */
public class CustomerNotFoundException extends BankingException {
    private final String query;

    public CustomerNotFoundException(String query) {
        super("Customer not found matching query: " + query);
        this.query = query;
    }

    public String getQuery() {
        return query;
    }
}
