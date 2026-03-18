package kai.domain.finding

import java.security.MessageDigest
import kai.domain.id.FindingId
import kai.domain.id.OracleId
import kai.domain.observations.Observations
import kai.domain.testcase.TestCase

enum class FindingKind {
    CRASH,
    DIFFERENTIAL
}

data class FindingEvidence(
    val description: String,
    val stackTrace: String?,
    val divergence: String?
) {
    companion object {
        fun create(
            description: String,
            stackTrace: String? = null,
            divergence: String? = null
        ): FindingEvidence {
            return FindingEvidence(description, stackTrace, divergence)
        }
    }
}

data class BugSignature(
    val hash: String,
    val kind: FindingKind,
    val profileName: String,
    val normalizedFrames: List<String>
) {
    companion object {
        fun create(
            kind: FindingKind,
            profileName: String,
            frames: List<String>
        ): BugSignature {
            val normalized = frames.filter { it.isNotBlank() }.take(5)
            return BugSignature(hash(kind, profileName, normalized), kind, profileName, normalized)
        }

        private fun hash(
            kind: FindingKind,
            profileName: String,
            frames: List<String>
        ): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val payload = listOf(kind.name, profileName, frames.joinToString("|")).joinToString("|")
            return digest.digest(payload.toByteArray())
                .joinToString("") { "%02x".format(it) }
                .take(16)
        }
    }
}

data class PendingFinding(
    val oracleId: OracleId,
    val kind: FindingKind,
    val signature: BugSignature,
    val observations: Observations,
    val testCase: TestCase,
    val evidence: FindingEvidence
)

sealed class OracleVerdict {
    data class Interesting(val finding: PendingFinding) : OracleVerdict()
    object NotInteresting : OracleVerdict()
}

enum class FindingStatus {
    NEW,
    REDUCED
}

data class Finding(
    val id: FindingId,
    val pending: PendingFinding,
    val reduced: TestCase?,
    val status: FindingStatus
) {
    companion object {
        fun create(
            pending: PendingFinding,
            reduced: TestCase? = null
        ): Finding {
            val status = if (reduced == null) FindingStatus.NEW else FindingStatus.REDUCED
            return Finding(FindingId(pending.signature.hash), pending, reduced, status)
        }
    }
}
