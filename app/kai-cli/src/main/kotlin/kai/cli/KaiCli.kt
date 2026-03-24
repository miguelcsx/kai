package kai.cli

import java.nio.file.Paths
import kai.cli.commands.RegressCommand
import kai.cli.commands.ReplayCommand
import kai.cli.commands.RunCommand

class KaiCli {
    fun run(args: List<String>) {
        when (args.firstOrNull()) {
            "run" -> RunCommand().run(args.drop(1))
            "replay" -> ReplayCommand().run(args.drop(1))
            "regress" -> RegressCommand().run(args.drop(1))
            else -> printUsage()
        }
    }

    private fun printUsage() {
        println("Usage:")
        println("  kai-cli run <config.toml> [--json] [--storage-dir DIR] [--plugins-dir DIR]")
        println("  kai-cli replay <finding-id> [--storage-dir DIR] [--plugins-dir DIR]")
        println("  kai-cli regress [--storage-dir DIR] [--plugins-dir DIR]")
        println("Example: ${Paths.get("examples/demo.toml")}")
    }
}
