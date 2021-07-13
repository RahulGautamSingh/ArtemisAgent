package artemis.agent.game.allies

import artemis.agent.game.ObjectEntry

data class AllySorter(
    val sortByClassFirst: Boolean = false,
    val sortByEnergy: Boolean = false,
    val sortByStatus: Boolean = false,
    val sortByClassSecond: Boolean = false,
    val sortByName: Boolean = false,
) : Comparator<ObjectEntry.Ally> by (
    listOfNotNull(
        CLASS_COMPARATOR.takeIf { sortByClassFirst },
        ENERGY_COMPARATOR.takeIf { sortByEnergy },
        STATUS_COMPARATOR.takeIf { sortByStatus },
        CLASS_COMPARATOR.takeIf { sortByClassSecond },
        NAME_COMPARATOR.takeIf { sortByName },
    ).let { comparators ->
        Comparator { ally1, ally2 ->
            comparators.firstNotNullOfOrNull { comparator ->
                comparator.compare(ally1, ally2).takeIf { it != 0 }
            } ?: 0
        }
    }
) {
    private companion object {
        val CLASS_COMPARATOR: Comparator<ObjectEntry.Ally> = compareByDescending {
            it.obj.hullId.value
        }

        val ENERGY_COMPARATOR: Comparator<ObjectEntry.Ally> = compareByDescending {
            if (it.hasEnergy) 1 else 0
        }

        val STATUS_COMPARATOR: Comparator<ObjectEntry.Ally> = compareByDescending {
            it.status.sortIndex.ordinal
        }

        val NAME_COMPARATOR: Comparator<ObjectEntry.Ally> = compareBy { it.obj.name.value ?: "" }
    }
}
