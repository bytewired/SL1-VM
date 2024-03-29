package x1.hypervisor.api

import x1.hypervisor.api.utils.toByteArray
import x1.hypervisor.api.utils.toInt
import java.io.InputStream
import java.io.OutputStream

/**
 * Command layout
 * +--------------------------+
 * | 4u size | 1u type | data |
 * +--------------------------+
 */

abstract class Command(val type: Type) {
    internal abstract fun toByteArray(): ByteArray

    override fun toString(): String {
        return "Command(type=$type)"
    }

    enum class Type {
        REQUEST_SYNC,
        SYNC_DATA
    }
}

class RequestSyncCommand : Command(Type.REQUEST_SYNC) {
    override fun toByteArray(): ByteArray = byteArrayOf(type.ordinal.toByte())

}

class SyncDataResponse(val data: ByteArray) : Command(Type.SYNC_DATA) {
    override fun toByteArray(): ByteArray = byteArrayOf(type.ordinal.toByte()) + data
}

class Reader {
    private val buffer = ByteArray(1024)
    private var dataArray = ByteArray(0)
    private var reset = true
    private var consumedBytes = 0

    // returns true if finished
    fun read(inputStream: InputStream): Boolean {
        val read = inputStream.read(buffer)

        if (reset) {
            reset = false

            val length = buffer.copyOfRange(0, 4).toInt()

            dataArray = ByteArray(length)

            buffer.copyInto(dataArray, 0, 4, read)

            consumedBytes = read - 4
        } else {
            buffer.copyInto(dataArray, destinationOffset = consumedBytes, 0, read)

            consumedBytes += read
        }

        return consumedBytes == dataArray.size
    }

    fun parse(): Command {
        return when (Command.Type.values()[dataArray[0].toInt()]) {
            Command.Type.REQUEST_SYNC -> {
                RequestSyncCommand()
            }
            Command.Type.SYNC_DATA -> {
                SyncDataResponse(dataArray.copyOfRange(1, dataArray.size))
            }
        }.apply {
            reset()
        }
    }

    private fun reset() {
        dataArray = ByteArray(0)
        consumedBytes = 0
        reset = true
    }
}

class Writer(private val outputStream: OutputStream) {
    fun write(command: Command) {
        val dataByteArray = command.toByteArray()
        val sizeByteArray = dataByteArray.size.toByteArray()
        outputStream.write(sizeByteArray + dataByteArray)
        outputStream.flush()
    }
}