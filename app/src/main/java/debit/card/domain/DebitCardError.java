package debit.card.domain;

public sealed interface DebitCardError permits
        DebitCardError.CardNotFoundError,
        DebitCardError.LimitAssignError,
        DebitCardError.CannotChargeError,
        DebitCardError.CannotBlockCardError,
        DebitCardError.CannotPayOffError {
    record CardNotFoundError() implements DebitCardError {
    }

    sealed interface LimitAssignError extends DebitCardError permits LimitAlreadyAssigned {
    }

    record LimitAlreadyAssigned() implements LimitAssignError {
    }

    record CannotChargeError() implements DebitCardError {
    }

    record CannotBlockCardError() implements DebitCardError {
    }

    record CannotPayOffError() implements DebitCardError {
    }

}
