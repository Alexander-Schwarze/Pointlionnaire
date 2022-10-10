import kotlinx.serialization.Serializable
import java.io.OutputStream

class MultiOutputStream(private vararg val streams: OutputStream) : OutputStream() {
    override fun close() = streams.forEach(OutputStream::close)
    override fun flush() = streams.forEach(OutputStream::flush)

    override fun write(b: Int) = streams.forEach {
        it.write(b)
    }

    override fun write(b: ByteArray) = streams.forEach {
        it.write(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) = streams.forEach {
        it.write(b, off, len)
    }
}

@Serializable
data class Question(
    val questionText: String,
    val answer: String,
    val isLast2Questions: Boolean,
    val isTieBreakerQuestion: Boolean
)

fun String.pluralForm(number: Int) = when {
    number == 1 -> this
    endsWith("y") -> "${substringBeforeLast("y")}ies"
    else -> "${this}s"
}