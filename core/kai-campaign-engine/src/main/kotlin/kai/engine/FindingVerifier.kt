package kai.engine

import kai.domain.campaign.CampaignConfig
import kai.domain.finding.OracleVerdict
import kai.domain.finding.PendingFinding
import kai.domain.observations.Observations
import kai.domain.result.getOrThrow
import kai.domain.testcase.TestCase
import kai.plugin.registry.PluginRegistry

interface FindingVerifier {
    fun reproduces(config: CampaignConfig, candidate: TestCase, original: PendingFinding): Boolean
}

class OracleFindingVerifier(
    private val registry: PluginRegistry,
    private val preparationService: TestCasePreparationService = TestCasePreparationService()
) : FindingVerifier {
    override fun reproduces(config: CampaignConfig, candidate: TestCase, original: PendingFinding): Boolean {
        return runCatching {
            val observations = execute(config, candidate)
            val oracle = registry.resolveOracle(original.oracleId.value)
            sameFinding(original, oracle.check(observations, candidate))
        }.getOrDefault(false)
    }

    private fun execute(config: CampaignConfig, candidate: TestCase): Observations {
        val executor = registry.resolveExecutor(config.executorId.value)
        val prepared = preparationService.prepare(candidate, executor).getOrThrow()
        val results = executor.execute(prepared, config.executionConfig)
        return Observations.create(prepared.id, results)
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
