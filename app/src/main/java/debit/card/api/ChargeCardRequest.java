package debit.card.api;

import java.math.BigDecimal;
import java.util.UUID;

record ChargeCardRequest(
        UUID transactionUUID,
        BigDecimal amount
) {
}
