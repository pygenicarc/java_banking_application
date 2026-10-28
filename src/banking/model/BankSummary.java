package banking.model;

import java.math.BigDecimal;

/**
 * Immutable value object holding consolidated banking system metrics.
 */
public class BankSummary {
    private final int totalCustomers;
    private final int totalAccounts;
    private final BigDecimal totalDepositsHeld;
    private final int totalTransactions;
    private final BigDecimal totalAmountDeposited;
    private final BigDecimal totalAmountWithdrawn;

    public BankSummary(int totalCustomers,
                       int totalAccounts,
                       BigDecimal totalDepositsHeld,
                       int totalTransactions,
                       BigDecimal totalAmountDeposited,
                       BigDecimal totalAmountWithdrawn) {
        this.totalCustomers = totalCustomers;
        this.totalAccounts = totalAccounts;
        this.totalDepositsHeld = totalDepositsHeld;
        this.totalTransactions = totalTransactions;
        this.totalAmountDeposited = totalAmountDeposited;
        this.totalAmountWithdrawn = totalAmountWithdrawn;
    }

    public int getTotalCustomers() {
        return totalCustomers;
    }

    public int getTotalAccounts() {
        return totalAccounts;
    }

    public BigDecimal getTotalDepositsHeld() {
        return totalDepositsHeld;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public BigDecimal getTotalAmountDeposited() {
        return totalAmountDeposited;
    }

    public BigDecimal getTotalAmountWithdrawn() {
        return totalAmountWithdrawn;
    }
}
