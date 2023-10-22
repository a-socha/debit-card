package debit.card.domain.commands;

import java.math.BigDecimal;
import java.util.UUID;

public record AssignLimitCommand(UUID cardUUID, BigDecimal limit) implements CardCommand {
}
