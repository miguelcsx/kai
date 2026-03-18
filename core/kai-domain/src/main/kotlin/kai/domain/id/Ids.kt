package kai.domain.id

data class CampaignId(val value: String)
data class TestCaseId(val value: String)
data class StrategyId(val value: String)
data class ExecutorId(val value: String)
data class OracleId(val value: String)
data class ReducerId(val value: String)
data class FindingId(val value: String)

data class InterfaceVersion(val major: Int, val minor: Int) {
    fun isCompatibleWith(expected: InterfaceVersion): Boolean {
        return major == expected.major
    }

    override fun toString(): String {
        return "$major.$minor"
    }

    companion object {
        val V1 = InterfaceVersion(1, 0)
    }
}
