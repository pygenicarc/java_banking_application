package banking.model;

/**
 * Enumeration representing the supported banking transaction types.
 */
public enum TransactionType {
    OPENING_DEPOSIT("Opening Deposit"),
    DEPOSIT("Deposit"),
    WITHDRAWAL("Withdrawal"),
    TRANSFER_OUT("Transfer Out"),
    TRANSFER_IN("Transfer In");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isCredit() {
        return this == OPENING_DEPOSIT || this == DEPOSIT || this == TRANSFER_IN;
    }

    public boolean isDebit() {
        return this == WITHDRAWAL || this == TRANSFER_OUT;
    }

    public static TransactionType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction type string cannot be null or empty.");
        }
        for (TransactionType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim()) || type.displayName.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown transaction type: " + value);
    }
}
