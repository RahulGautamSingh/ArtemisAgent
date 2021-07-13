package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.enums.DriveType
import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.protocol.core.SimpleEventPacket
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.world.Artemis
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.negativeFloat
import io.kotest.property.arbitrary.numericFloat
import io.kotest.property.arbitrary.string
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFloatLittleEndian
import io.ktor.utils.io.core.writeIntLittleEndian

class AllShipSettingsPacketTest : PacketTestSpec.Server<AllShipSettingsPacket>(
    specName = "AllShipSettingsPacket",
    fixtures = AllShipSettingsPacketFixture.ALL,
    failures = listOf(
        "low" to Arb.negativeFloat(),
        "high" to Arb.numericFloat(min = 1.001f),
    ).map { (condition, arbAccentColor) ->
        object : Failure(TestPacketTypes.SIMPLE_EVENT, "Accent color too $condition") {
            override val payloadGen: Gen<ByteReadPacket> = Arb.list(
                Arb.bind(
                    Arb.int(),
                    Arb.string(),
                    Arb.int(),
                    arbAccentColor,
                    Arb.enum<DriveType>(),
                ) { hasName, name, shipType, accentColor, drive ->
                    Pair(
                        hasName to name.takeIf { hasName != 0 },
                        Triple(shipType, drive, accentColor)
                    )
                },
                Artemis.SHIP_COUNT..Artemis.SHIP_COUNT,
            ).map { shipList ->
                buildPacket {
                    writeIntLittleEndian(SimpleEventPacket.Subtype.SHIP_SETTINGS.toInt())
                    shipList.forEach { (nameData, shipData) ->
                        writeIntLittleEndian(shipData.second.ordinal)
                        writeIntLittleEndian(shipData.first)
                        writeFloatLittleEndian(shipData.third)
                        writeIntLittleEndian(nameData.first)
                        if (nameData.first != 0) {
                            writeString(nameData.second.shouldNotBeNull())
                        }
                    }
                }
            }
        }
    },
)
