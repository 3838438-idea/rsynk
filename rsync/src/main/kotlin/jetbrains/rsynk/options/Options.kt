package jetbrains.rsynk.options

sealed class Option {
    object Server : Option()
    object Sender : Option()
    object Daemon : Option()

    object Compress : Option()
    class ChecksumSeed(val seed: Int) : Option()
    object ChecksumSeedOrderFix : Option()
    object Delete : Option()

    object FListIOErrorSafety : Option()
    object RelativePaths : Option()
    object ShellCommand : Option()
    object SymlinkTimeSetting : Option()
    object NumericIds : Option()
    object OneFileSystem : Option()
    class PreReleaseInfo(val info: String) : Option()
    object PreserveDevices : Option()
    object PreserveGroup : Option()
    object PreserveLinks : Option()
    object PreserveSpecials : Option()
    object PreserveUser : Option()
    object ProtectArgs : Option()
    object PruneEmptyDirectories : Option()

    sealed class FileSelection : Option() {
        /**
         * Transfer client's file list exactly but exclude directories
         * */
        object NoDirectories : FileSelection()

        /**
         * Transfer client's file list exactly and include directories
         * recursively
         * */
        object Recurse : FileSelection()

        /**
         * Transfer client's file list and the content of any dot directory
         * */
        object TransferDirectoriesWithoutContent : FileSelection()
    }

    object VerboseMode : Option()
}






