package debit.card.domain.commands;

import java.util.UUID;

public record UnblockCardCommand(
        UUID cardUUID
) implements CardCommand {
}
