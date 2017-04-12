package jetbrains.rsynk.options

class RequestOptions(val options: Set<Option>) {

    val server: Boolean = options.contains(Option.Server)
    val sender: Boolean = options.contains(Option.Sender)
    val daemon: Boolean = options.contains(Option.Daemon)

    val compress: Boolean = options.contains(Option.Compress)
    val checksumSeedOrderFix = options.contains(Option.ChecksumSeedOrderFix)

    val delete: Boolean = options.contains(Option.Delete)
    val directoryMode: Option.FileSelection
        get() {
            return options.filter { it is Option.FileSelection }
                    .map { it as Option.FileSelection }
                    .singleOrNull() ?: Option.FileSelection.NoDirectories
        }

    val incrementalRecurse: Boolean = options.contains(Option.IncrementalRecurse)
    val numericIds: Boolean = Option.NumericIds in options
    val oneFileSystem: Boolean = options.contains(Option.OneFileSystem)
    val preReleaseInfo: String?
        get() {
            val info = options.firstOrNull { it is Option.PreReleaseInfo } as? Option.PreReleaseInfo
            return info?.info
        }
    val preserveDevices: Boolean  = Option.PreserveDevices in options
    val preserveGroup: Boolean = Option.PreserveGroup in options
    val preserveLinks: Boolean = Option.PreserveLinks in options
    val preserveSpecials: Boolean = Option.PreserveSpecials in options
    val preserveUser: Boolean = Option.PreserveUser in options
    val protectArgs: Boolean = options.contains(Option.ProtectArgs)
    val relativeNames: Boolean = options.contains(Option.RelativePaths)
    val saveFlist: Boolean = options.contains(Option.FListIOErrorSafety)
    val shellCommand: Boolean = options.contains(Option.ShellCommand)
    val symlinkTimeSettings: Boolean = options.contains(Option.SymlinkTimeSetting)
    val verboseMode: Boolean = options.contains(Option.VerboseMode)
}
