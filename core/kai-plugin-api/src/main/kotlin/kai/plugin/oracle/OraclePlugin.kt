package kai.plugin.oracle

import kai.domain.finding.FindingKind
import kai.domain.finding.OracleVerdict
import kai.domain.id.InterfaceVersion
import kai.domain.id.OracleId
import kai.domain.observations.Observations
import kai.domain.testcase.TestCase

interface OraclePlugin {
    val id: OracleId
    val version: InterfaceVersion
    val kind: FindingKind

    fun check(observations: Observations, testCase: TestCase): OracleVerdict
}
