package debit.card

internal val String.bd
    get() = this.toBigDecimal()