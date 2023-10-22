package debit.card.view;

import io.vavr.control.Option;

import java.math.BigDecimal;
import java.util.UUID;

public record DebitCardSummary(
        UUID cardUUID,
        BigDecimal balance,
        Option<BigDecimal> limit,
        boolean blocked
) {
}
