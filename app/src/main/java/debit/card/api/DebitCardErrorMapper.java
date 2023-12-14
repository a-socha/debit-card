package debit.card.api;

import debit.card.domain.DebitCardError;
import debit.card.domain.commands.CardCommand;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;

import static debit.card.domain.DebitCardError.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

class DebitCardErrorMapper {
    static ResponseEntity<ErrorView> mapErrorToResultEntity(CardCommand command, DebitCardError error) {
        return switch (error) {
            case CardNotFoundError cardNotFoundError -> notFound(command, cardNotFoundError);
            default -> badRequest(command, error);
        };
    }


    private static ResponseEntity<ErrorView> notFound(CardCommand command, CardNotFoundError cardNotFoundError) {
        return new ResponseEntity<>(errorView(command, cardNotFoundError), NOT_FOUND);
    }

    private static ResponseEntity<ErrorView> badRequest(CardCommand command, DebitCardError debitCardError) {
        return new ResponseEntity<>(errorView(command, debitCardError), BAD_REQUEST);
    }

    @NotNull
    private static ErrorView errorView(CardCommand command, DebitCardError debitCardError) {
        return new ErrorView(debitCardError.getClass().getSimpleName(), command);
    }

}
