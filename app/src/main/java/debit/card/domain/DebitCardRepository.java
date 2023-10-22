package debit.card.domain;

import debit.card.view.DebitCardSummary;
import io.vavr.collection.List;
import io.vavr.control.Option;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

interface DebitCardRepository {
    Option<DebitCard> getByUUID(UUID cardUUID);

    Option<DebitCardSummary> getSummaryByUUID(UUID cardUUID);

    void save(DebitCard card);
}

class InMemoryDebitCardRepository implements DebitCardRepository {
    private final ConcurrentHashMap<UUID, List<DebitCardEvent>> inMemoryEventStore;

    InMemoryDebitCardRepository() {
        this.inMemoryEventStore = new ConcurrentHashMap<>();
    }

    void clean() {
        inMemoryEventStore.clear();
    }

    @Override
    public Option<DebitCard> getByUUID(UUID cardUUID) {
        return Option.some(inMemoryEventStore.get(cardUUID))
                .map(List::ofAll)
                .map(events -> DebitCard.fromEvents(cardUUID, events));
    }

    @Override
    public Option<DebitCardSummary> getSummaryByUUID(UUID cardUUID) {
        return getByUUID(cardUUID).map(DebitCard::toSummary);
    }

    @Override
    public void save(DebitCard card) {
        var uuid = card.toSummary().cardUUID();
        inMemoryEventStore.computeIfPresent(uuid, (key, events) -> events.appendAll(card.pendingChanges()));
        inMemoryEventStore.putIfAbsent(uuid, card.pendingChanges());
        card.flushChanges();
    }
}