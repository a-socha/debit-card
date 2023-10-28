package debit.card.domain;

import debit.card.view.DebitCardSummary;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.bson.types.Decimal128;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.repository.CrudRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static debit.card.domain.DebitCardEvent.*;

class MongoDebitCardRepository implements DebitCardRepository {
    private final MongoDebitCardCrudRepository crudRepository;

    MongoDebitCardRepository(MongoDebitCardCrudRepository crudRepository) {
        this.crudRepository = crudRepository;
    }

    @Override
    public Option<DebitCard> getByUUID(UUID cardUUID) {
        return Option.ofOptional(crudRepository.findById(cardUUID))
                .map(this::toDebitCard);
    }

    private DebitCard toDebitCard(DebitCardEntity debitCardEntity) {
        var events = List.ofAll(debitCardEntity.events()).map(DebitCardEventEntity::toEvent);
        return DebitCard.fromEvents(debitCardEntity.debitCardId(), events);
    }

    @Override
    public Option<DebitCardSummary> getSummaryByUUID(UUID cardUUID) {
        return getByUUID(cardUUID).map(DebitCard::toSummary);
    }

    @Override
    public void save(DebitCard card) {
        var cardUUID = card.toSummary().cardUUID();
        var currentEvents = crudRepository.findById(cardUUID)
                .map(DebitCardEntity::events)
                .map(List::ofAll)
                .orElseGet(List::empty);

        var pendingChanges = card.pendingChanges().map(DebitCardEventEntity::from);
        var entity = new DebitCardEntity(
                cardUUID, currentEvents.appendAll(pendingChanges).toJavaList()
        );
        crudRepository.save(entity);
    }
}

@Document
record DebitCardEntity(
        @Id UUID debitCardId,

        java.util.List<DebitCardEventEntity> events
) {
}

record DebitCardEventEntity(
        DebitCardEventType type,
        org.bson.Document body
) {

    static DebitCardEventEntity from(DebitCardEvent event) {
        return new DebitCardEventEntity(getType(event), bson(event));
    }

    DebitCardEvent toEvent() {
        return switch (type) {
            case LimitAssigned -> new LimitAssigned(getBigDecimal("limit"));
            case TransactionAccepted -> new TransactionAccepted(uuid(), getBigDecimal("value"));
            case TransactionRejected -> new TransactionRejected(uuid(), getBigDecimal("value"));
            case CardBlockedRejected -> new CardBlockedRejected();
            case CardBlocked -> new CardBlocked();
            case CardUnblocked -> new CardUnblocked();
        };
    }

    private UUID uuid() {
        return body.get("uuid", UUID.class);
    }

    private BigDecimal getBigDecimal(String fieldName) {
        var bsonDecimal = body.get(fieldName, Decimal128.class);
        return bsonDecimal.bigDecimalValue();
    }

    @NotNull
    private static DebitCardEventType getType(DebitCardEvent event) {
        return switch (event) {
            case LimitAssigned la -> DebitCardEventType.LimitAssigned;
            case TransactionAccepted ta -> DebitCardEventType.TransactionAccepted;
            case TransactionRejected tr -> DebitCardEventType.TransactionRejected;
            case CardBlocked cb -> DebitCardEventType.CardBlocked;
            case CardUnblocked cu -> DebitCardEventType.CardUnblocked;
            case CardBlockedRejected cbr -> DebitCardEventType.CardBlockedRejected;
        };
    }

    private static org.bson.Document bson(DebitCardEvent event) {
        return switch (event) {
            case LimitAssigned la -> bson(Map.of("limit", la.limit()));
            case TransactionAccepted ta -> bson(Map.of("uuid", ta.uuid(), "value", ta.value()));
            case TransactionRejected tr -> bson(Map.of("uuid", tr.uuid(), "value", tr.value()));
            default -> bson(Map.of());
        };
    }

    @NotNull
    private static org.bson.Document bson(Map<String, Object> map) {
        return new org.bson.Document(map);
    }
}

enum DebitCardEventType {
    LimitAssigned,
    TransactionAccepted,
    TransactionRejected,
    CardBlockedRejected,
    CardBlocked,
    CardUnblocked
}

interface MongoDebitCardCrudRepository extends CrudRepository<DebitCardEntity, UUID> {
}
