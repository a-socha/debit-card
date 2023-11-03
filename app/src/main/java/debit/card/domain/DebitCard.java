package debit.card.domain;

import debit.card.view.DebitCardSummary;
import io.vavr.collection.List;
import io.vavr.control.Option;

import java.math.BigDecimal;
import java.util.UUID;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;
import static java.math.BigDecimal.ZERO;

class DebitCard {
    private final UUID cardUUID;
    private final Long version;
    private final List<DebitCardEvent> pendingChanges;
    private final Option<BigDecimal> debitLimit;
    private final BigDecimal balance;
    private final boolean blocked;

    private DebitCard(
            UUID cardUUID,
            Long version,
            List<DebitCardEvent> events,
            Option<BigDecimal> debitLimit,
            BigDecimal balance,
            boolean blocked
    ) {
        this.cardUUID = cardUUID;
        this.version = version;
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
        return applyWithAppend(new DebitCardEvent.TransactionAccepted(transaction.transactionId(), transaction.value()));
    }

    private DebitCard chargeCard(ChargeCommand transaction) {
        if (!blocked && hasEnoughMoney(transaction.value())) {
            return applyWithAppend(new DebitCardEvent.TransactionAccepted(transaction.transactionId(), transaction.value()));
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
            case DebitCardEvent.TransactionAccepted transactionAccepted -> transactionAccepted(transactionAccepted);
            case DebitCardEvent.TransactionRejected transactionRejected -> transactionRejected(transactionRejected);
            case DebitCardEvent.CardBlocked cardBlocked -> cardBlocked(cardBlocked);
            case DebitCardEvent.CardBlockedRejected cardBlockedRejected -> cardBlockedRejected(cardBlockedRejected);
            case DebitCardEvent.CardUnblocked cardUnblocked -> cardUnblocked(cardUnblocked);
        };
    }


    private DebitCard cardBlocked(DebitCardEvent.CardBlocked cardBlocked) {
        return new DebitCard(cardUUID, version, registerChange(cardBlocked), debitLimit, balance, true);
    }

    private DebitCard cardBlockedRejected(DebitCardEvent.CardBlockedRejected cardBlockedRejected) {
        return rejectOperation(cardBlockedRejected);
    }

    private DebitCard cardUnblocked(DebitCardEvent.CardUnblocked cardUnblocked) {
        return new DebitCard(cardUUID, version, registerChange(cardUnblocked), debitLimit, balance, false);
    }

    private DebitCard transactionAccepted(DebitCardEvent.TransactionAccepted transactionAccepted) {
        return new DebitCard(cardUUID, version, registerChange(transactionAccepted), debitLimit, balance.add(transactionAccepted.value()), blocked);
    }

    private DebitCard transactionRejected(DebitCardEvent.TransactionRejected transactionRejected) {
        return rejectOperation(transactionRejected);
    }

    private DebitCard limitAssigned(DebitCardEvent.LimitAssigned created) {
        return new DebitCard(cardUUID, version, registerChange(created), some(created.limit()), ZERO, blocked);
    }

    private List<DebitCardEvent> registerChange(DebitCardEvent debitCardEvent) {
        return pendingChanges.append(debitCardEvent);
    }

    private DebitCard rejectOperation(DebitCardEvent rejectionEvent) {
        return new DebitCard(cardUUID, version, registerChange(rejectionEvent), debitLimit, balance, blocked);
    }

    DebitCard flushChanges() {
        return new DebitCard(cardUUID, version, List.empty(), debitLimit, balance, blocked);
    }

    static DebitCard createNew() {
        return createNew(UUID.randomUUID());
    }

    static DebitCard createNew(UUID cardUUID) {
        return createNew(cardUUID, null);
    }
    static DebitCard createNew(UUID cardUUID, Long version) {
        return new DebitCard(cardUUID, version, List.empty(), none(), ZERO, false);
    }

    static DebitCard fromEvents(UUID cardUUID, Long version, List<DebitCardEvent> events) {
        var cardWithChanges = events.foldLeft(createNew(cardUUID, version), DebitCard::applyWithAppend);
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

    DebitCardSummary toSummary() {
        return new DebitCardSummary(
                cardUUID,
                balance,
                debitLimit,
                blocked
        );
    }

    Long version() {
        return this.version;
    }
}


sealed interface DebitCardEvent permits
        DebitCardEvent.Success,
        DebitCardEvent.Failure {

    sealed interface Success extends DebitCardEvent permits
            DebitCardEvent.LimitAssigned,
            DebitCardEvent.TransactionAccepted,
            DebitCardEvent.CardBlocked,
            DebitCardEvent.CardUnblocked {
    }

    sealed interface Failure extends DebitCardEvent permits
            DebitCardEvent.TransactionRejected,
            DebitCardEvent.CardBlockedRejected {
    }

    record LimitAssigned(BigDecimal limit) implements DebitCardEvent.Success {
    }

    record TransactionAccepted(UUID uuid, BigDecimal value) implements DebitCardEvent.Success {

    }

    record TransactionRejected(UUID uuid, BigDecimal value) implements DebitCardEvent.Failure {
    }

    record CardBlockedRejected() implements DebitCardEvent.Failure {
    }

    record CardBlocked() implements DebitCardEvent.Success {
    }

    record CardUnblocked() implements DebitCardEvent.Success {
    }
}