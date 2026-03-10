package com.trafficanalyzer.analysis

/**
 * FilterRule — Identifies which filter stage made a decision on a packet.
 *
 * Each rule corresponds to one stage in the [PacketFilterEngine] chain.
 * The decision is either PASS (packet survives this stage) or DROP (packet
 * is excluded with an attached reason label for the UI).
 */
enum class FilterRule(val label: String) {
    PROTOCOL_TCP   ("Protocol: TCP only"),
    DIRECTION_OUT  ("Direction: Outbound only"),
    TCP_SYN        ("TCP Flag: SYN only"),
    PORT_WEB       ("Port: 80 / 443 only"),
    LOCAL_DROP     ("Network: RFC-1918 dropped"),
    REPUTATION     ("Reputation: Score below threshold"),
    PASSED         ("PASS")
}

/**
 * FilterDecision — The result of applying the full filter chain to one packet.
 *
 * @param pass       true → packet is shown in filtered view.
 * @param rule       Which rule caused the DROP (or PASSED if pass=true).
 * @param reason     Human-readable label for the UI badge.
 */
data class FilterDecision(
    val pass:   Boolean,
    val rule:   FilterRule,
    val reason: String
) {
    companion object {
        fun pass()                         = FilterDecision(true,  FilterRule.PASSED,       "PASS")
        fun drop(rule: FilterRule)         = FilterDecision(false, rule, rule.label)
    }
}
