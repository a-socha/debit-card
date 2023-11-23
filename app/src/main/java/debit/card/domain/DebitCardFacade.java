package debit.card.domain;

import debit.card.domain.commands.*;
import debit.card.view.DebitCardSummary;
import io.vavr.control.Option;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static debit.card.domain.DebitCardError.*;

public class DebitCardFacade {
    private final DebitCardRepository debitCardRepository;

    DebitCardFacade(DebitCardRepository debitCardRepository) {
        this.debitCardRepository = debitCardRepository;
    }

    public Option<DebitCardSummary> getSummary(UUID debitCardUUID) {
        return debitCardRepository.getSummaryByUUID(debitCardUUID);
    }

    public UUID createNewCard() {
        var card = DebitCard.createNew();
        debitCardRepository.save(card);
        return card.toSummary().cardUUID();
    }

    public DebitCardOperationResult<AssignLimitCommand> assignLimitToCard(AssignLimitCommand assignLimitCommand) {
        return debitCardRepository.getByUUID(assignLimitCommand.cardUUID())
                .fold(
                        handleNotFoundCard(assignLimitCommand),
                        handleAssignLimitToCard(assignLimitCommand)
                );
    }

    private static <T extends CardCommand> Supplier<DebitCardOperationResult<T>> handleNotFoundCard(T assignLimitCommand) {
        return () -> DebitCardOperationResult.failed(assignLimitCommand, new CardNotFoundError());
    }

    private Function<DebitCard, DebitCardOperationResult<AssignLimitCommand>> handleAssignLimitToCard(AssignLimitCommand assignLimitCommand) {
        return card -> {
            var cardWithAssignedLimit = card.assignLimit(assignLimitCommand.limit());
            debitCardRepository.save(cardWithAssignedLimit);
            return cardWithAssignedLimit.pendingChanges().singleOption()
                    .fold(
                            () -> DebitCardOperationResult.failed(assignLimitCommand, new LimitAlreadyAssigned()),
                            (change) -> DebitCardOperationResult.success(assignLimitCommand)
                    );
        };
    }

    public DebitCardOperationResult<ChargeCardCommand> chargeCard(ChargeCardCommand chargeCardCommand) {
        return debitCardRepository.getByUUID(chargeCardCommand.cardUUID())
                .fold(
                        handleNotFoundCard(chargeCardCommand),
                        (card) -> handleCardCharge(card, chargeCardCommand)
                );
    }

    private DebitCardOperationResult<ChargeCardCommand> handleCardCharge(DebitCard debitCard, ChargeCardCommand command) {
        var debitCardAfterTransaction = debitCard.applyTransaction(TransactionCommand.charge(command.transactionUUID(), command.amount()));
        var result = debitCardAfterTransaction.pendingChanges().single();

        debitCardRepository.save(debitCardAfterTransaction);

        if (result instanceof DebitCardEvent.Success) {
            return DebitCardOperationResult.success(command);
        } else {
            return DebitCardOperationResult.failed(command, new CannotChargeError());
        }
    }

    public DebitCardOperationResult<PayOffCardCommand> payOffCard(PayOffCardCommand chargeCardCommand) {
        return runOperationOnCardWithUuid(chargeCardCommand, (card) -> handlePayOffCard(card, chargeCardCommand));
    }

    private DebitCardOperationResult<PayOffCardCommand> handlePayOffCard(DebitCard card, PayOffCardCommand command) {
        var cardAfterOperation = card.applyTransaction(TransactionCommand.payOff(command.transactionUUID(), command.amount()));
        var result = cardAfterOperation.pendingChanges().single();
        debitCardRepository.save(cardAfterOperation);
        if (result instanceof DebitCardEvent.Success) {
            return DebitCardOperationResult.success(command);
        } else {
            return DebitCardOperationResult.failed(command, new CannotPayOffError());
        }
    }

    public DebitCardOperationResult<BlockCardCommand> blockCard(BlockCardCommand blockCardCommand) {
        return runOperationOnCardWithUuid(
                blockCardCommand,
                (card) -> handleBlockCard(card, blockCardCommand)
        );
    }

    private DebitCardOperationResult<BlockCardCommand> handleBlockCard(DebitCard card, BlockCardCommand blockCardCommand) {
        var cardAfterBlock = card.block();
        var result = cardAfterBlock.pendingChanges().single();
        var operationResult = result instanceof DebitCardEvent.Success
                ? DebitCardOperationResult.success(blockCardCommand)
                : DebitCardOperationResult.failed(blockCardCommand, new CannotBlockCardError());
        debitCardRepository.save(cardAfterBlock);
        return operationResult;
    }

    private <T extends CardCommand> DebitCardOperationResult<T> runOperationOnCardWithUuid(
            T cardCommand,
            Function<DebitCard, DebitCardOperationResult<T>> operation) {
        return debitCardRepository.getByUUID(cardCommand.cardUUID())
                .fold(
                        handleNotFoundCard(cardCommand),
                        operation
                );
    }

}
