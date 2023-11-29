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
        return runOperationOnCardWithUuid(assignLimitCommand, card -> handleAssignLimitToCard(card, assignLimitCommand));
    }

    private DebitCardOperationResult<AssignLimitCommand> handleAssignLimitToCard(DebitCard card, AssignLimitCommand assignLimitCommand) {
        return handleCardOperationThatMayFail(
                () -> card.assignLimit(assignLimitCommand.limit()),
                assignLimitCommand,
                LimitAlreadyAssigned::new
        );
    }

    public DebitCardOperationResult<ChargeCardCommand> chargeCard(ChargeCardCommand chargeCardCommand) {
        return runOperationOnCardWithUuid(chargeCardCommand, card -> handleCardCharge(card, chargeCardCommand));
    }

    private DebitCardOperationResult<ChargeCardCommand> handleCardCharge(DebitCard card, ChargeCardCommand command) {
        return handleCardOperationThatMayFail(
                () -> card.applyTransaction(TransactionCommand.charge(command.transactionUUID(), command.amount())),
                command,
                CannotChargeError::new
        );
    }

    public DebitCardOperationResult<PayOffCardCommand> payOffCard(PayOffCardCommand chargeCardCommand) {
        return runOperationOnCardWithUuid(
                chargeCardCommand,
                (card) -> handlePayOffCard(card, chargeCardCommand)
        );
    }

    private DebitCardOperationResult<PayOffCardCommand> handlePayOffCard(DebitCard card, PayOffCardCommand command) {
        return handleCardOperationThatMayFail(
                () -> card.applyTransaction(TransactionCommand.payOff(command.transactionUUID(), command.amount())),
                command,
                CannotPayOffError::new
        );
    }

    public DebitCardOperationResult<BlockCardCommand> blockCard(BlockCardCommand blockCardCommand) {
        return runOperationOnCardWithUuid(
                blockCardCommand,
                (card) -> handleCardOperationThatMayFail(card::block, blockCardCommand, CannotBlockCardError::new)
        );
    }

    public DebitCardOperationResult<UnblockCardCommand> unblockCard(UnblockCardCommand unblockCardCommand) {
        return runOperationOnCardWithUuid(
                unblockCardCommand,
                (card) -> handleUnblockCard(card, unblockCardCommand)
        );
    }

    private DebitCardOperationResult<UnblockCardCommand> handleUnblockCard(DebitCard card, UnblockCardCommand unblockCardCommand) {
        var cardAfterBlock = card.unblock();
        debitCardRepository.save(cardAfterBlock);
        return DebitCardOperationResult.success(unblockCardCommand);
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

    private <T extends CardCommand> DebitCardOperationResult<T> handleCardOperationThatMayFail(
            Supplier<DebitCard> cardOperationResult,
            T cardCommand,
            Supplier<DebitCardError> debitCardError
    ) {
        var cardAfterOperation = cardOperationResult.get();
        debitCardRepository.save(cardAfterOperation);
        return cardAfterOperation.pendingChanges().singleOption()
                .fold(
                        () -> DebitCardOperationResult.failed(cardCommand, debitCardError.get()),
                        (result) -> handleFailedResult(result, cardCommand, debitCardError)
                );
    }

    private <T extends CardCommand> DebitCardOperationResult<T> handleFailedResult(DebitCardEvent result, T cardCommand, Supplier<DebitCardError> debitCardError) {
        return result instanceof DebitCardEvent.Success
                ? DebitCardOperationResult.success(cardCommand)
                : DebitCardOperationResult.failed(cardCommand, debitCardError.get());
    }


    private static <T extends CardCommand> Supplier<DebitCardOperationResult<T>> handleNotFoundCard(T assignLimitCommand) {
        return () -> DebitCardOperationResult.failed(assignLimitCommand, new CardNotFoundError());
    }

}
