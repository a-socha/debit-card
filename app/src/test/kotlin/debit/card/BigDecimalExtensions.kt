package debit.card

val String.bd
    get() = this.toBigDecimal()