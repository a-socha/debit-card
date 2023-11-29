package debit.card.api

import debit.card.bd
import debit.card.domain.*
import debit.card.domain.DebitCardError.*
import debit.card.domain.commands.*
import debit.card.view.DebitCardSummary
import io.vavr.control.Option
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.BDDMockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.util.UUID
import java.util.stream.Stream

private val debitCardId = UUID.randomUUID()

@WebMvcTest(DebitCardController::class)
@Import(JacksonConfig::class)
internal class DebitCardControllerIT {
    @MockBean
    private lateinit var debitCardFacade: DebitCardFacade

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should return uuid on debit card creation`() {
        // given
        given(debitCardFacade.createNewCard()).willReturn(debitCardId)

        // expect
        mockMvc.post("/v1/debit-cards")
                .andDo { print() }
                .andExpect {
                    status { isOk() }
                    content {
                        contentType("application/json")
                        json("""
                            {
                                "debitCardId": "$debitCardId"
                            }
                        """.trimIndent())
                    }
                }
    }

    @ParameterizedTest
    @MethodSource("cardSummaries")
    fun `should return card summary`(
            summary: DebitCardSummary, expectedBody: String
    ) {
        // given
        given(debitCardFacade.getSummary(debitCardId)).willReturn(Option.of(summary))

        // expect
        mockMvc.get("/v1/debit-cards/$debitCardId")
                .andDo { print() }
                .andExpect {
                    status { isOk() }
                    content {
                        contentType("application/json")
                        json(expectedBody)
                    }
                }
    }

    @Test
    fun `should return 404 for not found summary`() {
        // given
        given(debitCardFacade.getSummary(debitCardId)).willReturn(Option.none())

        // expect
        mockMvc.get("/v1/debit-cards/$debitCardId")
                .andDo { print() }
                .andExpect {
                    status { isNotFound() }
                    content {
                        contentType("application/json")
                        json("""
                            {
                                "type": "CardNotFoundError",
                                "details": {
                                    "debitCardUUID": "$debitCardId"
                                }
                            }
                        """.trimIndent())
                    }
                }
    }

    @Test
    fun `should return 200 when card limmit assigned`() {
        // given
        val assignLimitCommand = AssignLimitCommand(debitCardId, "10".bd)
        given(debitCardFacade.assignLimitToCard(assignLimitCommand))
                .willReturn(DebitCardOperationResult.success(assignLimitCommand))

        // expect
        mockMvc.put("/v1/debit-cards/$debitCardId/limit") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "limit": ${assignLimitCommand.limit} 
                }
            """.trimIndent()
        }
                .andDo { print() }
                .andExpect {
                    status { isOk() }
                    content {
                        contentType("application/json")
                        json("""
                            {
                                "cardUUID": "$debitCardId",
                                "limit": ${assignLimitCommand.limit}
                            }
                        """.trimIndent())
                    }
                }
    }

    @ParameterizedTest
    @MethodSource("cardAssignmentErrors")
    fun `should return error when card limmit assigned failed`(
            debitCardError: DebitCardError, errorName: String, status: Int
    ) {
        // given
        val assignLimitCommand = AssignLimitCommand(debitCardId, "10".bd)
        given(debitCardFacade.assignLimitToCard(assignLimitCommand))
                .willReturn(DebitCardOperationResult.failed(assignLimitCommand, debitCardError))

        // expect
        mockMvc.put("/v1/debit-cards/$debitCardId/limit") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "limit": ${assignLimitCommand.limit} 
                }
            """.trimIndent()
        }
                .andDo { print() }
                .andExpect {
                    status { isEqualTo(status) }
                    content {
                        contentType("application/json")
                        json("""
                            {
                                "type": "$errorName",
                                "details": {
                                    "cardUUID": "$debitCardId",
                                    "limit": ${assignLimitCommand.limit}
                                }
                            }
                        """.trimIndent())
                    }
                }
    }

    @Test
    fun `should return 200 when card charge successfull`() {
        // given
        val transactionId = UUID.randomUUID()
        val chargeCardCommand = ChargeCardCommand(
                debitCardId, transactionId, "10".bd
        )
        given(debitCardFacade.chargeCard(chargeCardCommand))
                .willReturn(DebitCardOperationResult.success(chargeCardCommand))

        // expect
        mockMvc.put("/v1/debit-cards/$debitCardId/charge") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "transactionUUID": "$transactionId",
                    "amount": ${chargeCardCommand.amount} 
                }
            """.trimIndent()
        }
                .andDo { print() }
                .andExpect {
                    status { isOk() }
                    content {
                        contentType("application/json")
                        json("""
                            {
                                   "cardUUID": "$debitCardId",
                                   "transactionUUID": "$transactionId",
                                   "amount": ${chargeCardCommand.amount}
                            }
                        """.trimIndent())
                    }
                }
    }

    @ParameterizedTest
    @MethodSource("chargeErrors")
    fun `should return error when card charge failed`(
            debitCardError: DebitCardError, errorName: String, status: Int
    ) {
        // given
        val transactionId = UUID.randomUUID()
        val chargeCardCommand = ChargeCardCommand(
                debitCardId, transactionId, "10".bd
        )
        given(debitCardFacade.chargeCard(chargeCardCommand))
                .willReturn(DebitCardOperationResult.failed(chargeCardCommand, debitCardError))

        // expect
        mockMvc.put("/v1/debit-cards/$debitCardId/charge") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "transactionUUID": "$transactionId",
                    "amount": ${chargeCardCommand.amount} 
                }
            """.trimIndent()
        }
                .andDo { print() }
                .andExpect {
                    status { isEqualTo(status) }
                    content {
                        contentType("application/json")
                        json("""
                            {
                                "type": "$errorName",
                                "details": {
                                    "cardUUID": "$debitCardId",
                                    "transactionUUID": "$transactionId",
                                    "amount": ${chargeCardCommand.amount}
                                }
                            }
                        """.trimIndent())
                    }
                }
    }

    @Test
    fun `should return 200 when card pay off successfull`() {
        // given
        val transactionId = UUID.randomUUID()
        val payOffCardCommand = PayOffCardCommand(
                debitCardId, transactionId, "10".bd
        )
        given(debitCardFacade.payOffCard(payOffCardCommand))
                .willReturn(DebitCardOperationResult.success(payOffCardCommand))

        // expect
        mockMvc.put("/v1/debit-cards/$debitCardId/pay-off") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "transactionUUID": "$transactionId",
                    "amount": ${payOffCardCommand.amount} 
                }
            """.trimIndent()
        }
                .andDo { print() }
                .andExpect {
                    status { isOk() }
                    content {
                        contentType("application/json")
                        json("""
                            {
                                   "cardUUID": "$debitCardId",
                                   "transactionUUID": "$transactionId",
                                   "amount": ${payOffCardCommand.amount}
                            }
                        """.trimIndent())
                    }
                }
    }

    @ParameterizedTest
    @MethodSource("payOffErrors")
    fun `should return error when card pay off failed`(
            debitCardError: DebitCardError, errorName: String, status: Int
    ) {
        // given
        val transactionId = UUID.randomUUID()
        val payOffCardCommand = PayOffCardCommand(
                debitCardId, transactionId, "10".bd
        )
        given(debitCardFacade.payOffCard(payOffCardCommand))
                .willReturn(DebitCardOperationResult.failed(payOffCardCommand, debitCardError))

        // expect
        mockMvc.put("/v1/debit-cards/$debitCardId/pay-off") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "transactionUUID": "$transactionId",
                    "amount": ${payOffCardCommand.amount} 
                }
            """.trimIndent()
        }
                .andDo { print() }
                .andExpect {
                    status { isEqualTo(status) }
                    content {
                        contentType("application/json")
                        json("""
                            {
                                "type": "$errorName",
                                "details": {
                                    "cardUUID": "$debitCardId",
                                    "transactionUUID": "$transactionId",
                                    "amount": ${payOffCardCommand.amount}
                                }
                            }
                        """.trimIndent())
                    }
                }
    }

    @Test
    fun `should return 200 when card block successfull`() {
        // given
        given(debitCardFacade.blockCard(BlockCardCommand(debitCardId)))
                .willReturn(DebitCardOperationResult.success(BlockCardCommand(debitCardId)))

        // expect
        mockMvc.put("/v1/debit-cards/$debitCardId/block")
                .andDo { print() }
                .andExpect {
                    status { isOk() }
                    content {
                        contentType("application/json")
                        json("""
                            {
                                   "cardUUID": "$debitCardId"
                            }
                        """.trimIndent())
                    }
                }
    }

    @ParameterizedTest
    @MethodSource("blockErrors")
    fun `should return error when block card failed`(
            debitCardError: DebitCardError, errorName: String, status: Int
    ) {
        // given
        given(debitCardFacade.blockCard(BlockCardCommand(debitCardId)))
                .willReturn(DebitCardOperationResult.failed(BlockCardCommand(debitCardId), debitCardError))

        // expect
        mockMvc.put("/v1/debit-cards/$debitCardId/block")
                .andDo { print() }
                .andExpect {
                    status { isEqualTo(status) }
                    content {
                        contentType("application/json")
                        json("""
                            {
                                "type": "$errorName",
                                "details": {
                                    "cardUUID": "$debitCardId"
                                }
                            }
                        """.trimIndent())
                    }
                }
    }

    @Test
    fun `should return 200 when card unblock successfull`() {
        // given
        given(debitCardFacade.unblockCard(UnblockCardCommand(debitCardId)))
                .willReturn(DebitCardOperationResult.success(UnblockCardCommand(debitCardId)))

        // expect
        mockMvc.put("/v1/debit-cards/$debitCardId/unblock")
                .andDo { print() }
                .andExpect {
                    status { isOk() }
                    content {
                        contentType("application/json")
                        json("""
                            {
                                   "cardUUID": "$debitCardId"
                            }
                        """.trimIndent())
                    }
                }
    }

    @Test
    fun `should return 404 error when card to unblock is not found`(
    ) {
        // given
        given(debitCardFacade.unblockCard(UnblockCardCommand(debitCardId)))
                .willReturn(DebitCardOperationResult.failed(UnblockCardCommand(debitCardId), CardNotFoundError()))

        // expect
        mockMvc.put("/v1/debit-cards/$debitCardId/unblock")
                .andDo { print() }
                .andExpect {
                    status { isNotFound() }
                    content {
                        contentType("application/json")
                        json("""
                            {
                                "type": "CardNotFoundError",
                                "details": {
                                    "cardUUID": "$debitCardId"
                                }
                            }
                        """.trimIndent())
                    }
                }
    }


    companion object {
        @JvmStatic
        fun cardSummaries(): Stream<Arguments> = Stream.of(
                arguments(
                        named("given summary with limit", DebitCardSummary(
                                debitCardId,
                                "10".bd,
                                Option.of("5".bd),
                                false
                        )),
                        named("expected summary with limit", """{
                            "cardUUID": "$debitCardId",
                            "balance": 10,
                            "limit": 5,
                            "blocked": false
                        }""".trimIndent())
                ),
                arguments(
                        named("given summary without limit", DebitCardSummary(
                                debitCardId,
                                "10".bd,
                                Option.none(),
                                false
                        )),
                        named("expected summary without amount", """{
                            "cardUUID": "$debitCardId",
                            "balance": 10,
                            "limit": null,
                            "blocked": false
                        }""".trimIndent())
                )
        )

        @JvmStatic
        fun cardAssignmentErrors() = Stream.of(
                arguments(LimitAlreadyAssigned(), "LimitAlreadyAssigned", 400),
                arguments(CardNotFoundError(), "CardNotFoundError", 404)
        )

        @JvmStatic
        fun chargeErrors() = Stream.of(
                arguments(CannotChargeError(), "CannotChargeError", 400),
                arguments(CardNotFoundError(), "CardNotFoundError", 404)
        )

        @JvmStatic
        fun payOffErrors() = Stream.of(
                arguments(CannotPayOffError(), "CannotPayOffError", 400),
                arguments(CardNotFoundError(), "CardNotFoundError", 404)
        )

        @JvmStatic
        fun blockErrors() = Stream.of(
                arguments(CannotBlockCardError(), "CannotBlockCardError", 400),
                arguments(CardNotFoundError(), "CardNotFoundError", 404)
        )
    }
}