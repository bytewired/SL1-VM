package dev

import bytecode.*
import utils.ByteArrayNavigator
import utils.INT_16_BITS_ARRAY_SIZE
import utils.INT_32_BITS_ARRAY_SIZE
import java.io.File
import java.io.FileInputStream

object OutputReader {
    fun read(path: String): BytecodeFile {
        val file = File(path)
        val inStream = FileInputStream(file)

        val buffer = ByteArray(file.length().toInt())
        inStream.read(buffer)

        val navigator = ByteArrayNavigator(buffer)

        val majorVersion = navigator.readNextBytesAsInt(INT_16_BITS_ARRAY_SIZE)
        val minorVersion = navigator.readNextBytesAsInt(INT_16_BITS_ARRAY_SIZE)
        val constantPoolSize = navigator.readNextBytesAsInt(INT_16_BITS_ARRAY_SIZE)

        // read constant pool
        val constantPool = mutableListOf<ConstantPoolEntry>()

        while (constantPool.size != constantPoolSize) {
            val type = ConstantPoolEntry.Type.values()[navigator.readNextBytesAsInt(1)]

            when (type) {
                ConstantPoolEntry.Type.INT -> {
                    constantPool.add(IntEntry.of(navigator.readNextBytes(INT_32_BITS_ARRAY_SIZE)))
                }
                ConstantPoolEntry.Type.DOUBLE -> {
                    constantPool.add(DoubleEntry.of(navigator.readNextBytes(INT_32_BITS_ARRAY_SIZE)))
                }
                ConstantPoolEntry.Type.STRING -> {
                    val length = navigator.readNextByte().toInt()

                    constantPool.add(StringEntry.of(length, navigator.readNextBytes(length)))
                }
            }
        }

        val instruction = mutableListOf<Instruction>()

        while(!navigator.isEnd()) {
            val instrWord = navigator.readNextBytes(INT_32_BITS_ARRAY_SIZE)

            instruction.add(Instruction.of(instrWord))
        }

        return BytecodeFile(file.name, majorVersion, minorVersion, constantPool, instruction)
    }
}