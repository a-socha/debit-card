package debit.card.domain;

import debit.card.domain.commands.CardCommand;
import io.vavr.collection.List;

public class DebitCardOperationResult<T extends CardCommand> {
    private final T cardCommand;
    private final List<DebitCardError> errors;

    private DebitCardOperationResult(T cardCommand, List<DebitCardError> errors) {
        this.cardCommand = cardCommand;
        this.errors = errors;
    }

    public boolean isSuccess() {
        return errors.isEmpty();
    }

    public List<DebitCardError> errors() {
        return errors;
    }

    static <T extends CardCommand> DebitCardOperationResult<T> success(T cardCommand) {
        return new DebitCardOperationResult<>(cardCommand, List.empty());
    }

    static <T extends CardCommand> DebitCardOperationResult<T> failed(T cardCommand, List<DebitCardError> errors) {
        return new DebitCardOperationResult<>(cardCommand, errors);
    }
    static <T extends CardCommand> DebitCardOperationResult<T> failed(T cardCommand, DebitCardError error) {
        return new DebitCardOperationResult<>(cardCommand, List.of(error));
    }
}

