package debit.card.domain.commands;

import java.util.UUID;

public record BlockCardCommand(
        UUID cardUUID
) implements CardCommand {
}
