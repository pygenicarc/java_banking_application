package banking.exception;

/**
 * Thrown when an account cannot be located with the specified account number.
 */
public class AccountNotFoundException extends BankingException {
    private final String accountNumber;

    public AccountNotFoundException(String accountNumber) {
        super("Account not found with account number: " + accountNumber);
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
