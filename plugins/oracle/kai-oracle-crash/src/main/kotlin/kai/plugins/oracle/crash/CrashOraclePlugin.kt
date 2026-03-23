package kai.plugins.oracle.crash

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

class CrashOraclePlugin : OraclePlugin {
    override val id = OracleId("crash-ice")
    override val version = InterfaceVersion.V1
    override val kind = FindingKind.CRASH

    override fun check(observations: Observations, testCase: TestCase): OracleVerdict {
        val result = observations.results.firstOrNull(::isInterestingResult) ?: return OracleVerdict.NotInteresting
        val frames = normalizedFrames(result.stderr)
        val signature = BugSignature.create(kind, result.profileName, frames)
        val evidence = FindingEvidence.create(
            description = "Compiler crash or internal error detected for profile ${result.profileName}",
            stackTrace = result.stderr
        )
        return OracleVerdict.Interesting(
            PendingFinding(id, kind, signature, observations, testCase, evidence)
        )
    }

    private fun isInterestingResult(result: kai.domain.observations.ExecutionResult): Boolean {
        if (result.exitCode == 0 || result.exitCode == 1) {
            return containsIceMarker(result.stderr)
        }
        return containsIceMarker(result.stderr) || result.exitCode > 1
    }

    private fun containsIceMarker(stderr: String): Boolean {
        val text = stderr.toLowerCase()
        return listOf(
            "internal error",
            "exception during",
            "illegalstateexception",
            "assertionerror",
            "kotlinnullpointerexception"
        ).any { text.contains(it) }
    }

    private fun normalizedFrames(stderr: String): List<String> {
        return stderr.lines()
            .map { it.trim() }
            .filter { it.startsWith("at ") }
            .map { it.substringBefore("(") }
            .take(5)
    }
}
