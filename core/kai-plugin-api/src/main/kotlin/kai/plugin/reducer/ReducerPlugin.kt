package kai.plugin.reducer

import kai.domain.id.InterfaceVersion
import kai.domain.id.ReducerId
import kai.domain.testcase.TestCase

interface InterestingnessPredicate {
    fun isSatisfied(candidate: TestCase): Boolean
}

data class ReductionBudget(val maxIterations: Int)

data class ReducedTestCase(
    val original: TestCase,
    val reduced: TestCase,
    val iterations: Int
)

interface ReducerPlugin {
    val id: ReducerId
    val version: InterfaceVersion

    fun reduce(
        testCase: TestCase,
        predicate: InterestingnessPredicate,
        budget: ReductionBudget
    ): ReducedTestCase
}
