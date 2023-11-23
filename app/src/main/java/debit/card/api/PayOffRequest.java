package debit.card.api;

import java.math.BigDecimal;
import java.util.UUID;

record PayOffRequest(
        UUID transactionUUID,
        BigDecimal amount
) {
}
