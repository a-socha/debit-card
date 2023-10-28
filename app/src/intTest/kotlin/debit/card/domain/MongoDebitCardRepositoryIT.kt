@file:Suppress("invisible_reference", "invisible_member")

package debit.card.domain

import debit.card.domain.DebitCardModule.DEBIT_CARD_REPOSITORY
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import


@SpringBootTest(properties = [
    "${DEBIT_CARD_REPOSITORY}=mongo"
])
@Import(MongoDbTestContainerConfig::class)
internal class MongoDebitCardRepositoryIT : DebitCardRepositoryTest() {

    @Autowired
    lateinit var mongoRepository: MongoDebitCardRepository

    override val repository: DebitCardRepository
        get() = mongoRepository

}