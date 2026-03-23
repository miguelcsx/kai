package kai.plugins.reducer.delta

import kai.domain.id.InterfaceVersion
import kai.domain.id.ReducerId
import kai.domain.testcase.SourceFile
import kai.domain.testcase.TestCase
import kai.plugin.reducer.InterestingnessPredicate
import kai.plugin.reducer.ReducedTestCase
import kai.plugin.reducer.ReductionBudget
import kai.plugin.reducer.ReducerPlugin

class DeltaReducerPlugin : ReducerPlugin {
    override val id = ReducerId("delta-debugger")
    override val version = InterfaceVersion.V1

    override fun reduce(
        testCase: TestCase,
        predicate: InterestingnessPredicate,
        budget: ReductionBudget
    ): ReducedTestCase {
        var current = testCase
        var iterations = 0
        repeat(budget.maxIterations) {
            val next = trimOneLine(current) ?: return@repeat
            if (predicate.isSatisfied(next)) {
                current = next
                iterations++
            }
        }
        return ReducedTestCase(testCase, current, iterations)
    }

    private fun trimOneLine(testCase: TestCase): TestCase? {
        val source = testCase.sources.first()
        val lines = source.content.lines()
        if (lines.size <= 2) {
            return null
        }
        val trimmed = lines.dropLast(1).joinToString("\n")
        if (!looksSafe(trimmed)) {
            return null
        }
        return TestCase.create(
            sources = listOf(SourceFile.create(source.relativePath, trimmed)),
            buildConfig = testCase.buildConfig,
            provenance = testCase.provenance
        )
    }

    private fun looksSafe(content: String): Boolean {
        if (!content.contains("fun main")) {
            return false
        }
        return balance(content, '{', '}') && balance(content, '(', ')')
    }

    private fun balance(
        content: String,
        open: Char,
        close: Char
    ): Boolean {
        var count = 0
        content.forEach {
            if (it == open) count++
            if (it == close) count--
        }
        return count == 0
    }
}
