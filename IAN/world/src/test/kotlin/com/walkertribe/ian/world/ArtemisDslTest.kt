package com.walkertribe.ian.world

import com.walkertribe.ian.util.shouldBeUnknown
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull

class ArtemisDslTest : DescribeSpec({
    describe("ArtemisCreature") {
        it("isNotTyphon") {
            ArtemisCreature.Dsl.isNotTyphon.shouldBeUnknown()
        }
    }

    describe("ArtemisNpc") {
        it("isEnemy") {
            ArtemisNpc.Dsl.isEnemy.shouldBeUnknown()
        }

        it("isSurrendered") {
            ArtemisNpc.Dsl.isSurrendered.shouldBeUnknown()
        }

        it("isInNebula") {
            ArtemisNpc.Dsl.isInNebula.shouldBeUnknown()
        }

        it("scanBits") {
            ArtemisNpc.Dsl.scanBits.shouldBeNull()
        }
    }

    describe("ArtemisPlayer") {
        describe("PlayerDsl") {
            it("shipIndex") {
                ArtemisPlayer.PlayerDsl.shipIndex shouldBeEqual Byte.MIN_VALUE
            }

            it("capitalShipID") {
                ArtemisPlayer.PlayerDsl.capitalShipID shouldBeEqual -1
            }

            it("alertStatus") {
                ArtemisPlayer.PlayerDsl.alertStatus.shouldBeNull()
            }

            it("dockingBase") {
                ArtemisPlayer.PlayerDsl.dockingBase shouldBeEqual -1
            }

            it("driveType") {
                ArtemisPlayer.PlayerDsl.driveType.shouldBeNull()
            }

            it("warp") {
                ArtemisPlayer.PlayerDsl.warp shouldBeEqual -1
            }
        }

        describe("UpgradesDsl") {
            it("doubleAgentActive") {
                ArtemisPlayer.UpgradesDsl.doubleAgentActive.shouldBeUnknown()
            }

            it("doubleAgentCount") {
                ArtemisPlayer.UpgradesDsl.doubleAgentCount shouldBeEqual -1
            }

            it("doubleAgentSecondsLeft") {
                ArtemisPlayer.UpgradesDsl.doubleAgentSecondsLeft shouldBeEqual -1
            }
        }
    }
})
