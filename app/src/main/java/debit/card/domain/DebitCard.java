package debit.card.domain;

import io.vavr.collection.List;
import io.vavr.control.Option;

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

    private DebitCard(
            UUID cardUUID,
            List<DebitCardEvent> events,
            Option<BigDecimal> debitLimit,
            BigDecimal balance
    ) {
        this.cardUUID = cardUUID;
        this.pendingChanges = events;
        this.debitLimit = debitLimit;
        this.balance = balance;
    }

    DebitCard applyTransaction(TransactionCommand transaction) {
        if (hasEnoughMoney(transaction.value())) {
            return applyWithAppend(new DebitCardEvent.TransactionProcessed(transaction.transactionId(), transaction.value()));
        }
        return applyWithAppend(new DebitCardEvent.TransactionRejected(transaction.transactionId(), transaction.value()));
    }

    private boolean hasEnoughMoney(BigDecimal value) {
        var balanceAfterTransaction = balance.add(value);
        return balanceAfterTransaction.compareTo(debitLimit.get()) >= 0;
    }

    static DebitCard createNew() {
        return new DebitCard(UUID.randomUUID(), List.empty(), none(), ZERO);
    }

    private DebitCard applyWithAppend(DebitCardEvent debitCardEvent) {
        return switch (debitCardEvent) {
            case DebitCardEvent.LimitAssigned created -> limitAssigned(created);
            case DebitCardEvent.TransactionProcessed transactionProcessed -> transactionAccepted(transactionProcessed);
            case DebitCardEvent.TransactionRejected transactionRejected -> transactionRejected(transactionRejected);
        };
    }

    private DebitCard transactionAccepted(DebitCardEvent.TransactionProcessed transactionProcessed) {
        return new DebitCard(cardUUID, pendingChanges.append(transactionProcessed), debitLimit, balance.add(transactionProcessed.value()));
    }

    private DebitCard transactionRejected(DebitCardEvent.TransactionRejected transactionRejected) {
        return new DebitCard(cardUUID, pendingChanges.append(transactionRejected), debitLimit, balance);
    }

    private DebitCard limitAssigned(DebitCardEvent.LimitAssigned created) {
        return new DebitCard(cardUUID, pendingChanges.append(created), some(created.limit()), ZERO);
    }

    DebitCard flushChanges() {
        return new DebitCard(cardUUID, List.empty(), debitLimit, balance);
    }

    static DebitCard fromEvents(UUID cardUUID, List<DebitCardEvent> events) {
        var cardWithChanges = events.foldLeft(cardWithUUID(cardUUID), DebitCard::applyWithAppend);
        return cardWithChanges.flushChanges();
    }


    private static DebitCard cardWithUUID(UUID cardUUID) {
        return new DebitCard(cardUUID, List.empty(), none(), ZERO);
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
}

sealed interface DebitCardEvent permits
        DebitCardEvent.LimitAssigned,
        DebitCardEvent.TransactionProcessed,
        DebitCardEvent.TransactionRejected {

    record LimitAssigned(BigDecimal limit) implements DebitCardEvent {
    }

    record TransactionProcessed(UUID uuid, BigDecimal value) implements DebitCardEvent {
    }

    record TransactionRejected(UUID uuid, BigDecimal value) implements DebitCardEvent {
    }
}

