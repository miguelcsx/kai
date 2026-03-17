package kai.engine

import kai.domain.campaign.CampaignConfig
import kai.domain.finding.OracleVerdict
import kai.domain.finding.PendingFinding
import kai.domain.testcase.TestCase
import kai.domain.observations.Observations
import kai.plugin.registry.PluginRegistry

interface FindingVerifier {
    fun reproduces(candidate: TestCase, original: PendingFinding): Boolean
}

class OracleFindingVerifier(
    private val config: CampaignConfig,
    private val registry: PluginRegistry
) : FindingVerifier {
    override fun reproduces(candidate: TestCase, original: PendingFinding): Boolean {
        return runCatching {
            val observations = execute(candidate)
            val oracle = registry.resolveOracle(original.oracleId.value)
            sameFinding(original, oracle.check(observations, candidate))
        }.getOrDefault(false)
    }

    private fun execute(candidate: TestCase): Observations {
        val executor = registry.resolveExecutor(config.executorId.value)
        val results = executor.execute(candidate, config.executionConfig)
        return Observations.create(candidate.id, results)
    }

    private fun sameFinding(
        original: PendingFinding,
        verdict: OracleVerdict
    ): Boolean {
        if (verdict !is OracleVerdict.Interesting) {
            return false
        }
        return verdict.finding.kind == original.kind &&
            verdict.finding.signature.hash == original.signature.hash
    }
}
