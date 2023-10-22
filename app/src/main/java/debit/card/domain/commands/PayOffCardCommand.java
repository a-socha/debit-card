package debit.card.domain.commands;

import java.math.BigDecimal;
import java.util.UUID;

public record PayOffCardCommand(
        UUID cardUUID,
        UUID transactionUUID,
        BigDecimal amount
) implements CardCommand{
}
