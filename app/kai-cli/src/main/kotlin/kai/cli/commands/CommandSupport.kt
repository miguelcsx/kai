package kai.cli.commands

internal data class CommonOptions(
    val storageDir: String?,
    val pluginDir: String?,
    val json: Boolean,
    val positional: List<String>
)

internal fun parseOptions(args: List<String>): CommonOptions {
    var storageDir: String? = null
    var pluginDir: String? = null
    var json = false
    val positional = mutableListOf<String>()
    var index = 0
    while (index < args.size) {
        when (val arg = args[index]) {
            "--json" -> json = true
            "--storage-dir" -> {
                index++
                storageDir = args.getOrNull(index) ?: error("--storage-dir requires a value")
            }
            "--plugins-dir" -> {
                index++
                pluginDir = args.getOrNull(index) ?: error("--plugins-dir requires a value")
            }
            else -> positional += arg
        }
        index++
    }
    return CommonOptions(storageDir, pluginDir, json, positional)
}
