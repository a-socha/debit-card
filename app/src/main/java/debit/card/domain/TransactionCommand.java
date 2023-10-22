package debit.card.domain;

import java.math.BigDecimal;
import java.util.UUID;

public sealed interface TransactionCommand permits
        ChargeCommand,
        PayOff {
    UUID transactionId();

    BigDecimal value();

    static TransactionCommand charge(UUID transactionId, BigDecimal chargeAmount) {
        return new ChargeCommand(transactionId, chargeAmount.negate());
    }

    static TransactionCommand payOff(UUID transactionId, BigDecimal payOffAmount) {
        return new PayOff(transactionId, payOffAmount);
    }
}

record ChargeCommand(UUID transactionId, BigDecimal value) implements TransactionCommand {
}

record PayOff(UUID transactionId, BigDecimal value) implements TransactionCommand {
}
