package debit.card.domain

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.MongoDBContainer

@TestConfiguration
internal open class MongoDbTestContainerConfig {
    @Bean
    @ServiceConnection
    open fun mongoDbContainer(): MongoDBContainer {
        return MongoDBContainer("mongo:latest")
    }
}