package com.walkertribe.ian.world

import com.walkertribe.ian.enums.ObjectType

/**
 * Black holes
 * @author rjwut
 */
class ArtemisBlackHole(
    id: Int,
    timestamp: Long,
) : BaseArtemisObject<ArtemisBlackHole>(id, timestamp) {
    override val type: ObjectType = ObjectType.BLACK_HOLE

    object Dsl : BaseArtemisObject.Dsl<ArtemisBlackHole>(ArtemisBlackHole::class)
}
