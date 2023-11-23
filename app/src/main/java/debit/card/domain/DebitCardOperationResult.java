package debit.card.domain;

import debit.card.domain.commands.CardCommand;
import io.vavr.Function2;
import io.vavr.collection.List;
import io.vavr.control.Option;

import java.util.function.Function;
import java.util.function.Supplier;

public class DebitCardOperationResult<T extends CardCommand> {
    private final T cardCommand;
    private final Option<DebitCardError> error;

    private DebitCardOperationResult(T cardCommand, Option<DebitCardError> error) {
        this.cardCommand = cardCommand;
        this.error = error;
    }

    public boolean isSuccess() {
        return error.isEmpty();
    }

    public DebitCardError error() {
        return error.get();
    }

    public <U> U fold(Function2<T, DebitCardError, U> onError, Function<T, U> onSuccess) {
        return error.map(error -> onError.apply(cardCommand, error))
                .getOrElse(() -> onSuccess.apply(cardCommand));
    }

    public static <T extends CardCommand> DebitCardOperationResult<T> success(T cardCommand) {
        return new DebitCardOperationResult<>(cardCommand, Option.none());
    }

    public static <T extends CardCommand> DebitCardOperationResult<T> failed(T cardCommand, DebitCardError error) {
        return new DebitCardOperationResult<>(cardCommand, Option.some(error));
    }
}

