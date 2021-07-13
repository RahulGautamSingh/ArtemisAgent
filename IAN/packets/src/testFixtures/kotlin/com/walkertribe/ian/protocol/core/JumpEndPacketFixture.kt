package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.util.version
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.map
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeIntLittleEndian

class JumpEndPacketFixture(
    arbVersion: Arb<Version> = Arb.version(),
) : PacketTestFixture.Server<JumpEndPacket>(
    TestPacketTypes.SIMPLE_EVENT,
) {
    class Data internal constructor(
        override val version: Version,
    ): PacketTestData.Server<JumpEndPacket> {
        override fun buildPayload(): ByteReadPacket = buildPacket {
            writeIntLittleEndian(SimpleEventPacket.Subtype.JUMP_END.toInt())
        }

        override fun validate(packet: JumpEndPacket) {
            // Nothing to validate
        }
    }

    override val generator: Gen<Data> = arbVersion.map(::Data)

    override suspend fun testType(packet: Packet.Server): JumpEndPacket =
        packet.shouldBeInstanceOf()
}
