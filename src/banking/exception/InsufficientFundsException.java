package banking.exception;

import java.math.BigDecimal;

/**
 * Thrown when an account does not have sufficient balance for a withdrawal or transfer.
 */
public class InsufficientFundsException extends BankingException {
    private final BigDecimal currentBalance;
    private final BigDecimal requestedAmount;

    public InsufficientFundsException(String message, BigDecimal currentBalance, BigDecimal requestedAmount) {
        super(message);
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
    }

    public InsufficientFundsException(BigDecimal currentBalance, BigDecimal requestedAmount) {
        super(String.format("Insufficient funds: current balance is $%,.2f, but requested amount is $%,.2f.",
                currentBalance, requestedAmount));
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
}
