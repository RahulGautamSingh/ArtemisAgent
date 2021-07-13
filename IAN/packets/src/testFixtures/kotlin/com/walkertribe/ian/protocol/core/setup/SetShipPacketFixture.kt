package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.protocol.core.PacketTestData
import com.walkertribe.ian.protocol.core.PacketTestFixture
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.protocol.core.ValueIntPacket
import com.walkertribe.ian.world.Artemis
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.exhaustive.of
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readIntLittleEndian

class SetShipPacketFixture private constructor(
    shipIndex: Int,
) : PacketTestFixture.Client<SetShipPacket>(
    packetType = TestPacketTypes.VALUE_INT,
    expectedPayloadSize = Int.SIZE_BYTES * 2,
) {
    class Data internal constructor(
        private val shipIndex: Int,
    ) : PacketTestData.Client<SetShipPacket>(SetShipPacket(shipIndex)) {
        init {
            packet.shipIndex shouldBeEqual shipIndex
        }

        override fun validatePayload(payload: ByteReadPacket) {
            payload.readIntLittleEndian() shouldBeEqual ValueIntPacket.Subtype.SET_SHIP.toInt()
            payload.readIntLittleEndian() shouldBeEqual shipIndex
        }
    }

    override val generator: Gen<Data> = Exhaustive.of(Data(shipIndex))
    override val specName: String = "Ship index: $shipIndex"

    companion object {
        val ALL = List(Artemis.SHIP_COUNT, ::SetShipPacketFixture)
    }
}
