package debit.card.api;

import debit.card.domain.DebitCardFacade;
import debit.card.domain.commands.AssignLimitCommand;
import debit.card.domain.commands.BlockCardCommand;
import debit.card.domain.commands.ChargeCardCommand;
import debit.card.domain.commands.PayOffCardCommand;
import io.vavr.collection.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/v1/debit-cards")
class DebitCardController {
    private static final Logger log = LoggerFactory.getLogger(DebitCardController.class);
    private final DebitCardFacade debitCardFacade;

    DebitCardController(DebitCardFacade debitCardFacade) {
        this.debitCardFacade = debitCardFacade;
    }

    @PostMapping
    ResponseEntity<DebitCardCreationResponse> createCard() {
        log.info("Card creation request");
        return ResponseEntity.ok(new DebitCardCreationResponse(debitCardFacade.createNewCard()));
    }

    @GetMapping("/{debitCardUUID}")
    ResponseEntity<?> getCardSummary(@PathVariable(name = "debitCardUUID") UUID debitCardUUID) {
        return debitCardFacade.getSummary(debitCardUUID).fold(
                () -> debitCardNotFound(debitCardUUID),
                ResponseEntity::ok
        );
    }

    @PutMapping("/{debitCardUUID}/limit")
    ResponseEntity<?> assignLimitToDebitCard(
            @PathVariable(name = "debitCardUUID") UUID debitCardUUID,
            @RequestBody AssignLimitRequest assignLimitRequest
    ) {
        return debitCardFacade.assignLimitToCard(new AssignLimitCommand(debitCardUUID, assignLimitRequest.limit()))
                .fold(
                        DebitCardErrorMapper::mapErrorToResultEntity,
                        ResponseEntity::ok
                );
    }


    @PutMapping("/{debitCardUUID}/charge")
    ResponseEntity<?> chargeCard(@PathVariable(name = "debitCardUUID") UUID debitCardUUID,
                                 @RequestBody ChargeCardRequest request) {
        return debitCardFacade.chargeCard(new ChargeCardCommand(debitCardUUID, request.transactionUUID(), request.amount()))
                .fold(
                        DebitCardErrorMapper::mapErrorToResultEntity,
                        ResponseEntity::ok
                );
    }

    @PutMapping("/{debitCardUUID}/pay-off")
    ResponseEntity<?> pauOffCard(@PathVariable(name = "debitCardUUID") UUID debitCardUUID,
                                 @RequestBody PayOffRequest request) {
        return debitCardFacade.payOffCard(new PayOffCardCommand(debitCardUUID, request.transactionUUID(), request.amount())).fold(
                DebitCardErrorMapper::mapErrorToResultEntity,
                ResponseEntity::ok
        );
    }

    @PutMapping("/{debitCardUUID}/block")
    ResponseEntity<?> blockCard(@PathVariable(name = "debitCardUUID") UUID debitCardUUID) {
        return debitCardFacade.blockCard(new BlockCardCommand(debitCardUUID)).fold(
                DebitCardErrorMapper::mapErrorToResultEntity,
                ResponseEntity::ok
        );
    }

    private ResponseEntity<ErrorView> debitCardNotFound(UUID uuid) {
        return new ResponseEntity<>(new ErrorView(
                "CardNotFoundError",
                HashMap.of("debitCardUUID", uuid)
        ),
                NOT_FOUND
        );
    }

}