package debit.card.domain

import debit.card.bd
import debit.card.domain.DebitCard.createNew
import debit.card.domain.commands.AssignLimitCommand
import debit.card.domain.commands.BlockCardCommand
import debit.card.domain.commands.ChargeCardCommand
import debit.card.domain.commands.PayOffCardCommand
import debit.card.view.DebitCardSummary
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class DebitCardFacadeTest {

    private val repository = InMemoryDebitCardRepository()

    val facade = DebitCardModule().facade(repository)

    private val cardUUID = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        repository.clean()
    }

    @Test
    fun `should create and store new debit card`() {
        // when
        val cardUUID = facade.createNewCard()
        val storedSummary = getSummaryById(cardUUID)

        // then
        assertThat(storedSummary.balance).isEqualTo("0")
        assertThat(storedSummary.limit).isEmpty()
        assertThat(storedSummary.blocked).isFalse()
    }

    @Test
    fun `should assign limit to the card`() {
        // given
        thereIsACard(createNew(cardUUID))

        // when
        val result = facade.assignLimitToCard(AssignLimitCommand(cardUUID, "15".bd))

        // then
        assertThat(result.isSuccess).isTrue()
        val cardSummary = getSummaryById(cardUUID)
        assertThat(cardSummary.limit.get()).isEqualTo("15".bd)
    }

    @Test
    fun `should not assign limit to the card with already assigned limit`() {
        // given
        thereIsACard(createNew(cardUUID).assignLimit("20".bd))

        // when
        val result = facade.assignLimitToCard(AssignLimitCommand(cardUUID, "15".bd))

        // then
        assertThat(result.isSuccess).isFalse()
        assertThat(result.errors()).containsExactly(LimitAlreadyAssigned())
        val cardSummary = getSummaryById(cardUUID)
        assertThat(cardSummary.limit.get()).isEqualTo("20".bd)
    }


    @Test
    fun `should be able to charge a card when it does not exceed limit`() {
        // given
        thereIsACard(createNew(cardUUID).assignLimit("-20".bd))

        // when
        val result = facade.chargeCard(ChargeCardCommand(cardUUID, UUID.randomUUID(), "15".bd))

        // then
        assertThat(result.isSuccess).isTrue()
        val summary = getSummaryById(cardUUID)
        assertThat(summary.balance).isEqualTo("-15")
        assertThat(summary.limit.get()).isEqualTo("-20")
    }

    @Test
    fun `should not be able to charge a card when it exceeds limit`() {
        // given
        thereIsACard(createNew(cardUUID).assignLimit("-20".bd))

        // when
        val result = facade.chargeCard(ChargeCardCommand(cardUUID, UUID.randomUUID(), "25".bd))

        // then
        assertThat(result.isSuccess).isFalse()
        val summary = getSummaryById(cardUUID)
        assertThat(summary.balance).isEqualTo("0")
        assertThat(summary.limit.get()).isEqualTo("-20")
    }

    @Test
    fun `should be able to block a card`() {
        // given
        thereIsACard(createNew(cardUUID).assignLimit("-20".bd))

        // when
        val result = facade.blockCard(BlockCardCommand(cardUUID))

        // then
        assertThat(result.isSuccess).isTrue()
        val summary = getSummaryById(cardUUID)
        assertThat(summary.blocked).isTrue()
    }

    @Test
    fun `cannot block blocked card`() {
        // given
        thereIsACard(createNew(cardUUID).assignLimit("-20".bd).block())

        // when
        val result = facade.blockCard(BlockCardCommand(cardUUID))

        // then
        assertThat(result.isSuccess).isFalse()
        assertThat(result.errors()).containsExactly(CannotBlockCardError())
        val summary = getSummaryById(cardUUID)
        assertThat(summary.blocked).isTrue()
    }

    @Test
    fun `should not allow to charge blocked card`() {
        // given
        thereIsACard(createNew(cardUUID).assignLimit("-20".bd).block())

        // when
        val result = facade.chargeCard(ChargeCardCommand(cardUUID, UUID.randomUUID(), "5".bd))

        // then
        assertThat(result.isSuccess).isFalse()
        assertThat(result.errors()).containsExactly(CannotChargeError())
        val summary = getSummaryById(cardUUID)
        assertThat(summary.blocked).isTrue()
        assertThat(summary.balance).isEqualTo("0")
        assertThat(summary.limit.get()).isEqualTo("-20")

    }

    @Test
    fun `should be able pay off`() {
        // given
        thereIsACard(createNew(cardUUID).assignLimit("-20".bd))

        // when
        val result = facade.payOffCard(PayOffCardCommand(cardUUID, UUID.randomUUID(), "15".bd))

        // then
        assertThat(result.isSuccess).isTrue()
        val summary = getSummaryById(cardUUID)
        assertThat(summary.balance).isEqualTo("15")
        assertThat(summary.limit.get()).isEqualTo("-20")
    }

    @Test
    fun `should be able pay off card even if it is blocked`() {
        // given
        thereIsACard(createNew(cardUUID).assignLimit("-20".bd).block())

        // when
        val result = facade.payOffCard(PayOffCardCommand(cardUUID, UUID.randomUUID(), "15".bd))

        // then
        assertThat(result.isSuccess).isTrue()
        val summary = getSummaryById(cardUUID)
        assertThat(summary.balance).isEqualTo("15")
        assertThat(summary.limit.get()).isEqualTo("-20")
    }

    private fun thereIsACard(card: DebitCard) {
        repository.save(card)
    }

    private fun getSummaryById(cardUUID: UUID): DebitCardSummary =
            facade.getSummary(cardUUID).get()
}