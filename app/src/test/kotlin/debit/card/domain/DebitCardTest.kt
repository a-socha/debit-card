package debit.card.domain

import debit.card.bd
import debit.card.domain.TransactionCommand.charge
import debit.card.domain.TransactionCommand.payOff
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*

internal class DebitCardTest {

    private val firstTransactionId = UUID.randomUUID()
    private val secondTransactionId = UUID.randomUUID()

    @Test
    fun `should be able to assign limit to debit card if was not assigned before`() {
        // given
        val card = DebitCard.createNew()

        // when
        val result = card.assignLimit("-10.00".bd)

        // then
        assertThat(result.pendingChanges()).containsExactly(
                DebitCardEvent.LimitAssigned("-10.00".bd)
        )
    }

    @Test
    fun `should not be able to assign limit to debit card if was assigned before`() {
        // given
        val card = cardWithAssignedLimit("-10.00".bd)

        // when
        card.assignLimit("-15.00".bd)

        // then
        assertThat(card.pendingChanges()).isEmpty()
    }

    @Test
    fun `charge that is bigger than card balance should not be accepted`() {
        // given
        val card = cardWithAssignedLimit("-200".bd)

        // when
        val cardAfterTransaction = card.applyTransaction(charge(firstTransactionId, "200.01".bd))

        // then
        assertThat(cardAfterTransaction.pendingChanges())
                .containsExactly(
                        DebitCardEvent.TransactionRejected(firstTransactionId, "-200.01".bd)
                )
    }

    @Test
    fun `charge that is smaller than card balance should be accepted`() {
        // given
        val card = cardWithAssignedLimit("-200".bd)

        // when
        val result = card.applyTransaction(charge(firstTransactionId, "199.01".bd))

        // then
        assertThat(result.pendingChanges())
                .containsExactly(
                        DebitCardEvent.TransactionProcessed(firstTransactionId, "-199.01".bd)
                )
    }

    @Test
    fun `card pay off should add balance to card`() {
        // given
        val card = cardWithAssignedLimit("-200".bd)

        // when
        val result = card
                .applyTransaction(payOff(firstTransactionId, "10".bd))
                .applyTransaction(charge(secondTransactionId, "200.01".bd))

        // then
        assertThat(result.pendingChanges()).containsExactly(
                DebitCardEvent.TransactionProcessed(firstTransactionId, "10".bd),
                DebitCardEvent.TransactionProcessed(secondTransactionId, "-200.01".bd)
        )
    }

    @Test
    fun `charge transactions should be accepted until card balance is not below limit`() {
        // given
        val card = cardWithAssignedLimit("-200".bd)

        // when
        val result = card.applyTransactions(
                charge(UUID.randomUUID(), "10".bd),
                charge(UUID.randomUUID(), "50".bd),
                charge(UUID.randomUUID(), "100".bd),
                charge(UUID.randomUUID(), "40".bd),

                charge(UUID.randomUUID(), "1".bd),

                payOff(UUID.randomUUID(), "1".bd),
                charge(UUID.randomUUID(), "1".bd),
                charge(UUID.randomUUID(), "1".bd)
        )

        // then
        with(result.pendingChanges()) {
            assertThat(this[0]).isInstanceOf(DebitCardEvent.TransactionProcessed::class.java)
            assertThat(this[1]).isInstanceOf(DebitCardEvent.TransactionProcessed::class.java)
            assertThat(this[2]).isInstanceOf(DebitCardEvent.TransactionProcessed::class.java)
            assertThat(this[3]).isInstanceOf(DebitCardEvent.TransactionProcessed::class.java)

            assertThat(this[4]).isInstanceOf(DebitCardEvent.TransactionRejected::class.java)

            assertThat(this[5]).isInstanceOf(DebitCardEvent.TransactionProcessed::class.java)
            assertThat(this[6]).isInstanceOf(DebitCardEvent.TransactionProcessed::class.java)
            assertThat(this[7]).isInstanceOf(DebitCardEvent.TransactionRejected::class.java)
        }
    }

    private fun cardWithAssignedLimit(limit: BigDecimal): DebitCard = DebitCard.createNew()
            .assignLimit(limit)
            .flushChanges()
}

private fun DebitCard.applyTransactions(vararg transactions: TransactionCommand): DebitCard {
    return transactions.fold(this, DebitCard::applyTransaction)
}
