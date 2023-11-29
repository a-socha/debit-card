package debit.card.domain;

import debit.card.view.DebitCardSummary;
import io.vavr.collection.List;
import io.vavr.control.Option;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

interface DebitCardRepository {
    Option<DebitCard> getByUUID(UUID cardUUID);

    Option<DebitCardSummary> getSummaryByUUID(UUID cardUUID);

    void save(DebitCard card);
}

class InMemoryDebitCardRepository implements DebitCardRepository {
    private final ConcurrentHashMap<UUID, VersionedEvents> inMemoryEventStore;

    InMemoryDebitCardRepository() {
        this.inMemoryEventStore = new ConcurrentHashMap<>();
    }

    void clean() {
        inMemoryEventStore.clear();
    }

    @Override
    public Option<DebitCard> getByUUID(UUID cardUUID) {
        return Option.of(inMemoryEventStore.get(cardUUID))
                .map(versionedEvents -> DebitCard.fromEvents(cardUUID, versionedEvents.version(), versionedEvents.events()));
    }

    @Override
    public Option<DebitCardSummary> getSummaryByUUID(UUID cardUUID) {
        return getByUUID(cardUUID).map(DebitCard::toSummary);
    }

    @Override
    public void save(DebitCard card) {
        var uuid = card.toSummary().cardUUID();
        inMemoryEventStore.computeIfPresent(uuid, (key, events) -> events.appendAll(card.version(), card.pendingChanges()));
        inMemoryEventStore.putIfAbsent(uuid, new VersionedEvents(0L, card.pendingChanges()));
        card.flushChanges();
    }
}

record VersionedEvents(
        Long version,
        List<DebitCardEvent> events
) {

    public VersionedEvents appendAll(Long version, List<DebitCardEvent> debitCardEvents) {
        if (Objects.equals(version, this.version)) {
            return new VersionedEvents(this.version + 1, events.appendAll(debitCardEvents));
        } else {
            throw new RuntimeException("Optimistic locking exception");
        }
    }
}