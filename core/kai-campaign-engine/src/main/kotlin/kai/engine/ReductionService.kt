package kai.engine

import kai.domain.campaign.CampaignConfig
import kai.domain.finding.Finding
import kai.domain.finding.OracleVerdict
import kai.plugin.reducer.InterestingnessPredicate
import kai.plugin.reducer.ReductionBudget
import kai.plugin.registry.PluginRegistry

class ReductionService(
    private val config: CampaignConfig,
    private val registry: PluginRegistry,
    private val verifier: FindingVerifier
) {
    fun reduce(verdict: OracleVerdict.Interesting): Finding {
        val reducerId = config.reducerIds.firstOrNull() ?: return Finding.create(verdict.finding)
        val reducer = registry.resolveReducer(reducerId.value)
        val predicate = predicate(verdict.finding)
        val reduced = reducer.reduce(verdict.finding.testCase, predicate, ReductionBudget(16)).reduced
        val accepted = if (predicate.isSatisfied(reduced)) reduced else null
        return Finding.create(verdict.finding, accepted)
    }

    private fun predicate(original: kai.domain.finding.PendingFinding): InterestingnessPredicate {
        return object : InterestingnessPredicate {
            override fun isSatisfied(candidate: kai.domain.testcase.TestCase): Boolean {
                if (candidate.charCount >= original.testCase.charCount) {
                    return false
                }
                return verifier.reproduces(candidate, original)
            }
        }
    }
}
