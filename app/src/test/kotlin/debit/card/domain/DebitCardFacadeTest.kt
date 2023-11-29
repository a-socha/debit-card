package debit.card.domain

import debit.card.bd
import debit.card.domain.DebitCard.createNew
import debit.card.domain.DebitCardError.*
import debit.card.domain.commands.*
import debit.card.view.DebitCardSummary
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal abstract class DebitCardFacadeTest {
    protected abstract val module: DebitCardModule
    protected abstract val repository: DebitCardRepository

    val facade: DebitCardFacade
        get() = module.facade(repository)

    protected abstract fun cleanState()

    private val cardUUID = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        cleanState()
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
        assertThat(result.error()).isEqualTo(LimitAlreadyAssigned())
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
        assertThat(result.error()).isEqualTo(CannotBlockCardError())
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
        assertThat(result.error()).isEqualTo(CannotChargeError())
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

    @Test
    fun `should be able to unblock not blocked card`() {
        // given
        thereIsACard(createNew(cardUUID).assignLimit("-20".bd))

        // when
        val result = facade.unblockCard(UnblockCardCommand(cardUUID))

        // then
        assertThat(result.isSuccess).isTrue()
        val summary = getSummaryById(cardUUID)
        assertThat(summary.blocked).isEqualTo(false)
    }

    @Test
    fun `should be able unblock card once it is blocked`() {
        // given
        thereIsACard(createNew(cardUUID).assignLimit("-20".bd).block())

        // when
        val result = facade.unblockCard(UnblockCardCommand(cardUUID))

        // then
        assertThat(result.isSuccess).isTrue()
        val summary = getSummaryById(cardUUID)
        assertThat(summary.blocked).isEqualTo(false)
    }

    private fun thereIsACard(card: DebitCard) {
        repository.save(card)
    }

    private fun getSummaryById(cardUUID: UUID): DebitCardSummary =
            facade.getSummary(cardUUID).get()

}

internal class DebitCardFacadeUnitTest : DebitCardFacadeTest() {
    override val module = DebitCardModule()
    override val repository = InMemoryDebitCardRepository()
    
    override fun cleanState() {
        repository.clean()
    }
}