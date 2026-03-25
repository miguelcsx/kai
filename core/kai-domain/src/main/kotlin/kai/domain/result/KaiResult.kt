package kai.domain.result

sealed interface KaiResult<out T> {
    data class Success<T>(val value: T) : KaiResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : KaiResult<Nothing>
}

inline fun <T> kaiResult(block: () -> T): KaiResult<T> {
    return try {
        KaiResult.Success(block())
    } catch (error: Throwable) {
        KaiResult.Failure(error.message ?: error::class.simpleName.orEmpty(), error)
    }
}

fun <T> KaiResult<T>.getOrThrow(): T {
    return when (this) {
        is KaiResult.Success -> value
        is KaiResult.Failure -> throw IllegalStateException(message, cause)
    }
}
