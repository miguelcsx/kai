package kai.plugin.executor

import kai.domain.execution.ExecutionConfig
import kai.domain.execution.ExecutorCapabilities
import kai.domain.execution.ExecutorProbeResult
import kai.domain.id.ExecutorId
import kai.domain.id.InterfaceVersion
import kai.domain.observations.ExecutionResult
import kai.domain.testcase.TestCase

interface ExecutorPlugin {
    val id: ExecutorId
    val version: InterfaceVersion
    val capabilities: ExecutorCapabilities

    fun execute(testCase: TestCase, config: ExecutionConfig): List<ExecutionResult>

    fun probe(): ExecutorProbeResult
}
