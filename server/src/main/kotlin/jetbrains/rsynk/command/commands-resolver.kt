/**
 * Copyright 2016 - 2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.rsynk.command

import jetbrains.rsynk.files.FileInfoReader
import jetbrains.rsynk.files.TrackedFilesProvider


internal interface CommandsResolver {
    fun resolve(args: List<String>): Command
}

internal class AllCommandsResolver(fileInfoReader: FileInfoReader,
                                   trackedFiles: TrackedFilesProvider) : CommandsResolver {

    private val rsyncCommandsResolver = RsyncCommandsResolver(fileInfoReader, trackedFiles)

    override fun resolve(args: List<String>): Command {

        args.firstOrNull()?.let {
            if (it == "rsync") {
                return rsyncCommandsResolver.resolve(args)
            }
        }

        throw CommandNotFoundException("Not commands found matching given arguments " +
                args.joinToString(prefix = "[", postfix = "]", separator = ", "))
    }

}

internal class RsyncCommandsResolver(fileInfoReader: FileInfoReader,
                                     trackedFiles: TrackedFilesProvider) : CommandsResolver {

    private interface RsyncCommandArgsPredicate {
        fun match(args: List<String>): Boolean
    }

    private data class RsyncCommandAndArgsPredicate(
            val command: RsyncCommand,
            val predicate: RsyncCommandArgsPredicate
    )

    private val commands: List<RsyncCommandAndArgsPredicate> = listOf(
            RsyncCommandAndArgsPredicate(
                    RsyncServerSendCommand(fileInfoReader, trackedFiles),
                    object : RsyncCommandArgsPredicate {
                        override fun match(args: List<String>): Boolean {
                            if (args.size < 4) {
                                return false
                            }
                            if (args.any { it == "--daemon" || it == "daemon" }) {
                                return false
                            }
                            return args[1] == "--server" && args[2] == "--sender"
                        }
                    }
            )
    )

    override fun resolve(args: List<String>): Command {

        val matchedCommands = commands.filter { it.predicate.match(args) }

        if (matchedCommands.isEmpty() || matchedCommands.size > 1) {
            throw CommandNotFoundException("Zero or more than one command match given args "
                    + args.joinToString(prefix = "[", postfix = "]", separator = ", "))
        }

        return matchedCommands[0].command
    }
}

class CommandNotFoundException(message: String) : RuntimeException(message)
