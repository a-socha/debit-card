package debit.card.api;

import io.vavr.jackson.datatype.VavrModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class JacksonConfig {
    @Bean
    com.fasterxml.jackson.databind.Module vavrModule() {
        return new VavrModule();
    }
}
