@file:Suppress("UNCHECKED_CAST", "unused")

package app.bunq4ynab.domain.model

/**
 * Models success/failure of operations.
 * The idea is to provide higher abstraction of operation that can be ended with result
 * either success or failure. Result.Success represents value in case of success, and
 * Result.Failure represents error in case of failure which is upper bounded with
 * Exception type.
 *
 * Usable samples:
 *
 * Result.of(operation)
 *   .flatMap { normalizedData(it) }
 *   .map { createRequestFromData(it) }
 *   .flatMap { database.updateFromRequest(it) }
 *
 * //multi-declaration
 * val (value, error) = result
 * //get
 * val value: Int = result.get<Int>() ?: 0
 * val ex: Exception = result.get<Exception>()!!
 * //success
 * result.success { }
 * //failure
 * result.failure { }
 * //fold is there, if you want to handle both success and failure
 * result.fold({ value ->
 *   //do something with value
 * }, { error ->
 *   //do something with error
 * })
 *
 * Ref: https://github.com/kittinunf/Result
 */
sealed class Result<out V : Any?, out E : Exception> {

    abstract operator fun component1(): V?
    abstract operator fun component2(): E?

    suspend fun <X> fold(success: suspend (V) -> X, failure: suspend (E) -> X): X {
        return when (this) {
            is Success -> success(this.value)
            is Failure -> failure(this.error)
        }
    }

    abstract fun get(): V

    class Success<out V : Any?, out E : Exception>(val value: V) : Result<V, E>() {
        override fun component1(): V? = value
        override fun component2(): E? = null

        override fun get(): V = value

        override fun toString() = "[Success: $value]"

        override fun hashCode(): Int = value.hashCode()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Success<*, *> && value == other.value
        }
    }

    class Failure<out V : Any?, out E : Exception>(val error: E) : Result<V, E>() {
        override fun component1(): V? = null
        override fun component2(): E? = error

        override fun get(): V = throw error

        fun getException(): E = error

        override fun toString() = "[Failure: $error]"

        override fun hashCode(): Int = error.hashCode()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Failure<*, *> && error == other.error
        }
    }

    companion object {
        // Factory methods
        fun <V : Any> success(value: V) = Success<V, Nothing>(value)

        fun <E : Exception> error(ex: E) = Failure<Nothing, E>(ex)

        fun <V : Any?> of(
            value: V?,
            fail: (() -> Exception) = { Exception() }
        ): Result<V, Exception> {
            return value?.let { Success<V, Nothing>(it) } ?: error(fail())
        }

        suspend fun <V : Any?, E : Exception> of(f: suspend () -> V): Result<V, E> = try {
            Success(f())
        } catch (ex: Exception) {
            Failure(ex as E)
        }
    }
}

inline fun <reified X> Result<*, *>.getAs() = when (this) {
    is Result.Success -> value as? X
    is Result.Failure -> error as? X
}

suspend fun <V : Any?> Result<V, *>.success(f: suspend (V) -> Unit) = fold(f, {})

suspend fun <E : Exception> Result<*, E>.failure(f: suspend (E) -> Unit) = fold({}, f)

infix fun <V : Any?, E : Exception> Result<V, E>.or(fallback: V) = when (this) {
    is Result.Success -> this
    else -> Result.Success(fallback)
}

inline infix fun <V : Any?, E : Exception> Result<V, E>.getOrElse(fallback: (E) -> V): V {
    return when (this) {
        is Result.Success -> value
        is Result.Failure -> fallback(error)
    }
}

fun <V : Any?, E : Exception> Result<V, E>.getOrNull(): V? {
    return when (this) {
        is Result.Success -> value
        is Result.Failure -> null
    }
}

suspend fun <V : Any?, U : Any?, E : Exception> Result<V, E>.map(transform: suspend (V) -> U): Result<U, E> =
    try {
        when (this) {
            is Result.Success -> Result.Success(transform(value))
            is Result.Failure -> Result.Failure(error)
        }
    } catch (ex: Exception) {
        Result.error(ex as E)
    }

suspend fun <V : Any?, U : Any?, E : Exception> Result<V, E>.flatMap(transform: suspend (V) -> Result<U, E>): Result<U, E> =
    try {
        when (this) {
            is Result.Success -> transform(value)
            is Result.Failure -> Result.Failure(error)
        }
    } catch (ex: Exception) {
        Result.error(ex as E)
    }

suspend fun <V : Any?, E : Exception, E2 : Exception> Result<V, E>.mapError(transform: suspend (E) -> E2): Result<V, E2> {
    return when (this) {
        is Result.Success -> Result.Success(value)
        is Result.Failure -> Result.Failure(transform(error))
    }
}

suspend fun <V : Any?, E : Exception, E2 : Exception> Result<V, E>.flatMapError(transform: suspend (E) -> Result<V, E2>) =
    when (this) {
        is Result.Success -> Result.Success(value)
        is Result.Failure -> transform(error)
    }

suspend fun <V : Any?, E : Exception> Result<V, E>.any(predicate: suspend (V) -> Boolean): Boolean =
    try {
        when (this) {
            is Result.Success -> predicate(value)
            is Result.Failure -> false
        }
    } catch (ex: Exception) {
        false
    }

suspend fun <V : Any?, U : Any> Result<V, *>.fanout(other: suspend () -> Result<U, *>): Result<Pair<V, U>, *> =
    flatMap { outer -> other().map { outer to it } }

suspend fun <V : Any?, E : Exception> List<Result<V, E>>.lift(): Result<List<V>, E> =
    fold(Result.Success<MutableList<V>, E>(mutableListOf()) as Result<MutableList<V>, E>) { acc, result ->
        acc.flatMap { combine ->
            result.map { combine.apply { add(it) } }
        }
    }
