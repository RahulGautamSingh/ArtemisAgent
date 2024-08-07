package artemis.agent.game.biomechs

data class BiomechSorter(
    val sortByClassFirst: Boolean = false,
    val sortByStatus: Boolean = false,
    val sortByClassSecond: Boolean = false,
    val sortByName: Boolean = false,
) : Comparator<BiomechEntry> by (
    listOfNotNull(
        CLASS_COMPARATOR.takeIf { sortByClassFirst },
        STATUS_COMPARATOR.takeIf { sortByStatus },
        CLASS_COMPARATOR.takeIf { sortByClassSecond },
        NAME_COMPARATOR.takeIf { sortByName },
    ).let { comparators ->
        Comparator { b1, b2 ->
            comparators.firstNotNullOfOrNull { comparator ->
                comparator.compare(b1, b2).takeIf { it != 0 }
            } ?: 0
        }
    }
) {
    private companion object {
        val CLASS_COMPARATOR: Comparator<BiomechEntry> = compareByDescending {
            it.biomech.hullId.value
        }

        val STATUS_COMPARATOR: Comparator<BiomechEntry> = compareByDescending { it }

        val NAME_COMPARATOR: Comparator<BiomechEntry> = compareBy { it.biomech.name.value ?: "" }
    }
}
