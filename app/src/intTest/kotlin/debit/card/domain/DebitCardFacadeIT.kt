@file:Suppress("invisible_reference", "invisible_member")

package debit.card.domain

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest(properties = [
    "${DebitCardModule.DEBIT_CARD_REPOSITORY}=mongo"
])
@Import(MongoDbTestContainerConfig::class)
internal class DebitCardFacadeIT : DebitCardFacadeTest() {
    @Autowired
    lateinit var crudRepository: MongoDebitCardCrudRepository

    @Autowired
    lateinit var moduleWithMongo: DebitCardModule

    @Autowired
    lateinit var mongoRepository: MongoDebitCardRepository

    override val module: DebitCardModule
        get() = moduleWithMongo

    override val repository: DebitCardRepository
        get() = mongoRepository

    override fun cleanState() {
        crudRepository.deleteAll()
    }
}
