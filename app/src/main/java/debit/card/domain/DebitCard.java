package debit.card.domain;

import io.vavr.collection.List;
import io.vavr.control.Option;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;
import static java.math.BigDecimal.ZERO;

class DebitCard {
    private final UUID cardUUID;
    private final List<DebitCardEvent> pendingChanges;
    private final Option<BigDecimal> debitLimit;
    private final BigDecimal balance;
    private final boolean blocked;

    private DebitCard(
            UUID cardUUID,
            List<DebitCardEvent> events,
            Option<BigDecimal> debitLimit,
            BigDecimal balance,
            boolean blocked
    ) {
        this.cardUUID = cardUUID;
        this.pendingChanges = events;
        this.debitLimit = debitLimit;
        this.balance = balance;
        this.blocked = blocked;
    }

    DebitCard applyTransaction(TransactionCommand transaction) {
        return switch (transaction) {
            case ChargeCommand chargeCommand -> chargeCard(chargeCommand);
            case PayOff payOffCommand -> payOffCard(payOffCommand);
        };
    }

    private DebitCard payOffCard(PayOff transaction) {
        return applyWithAppend(new DebitCardEvent.TransactionProcessed(transaction.transactionId(), transaction.value()));
    }

    private DebitCard chargeCard(ChargeCommand transaction) {
        if (!blocked && hasEnoughMoney(transaction.value())) {
            return applyWithAppend(new DebitCardEvent.TransactionProcessed(transaction.transactionId(), transaction.value()));
        }
        return applyWithAppend(new DebitCardEvent.TransactionRejected(transaction.transactionId(), transaction.value()));
    }

    private boolean hasEnoughMoney(BigDecimal value) {
        var balanceAfterTransaction = balance.add(value);
        return balanceAfterTransaction.compareTo(debitLimit.get()) >= 0;
    }


    private DebitCard applyWithAppend(DebitCardEvent debitCardEvent) {
        return switch (debitCardEvent) {
            case DebitCardEvent.LimitAssigned created -> limitAssigned(created);
            case DebitCardEvent.TransactionProcessed transactionProcessed -> transactionAccepted(transactionProcessed);
            case DebitCardEvent.TransactionRejected transactionRejected -> transactionRejected(transactionRejected);
            case DebitCardEvent.CardBlocked cardBlocked -> cardBlocked(cardBlocked);
            case DebitCardEvent.CardBlockedRejected cardBlockedRejected -> cardBlockedRejected(cardBlockedRejected);
            case DebitCardEvent.CardUnblocked cardUnblocked -> cardUnblocked(cardUnblocked);
        };
    }


    private DebitCard cardBlocked(DebitCardEvent.CardBlocked cardBlocked) {
        return new DebitCard(cardUUID, registerChange(cardBlocked), debitLimit, balance, true);
    }

    private DebitCard cardBlockedRejected(DebitCardEvent.CardBlockedRejected cardBlockedRejected) {
        return rejectOperation(cardBlockedRejected);
    }

    private DebitCard cardUnblocked(DebitCardEvent.CardUnblocked cardUnblocked) {
        return new DebitCard(cardUUID, registerChange(cardUnblocked), debitLimit, balance, false);
    }

    private DebitCard transactionAccepted(DebitCardEvent.TransactionProcessed transactionProcessed) {
        return new DebitCard(cardUUID, registerChange(transactionProcessed), debitLimit, balance.add(transactionProcessed.value()), blocked);
    }

    private DebitCard transactionRejected(DebitCardEvent.TransactionRejected transactionRejected) {
        return rejectOperation(transactionRejected);
    }

    private DebitCard limitAssigned(DebitCardEvent.LimitAssigned created) {
        return new DebitCard(cardUUID, registerChange(created), some(created.limit()), ZERO, blocked);
    }

    private List<DebitCardEvent> registerChange(DebitCardEvent debitCardEvent) {
        return pendingChanges.append(debitCardEvent);
    }

    private DebitCard rejectOperation(DebitCardEvent rejectionEvent) {
        return new DebitCard(cardUUID, registerChange(rejectionEvent), debitLimit, balance, blocked);
    }

    DebitCard flushChanges() {
        return new DebitCard(cardUUID, List.empty(), debitLimit, balance, blocked);
    }

    static DebitCard createNew() {
        return createNew(UUID.randomUUID());
    }

    private static DebitCard createNew(UUID cardUUID) {
        return new DebitCard(cardUUID, List.empty(), none(), ZERO, false);
    }

    static DebitCard fromEvents(UUID cardUUID, List<DebitCardEvent> events) {
        var cardWithChanges = events.foldLeft(createNew(cardUUID), DebitCard::applyWithAppend);
        return cardWithChanges.flushChanges();
    }


    List<DebitCardEvent> pendingChanges() {
        return pendingChanges;
    }

    public DebitCard assignLimit(BigDecimal limit) {
        if (debitLimit.isEmpty()) {
            return applyWithAppend(new DebitCardEvent.LimitAssigned(limit));
        }
        return this;
    }

    public DebitCard block() {
        return blocked
                ? applyWithAppend(new DebitCardEvent.CardBlockedRejected())
                : applyWithAppend(new DebitCardEvent.CardBlocked());

    }

    public DebitCard unblock() {
        if (blocked) {
            return applyWithAppend(new DebitCardEvent.CardUnblocked());
        }
        return this;
    }
}


sealed interface DebitCardEvent permits
        DebitCardEvent.LimitAssigned,
        DebitCardEvent.TransactionProcessed,
        DebitCardEvent.TransactionRejected,
        DebitCardEvent.CardBlocked,
        DebitCardEvent.CardBlockedRejected,
        DebitCardEvent.CardUnblocked {


    record LimitAssigned(BigDecimal limit) implements DebitCardEvent {
    }

    record TransactionProcessed(UUID uuid, BigDecimal value) implements DebitCardEvent {
    }

    record TransactionRejected(UUID uuid, BigDecimal value) implements DebitCardEvent {
    }

    record CardBlockedRejected() implements DebitCardEvent {
    }

    record CardBlocked() implements DebitCardEvent {
    }

    record CardUnblocked() implements DebitCardEvent {
    }
}