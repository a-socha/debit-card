package debit.card.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DebitCardModule {
    @Bean
    DebitCardFacade facade(DebitCardRepository debitCardRepository) {
        return new DebitCardFacade(debitCardRepository);
    }

    @Bean
    DebitCardRepository repository() {
        return new InMemoryDebitCardRepository();
    }

}
