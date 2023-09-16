package debit.card.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionCommand(UUID transactionId, BigDecimal value) {
    static TransactionCommand charge(UUID transactionId, BigDecimal chargeAmount) {
        return new TransactionCommand(transactionId, chargeAmount.negate());
    }

    static TransactionCommand payOff(UUID transactionId, BigDecimal payOffAmount) {
        return new TransactionCommand(transactionId, payOffAmount);
    }
}
