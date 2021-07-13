package com.walkertribe.ian.world

import com.walkertribe.ian.vesseldata.Vessel
import com.walkertribe.ian.vesseldata.VesselData
import kotlin.reflect.KClass

/**
 * Base implementation of a shielded world object.
 */
abstract class BaseArtemisShielded<T : BaseArtemisShielded<T>>(
    id: Int,
    timestamp: Long
) : BaseArtemisObject<T>(id, timestamp), ArtemisShielded<T> {
    override val hullId = Property.IntProperty(timestamp)
    override val shieldsFront = Property.FloatProperty(timestamp)
    override val shieldsFrontMax = Property.FloatProperty(timestamp)
    override val name = Property.ObjectProperty<String>(timestamp)

    override fun getVessel(vesselData: VesselData): Vessel? =
        if (hullId.hasValue) vesselData[hullId.value] else null

    override fun updates(other: T) {
        super.updates(other)

        name updates other.name
        hullId updates other.hullId
        shieldsFront updates other.shieldsFront
        shieldsFrontMax updates other.shieldsFrontMax
    }

    /**
     * Returns true if this object contains any data.
     */
    override val hasData: Boolean get() =
        super.hasData ||
            name.hasValue ||
            hullId.hasValue ||
            shieldsFront.hasValue ||
            shieldsFrontMax.hasValue

    open class Dsl<T : BaseArtemisShielded<T>>(
        objectClass: KClass<T>,
    ) : BaseArtemisObject.Dsl<T>(objectClass) {
        var name: String? = null
        var hullId: Int = -1
        var shieldsFront: Float = Float.NaN
        var shieldsFrontMax: Float = Float.NaN

        override fun updates(obj: T) {
            super.updates(obj)

            obj.name.value = name
            obj.hullId.value = hullId
            obj.shieldsFront.value = shieldsFront
            obj.shieldsFrontMax.value = shieldsFrontMax

            name = null
            hullId = -1
            shieldsFront = Float.NaN
            shieldsFrontMax = Float.NaN
        }
    }
}
