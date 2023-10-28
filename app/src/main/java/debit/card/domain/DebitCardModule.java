package debit.card.domain;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DebitCardModule {

    static final String DEBIT_CARD_REPOSITORY = "debit.card.repository";

    @Bean
    DebitCardFacade facade(DebitCardRepository debitCardRepository) {
        return new DebitCardFacade(debitCardRepository);
    }

    @Bean
    @ConditionalOnProperty(name = DEBIT_CARD_REPOSITORY, havingValue = "stub")
    DebitCardRepository repository() {
        return new InMemoryDebitCardRepository();
    }

    @Bean
    @ConditionalOnProperty(name = DEBIT_CARD_REPOSITORY, havingValue = "mongo")
    DebitCardRepository debitCardRepository(MongoDebitCardCrudRepository crudRepository) {
        return new MongoDebitCardRepository(crudRepository);
    }

}
