package debit.card.domain

import debit.card.bd
import debit.card.domain.TransactionCommand.charge
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

internal class InMemoryDebitCardRepositoryTest {

    private val repository = InMemoryDebitCardRepository()

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
}