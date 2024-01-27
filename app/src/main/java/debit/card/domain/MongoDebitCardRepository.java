package debit.card.domain;

import debit.card.view.DebitCardSummary;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

import static debit.card.domain.DebitCardModule.DEBIT_CARD_REPOSITORY;

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
        return DebitCard.fromEvents(debitCardEntity.debitCardId(), debitCardEntity.version(), events);
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
                cardUUID, card.version(), currentEvents.appendAll(pendingChanges).toJavaList()
        );
        crudRepository.save(entity);
    }
}

@ConditionalOnProperty(name = DEBIT_CARD_REPOSITORY, havingValue = "mongo")
interface MongoDebitCardCrudRepository extends CrudRepository<DebitCardEntity, UUID> {
}
