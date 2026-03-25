package kai.plugins.strategy.random

import java.util.Random
import kai.domain.id.InterfaceVersion
import kai.domain.id.StrategyId
import kai.domain.observations.Observations
import kai.domain.testcase.Provenance
import kai.domain.testcase.SourceFile
import kai.domain.testcase.TestCase
import kai.plugin.strategy.FitnessSignal
import kai.plugin.strategy.GenerationContext
import kai.plugin.strategy.MutationContext
import kai.plugin.strategy.StrategyPlugin

class RandomStrategyPlugin : StrategyPlugin {
    override val id = StrategyId("random-generator")
    override val version = InterfaceVersion.V1

    override fun generate(context: GenerationContext): Sequence<TestCase> {
        return sequenceOf(buildCase(context))
    }

    override fun mutate(seed: TestCase, context: MutationContext): TestCase {
        val nextContent = seed.sources.first().content + "\nval extra${context.iteration} = ${context.iteration}"
        return recreate(seed, nextContent, context.seed, context.iteration)
    }

    override fun feedback(observations: Observations, testCase: TestCase): FitnessSignal {
        val failures = observations.results.count { it.exitCode != 0 }
        return FitnessSignal(failures.toDouble(), "non_zero_results=$failures")
    }

    private fun buildCase(context: GenerationContext): TestCase {
        val random = Random(context.seed)
        val body = template(random, context.iteration)
        return TestCase.create(
            sources = listOf(SourceFile.create("Main.kt", body)),
            buildConfig = context.buildConfig,
            provenance = provenance(context)
        )
    }

    private fun template(random: Random, iteration: Int): String {
        val name = "v$iteration"
        val first = random.nextInt(9) + 1
        val second = random.nextInt(9) + 1
        return listOf(
            "fun helper$name(): Int {",
            "    val left = $first",
            "    val right = $second",
            "    val unused = left - right",
            "    return left + right",
            "}",
            "",
            "fun main() {",
            "    println(\"kai-$iteration=\" + helper$name())",
            "}"
        ).joinToString("\n")
    }

    private fun provenance(context: GenerationContext): Provenance {
        return Provenance(
            campaignId = context.campaignId,
            strategyId = id,
            parentId = null,
            generationSeed = context.seed,
            mutationDepth = 0
        )
    }

    private fun recreate(
        seed: TestCase,
        content: String,
        generationSeed: Long,
        mutationDepth: Int
    ): TestCase {
        return TestCase.create(
            sources = listOf(SourceFile.create(seed.sources.first().relativePath, content)),
            buildConfig = seed.buildConfig,
            provenance = seed.provenance.copy(
                parentId = seed.id,
                generationSeed = generationSeed,
                mutationDepth = mutationDepth
            )
        )
    }
}
