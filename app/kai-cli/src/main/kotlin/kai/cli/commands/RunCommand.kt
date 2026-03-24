package kai.cli.commands

class RunCommand {
    fun run(args: List<String>) {
        val options = parseOptions(args)
        require(options.positional.isNotEmpty()) { "run requires a config path" }
        val configPath = options.positional[0]
        val storage = defaultStorage(options.storageDir)
        val config = parser().parse(java.nio.file.Paths.get(configPath))
        defaultEngine(storage, options.pluginDir).run(config) { emitEvent(it, options.json) }.getOrThrow()
    }
}
