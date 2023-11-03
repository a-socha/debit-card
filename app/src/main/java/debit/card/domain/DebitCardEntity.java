package debit.card.domain;

import org.bson.types.Decimal128;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Document
record DebitCardEntity(
        @Id
        UUID debitCardId,
        @Version
        Long version,
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
            case LimitAssigned -> new DebitCardEvent.LimitAssigned(getBigDecimal("limit"));
            case TransactionAccepted -> new DebitCardEvent.TransactionAccepted(uuid(), getBigDecimal("value"));
            case TransactionRejected -> new DebitCardEvent.TransactionRejected(uuid(), getBigDecimal("value"));
            case CardBlockedRejected -> new DebitCardEvent.CardBlockedRejected();
            case CardBlocked -> new DebitCardEvent.CardBlocked();
            case CardUnblocked -> new DebitCardEvent.CardUnblocked();
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
            case DebitCardEvent.LimitAssigned la -> DebitCardEventType.LimitAssigned;
            case DebitCardEvent.TransactionAccepted ta -> DebitCardEventType.TransactionAccepted;
            case DebitCardEvent.TransactionRejected tr -> DebitCardEventType.TransactionRejected;
            case DebitCardEvent.CardBlocked cb -> DebitCardEventType.CardBlocked;
            case DebitCardEvent.CardUnblocked cu -> DebitCardEventType.CardUnblocked;
            case DebitCardEvent.CardBlockedRejected cbr -> DebitCardEventType.CardBlockedRejected;
        };
    }

    private static org.bson.Document bson(DebitCardEvent event) {
        return switch (event) {
            case DebitCardEvent.LimitAssigned la -> bson(Map.of("limit", la.limit()));
            case DebitCardEvent.TransactionAccepted ta -> bson(Map.of("uuid", ta.uuid(), "value", ta.value()));
            case DebitCardEvent.TransactionRejected tr -> bson(Map.of("uuid", tr.uuid(), "value", tr.value()));
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

