/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.cli;

import org.openrepose.cli.command.Command;
import org.openrepose.cli.command.results.CommandResult;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * @author zinic
 */
public class CommandDriver {

    private final Command myCommand;
    private final String[] args;

    public CommandDriver(Command command, String[] args) {
        this.myCommand = command;
        this.args = Arrays.copyOf(args, args.length);
    }

    public static void main(String[] args) {
        final CommandResult result = new CommandDriver(new RootCommandLine(), args).go();

        if (StringUtils.isNotBlank(result.getStringResult())) {
            System.out.println(result.getStringResult());
        }

        System.exit(result.getStatusCode());
    }

    public CommandResult go() {
        return args.length > 0 ? nextCommand(args[0]) : myCommand.perform(args);
    }

    public CommandResult nextCommand(String nextArgument) {
        if (StringUtils.isBlank(nextArgument)) {
            throw new IllegalArgumentException();
        }

        for (Command availableCommand : myCommand.availableCommands()) {
            if (availableCommand.getCommandToken().equalsIgnoreCase(nextArgument)) {
                return new CommandDriver(availableCommand, Arrays.copyOfRange(args, 1, args.length)).go();
            }
        }

        return myCommand.perform(args);
    }
}
