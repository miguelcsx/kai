package kai.engine

import kai.domain.campaign.CampaignConfig
import kai.domain.finding.Finding
import kai.domain.finding.OracleVerdict
import kai.plugin.reducer.InterestingnessPredicate
import kai.plugin.reducer.ReductionBudget
import kai.plugin.registry.PluginRegistry

class ReductionService(
    private val registry: PluginRegistry,
    private val verifier: FindingVerifier
) {
    fun forConfig(config: CampaignConfig): ConfiguredReductionService {
        return ConfiguredReductionService(config, registry, verifier)
    }
}

class ConfiguredReductionService(
    private val config: CampaignConfig,
    private val registry: PluginRegistry,
    private val verifier: FindingVerifier
) {
    fun reduce(verdict: OracleVerdict.Interesting): Finding {
        val reducerId = config.reducerIds.firstOrNull() ?: return Finding.create(verdict.finding)
        val predicate = predicate(verdict.finding)
        val reduced = registry.resolveReducer(reducerId.value)
            .reduce(verdict.finding.testCase, predicate, ReductionBudget(config.budget.maxReductionIterations))
            .reduced
        return Finding.create(verdict.finding, reduced.takeIf(predicate::isSatisfied))
    }

    private fun predicate(original: kai.domain.finding.PendingFinding): InterestingnessPredicate {
        return object : InterestingnessPredicate {
            override fun isSatisfied(candidate: kai.domain.testcase.TestCase): Boolean {
                if (candidate.charCount >= original.testCase.charCount) {
                    return false
                }
                return verifier.reproduces(config, candidate, original)
            }
        }
    }
}
