package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.enums.DriveType
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.vesseldata.Vessel
import com.walkertribe.ian.vesseldata.VesselData
import com.walkertribe.ian.vesseldata.VesselDataObject

/**
 * Represents a ship in [AllShipSettingsPacket].
 * @author rjwut
 */
class Ship internal constructor(
    /**
     * The name of the ship
     */
    val name: String?,

    /**
     * The hullId for this ship
     */
    val shipType: Int,

    /**
     * What drive type the ship is using.
     */
    val drive: DriveType,

    /**
     * Returns the accent color for the ship.
     * Unspecified: NaN
     */
    val accentColor: Float = Float.NaN,
) : VesselDataObject {
    /**
     * Returns the accent color for the ship as a Hue value (between 0 and 360).
     * Unspecified: NaN
     */
    val hue: Float get() = accentColor * HUE_RANGE

    init {
        if (!accentColor.isNaN()) {
            require(accentColor in 0f..1f) { "Accent color must be in range [0.0,1.0]" }
        }
    }

    /**
     * Returns the Vessel identified by the ship's hull ID, or null if no such Vessel can be found.
     */
    override fun getVessel(vesselData: VesselData): Vessel? = vesselData[shipType]

    override fun hashCode(): Int = arrayOf(name, shipType, drive, accentColor).contentHashCode()

    override fun equals(other: Any?): Boolean =
        other is Ship &&
            other.name == name &&
            matchesAccentColor(other.accentColor) &&
            other.drive == drive &&
            other.shipType == shipType

    override fun toString(): String = "$name: (type #$shipType)${
        if (accentColor.isNaN()) "" else " color=$hue"
    }"

    private fun matchesAccentColor(otherAccentColor: Float): Boolean =
        if (otherAccentColor.isNaN()) {
            accentColor.isNaN()
        } else {
            (accentColor - otherAccentColor) in -ACCENT_EPSILON..ACCENT_EPSILON
        }

    companion object {
        internal const val HUE_RANGE = 360f
        private const val ACCENT_EPSILON = 0.00000001f

        /**
         * Reads a Ship from this PacketReader. Don't use the public Ship constructor
         * in this scenario.
         */
        fun PacketReader.readShip(): Ship {
            val drive = readIntAsEnum<DriveType>()
            val hullId = readInt()
            val accentColor = if (version >= Version.ACCENT_COLOR) readFloat() else Float.NaN
            val hasName = readBool(Int.SIZE_BYTES)
            val name = if (hasName.booleanValue) readString() else null
            return Ship(name, hullId, drive, accentColor)
        }
    }
}
