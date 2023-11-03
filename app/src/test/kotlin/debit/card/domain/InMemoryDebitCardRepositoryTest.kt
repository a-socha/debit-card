package debit.card.domain

import debit.card.bd
import debit.card.domain.TransactionCommand.charge
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.*

internal abstract class DebitCardRepositoryTest {
    protected abstract val repository: DebitCardRepository

    private val debitCardId = UUID.randomUUID()

    @Test
    fun `should store debit card and flush all changes`() {
        // given
        val card = DebitCard.createNew(debitCardId)
                .assignLimit("10".bd)
                .applyTransaction(charge(UUID.randomUUID(), "5".bd))
                .block()
                .unblock()

        // when
        repository.save(card)

        // then
        val readCard = repository.getByUUID(debitCardId)
        assertThat(readCard.isDefined).isTrue()
        assertThat(readCard.get().toSummary()).isEqualTo(card.toSummary())
    }


    @Test
    fun `should read debit card summary`() {
        // given
        val card = DebitCard.createNew(debitCardId)
                .assignLimit("10".bd)
                .applyTransaction(charge(UUID.randomUUID(), "5".bd))
                .block()
                .unblock()

        // when
        repository.save(card)

        // then
        val readCard = repository.getSummaryByUUID(debitCardId)
        assertThat(readCard.isDefined).isTrue()
        assertThat(readCard.get()).isEqualTo(card.toSummary())
    }

    @Test
    fun `should throw exception when try to save stale object`() {
        // given
        val card = DebitCard.createNew(debitCardId)
                .assignLimit("10".bd)

        repository.save(card)

        // and
        val sameCard1 = repository.getByUUID(debitCardId).get()
        val sameCard2 = repository.getByUUID(debitCardId).get()

        // when
        sameCard1.applyTransaction(charge(UUID.randomUUID(), "5".bd))
        sameCard2.block()
                .unblock()

        repository.save(sameCard1)

        // then
        val readCard = repository.getSummaryByUUID(debitCardId)
        assertThat(readCard.isDefined).isTrue()
        assertThat(readCard.get()).isEqualTo(sameCard1.toSummary())

        // and
        assertThatThrownBy { repository.save(sameCard2) }.isInstanceOf(RuntimeException::class.java)

    }

}

internal class InMemoryDebitCardRepositoryTest : DebitCardRepositoryTest() {

    override val repository = InMemoryDebitCardRepository()

}