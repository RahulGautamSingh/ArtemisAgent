package com.walkertribe.ian.protocol

import com.walkertribe.ian.iface.ListenerArgument
import com.walkertribe.ian.iface.ListenerModule
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.util.JamCrc

/**
 * Interface for all packets that can be received or sent.
 */
sealed interface Packet {
    abstract class Server(reader: PacketReader) : Packet, ListenerArgument {
        final override val timestamp: Long = reader.packetTimestamp

        override fun offerTo(module: ListenerModule) {
            module.onPacket(this)
        }
    }

    abstract class Client(
        /**
         * Returns the type value for this packet, specified as the last field of
         * the preamble.
         */
        val type: Int
    ) : Packet {
        constructor(type: String) : this(getHash(type))

        fun writeTo(writer: PacketWriter) {
            writer.start(type)
            writePayload(writer)
        }

        /**
         * Causes the packet's payload to be written to the given PacketWriter.
         */
        protected abstract fun writePayload(writer: PacketWriter)

        private companion object {
            private val TYPE_HASH_MAP = mutableMapOf<String, Int>()

            private fun getHash(type: String): Int {
                return TYPE_HASH_MAP[type] ?: JamCrc.compute(type).also {
                    TYPE_HASH_MAP[type] = it
                }
            }
        }
    }

    /**
     * Any packet that IAN has not parsed. This may be because it was not recognized
     * by any registered protocol, or because there are no registered packet
     * listeners that are interested in it.
     * @author rjwut
     */
    sealed class Raw(
        val type: Int,

        /**
         * Returns the payload for this packet.
         */
        val payload: ByteArray
    ) : Packet {
        /**
         * Any packet received that isn't of a type recognized by a registered protocol
         * will be returned as this class. In most cases, you won't be interested in
         * these: they're mainly intended for reverse-engineering of the protocol and
         * debugging.
         */
        class Unknown(type: Int, payload: ByteArray) : Raw(type, payload)

        /**
         * Any packet received for which no packet listeners have been registered will
         * be returned as this class.
         */
        class Unparsed(type: Int, payload: ByteArray) : Raw(type, payload)
    }

    companion object {
        /**
         * The preamble of every packet starts with this value.
         */
        const val HEADER = 0xDEADBEEF.toInt()
        const val PREAMBLE_SIZE = Int.SIZE_BYTES * 6
    }
}
