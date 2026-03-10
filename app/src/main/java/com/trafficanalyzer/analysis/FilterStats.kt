package com.trafficanalyzer.analysis

/**
 * FilterStats — Live counters updated every time a packet passes through the engine.
 *
 * Immutable snapshot — the ViewModel replaces the whole object on each update
 * so Compose can diff it as a single state change.
 *
 * @param totalCaptured  Raw packets received from the TUN interface.
 * @param totalKept      Packets that passed all active filter stages.
 * @param totalDropped   Packets that were dropped by at least one stage.
 * @param dropsByRule    Per-rule drop counters for the breakdown chip row.
 */
data class FilterStats(
    val totalCaptured: Int                      = 0,
    val totalKept:     Int                      = 0,
    val totalDropped:  Int                      = 0,
    val dropsByRule:   Map<FilterRule, Int>     = emptyMap()
) {
    val dropPercent: Int
        get() = if (totalCaptured == 0) 0
                else (totalDropped * 100) / totalCaptured

    fun incrementDrop(rule: FilterRule): FilterStats {
        val newMap = dropsByRule.toMutableMap()
        newMap[rule] = (newMap[rule] ?: 0) + 1
        return copy(
            totalCaptured = totalCaptured + 1,
            totalDropped  = totalDropped  + 1,
            dropsByRule   = newMap
        )
    }

    fun incrementPass(): FilterStats = copy(
        totalCaptured = totalCaptured + 1,
        totalKept     = totalKept     + 1
    )
}
