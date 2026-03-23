package kai.plugins.oracle.differential

import kai.domain.finding.BugSignature
import kai.domain.finding.FindingEvidence
import kai.domain.finding.FindingKind
import kai.domain.finding.OracleVerdict
import kai.domain.finding.PendingFinding
import kai.domain.id.InterfaceVersion
import kai.domain.id.OracleId
import kai.domain.observations.Observations
import kai.domain.testcase.TestCase
import kai.plugin.oracle.OraclePlugin

class DifferentialOraclePlugin : OraclePlugin {
    override val id = OracleId("k1k2-differential")
    override val version = InterfaceVersion.V1
    override val kind = FindingKind.DIFFERENTIAL

    override fun check(observations: Observations, testCase: TestCase): OracleVerdict {
        if (observations.results.size < 2) {
            return OracleVerdict.NotInteresting
        }
        val first = observations.results[0]
        val second = observations.results[1]
        if (!isDifferent(first, second)) {
            return OracleVerdict.NotInteresting
        }
        val evidenceText = difference(first, second)
        val signature = BugSignature.create(kind, first.profileName + ":" + second.profileName, listOf(evidenceText))
        val evidence = FindingEvidence.create(
            description = "Differential mismatch between compiler profiles",
            divergence = evidenceText
        )
        return OracleVerdict.Interesting(
            PendingFinding(id, kind, signature, observations, testCase, evidence)
        )
    }

    private fun isDifferent(
        first: kai.domain.observations.ExecutionResult,
        second: kai.domain.observations.ExecutionResult
    ): Boolean {
        return first.exitCode != second.exitCode || normalize(first.stderr) != normalize(second.stderr)
    }

    private fun difference(
        first: kai.domain.observations.ExecutionResult,
        second: kai.domain.observations.ExecutionResult
    ): String {
        return listOf(
            "left=${first.profileName}:${first.exitCode}:${normalize(first.stderr)}",
            "right=${second.profileName}:${second.exitCode}:${normalize(second.stderr)}"
        ).joinToString("\n")
    }

    private fun normalize(text: String): String {
        return text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("|")
    }
}
