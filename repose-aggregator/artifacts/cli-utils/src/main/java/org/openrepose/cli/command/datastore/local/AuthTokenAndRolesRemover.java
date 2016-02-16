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
package org.openrepose.cli.command.datastore.local;

import org.openrepose.cli.command.AbstractCommand;
import org.openrepose.cli.command.results.*;
import org.openrepose.core.services.datastore.distributed.impl.ehcache.ReposeLocalCacheMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthTokenAndRolesRemover extends AbstractCommand {

    private static final Logger LOG = LoggerFactory.getLogger(AuthTokenAndRolesRemover.class);
    private static final int ARGUMENTS_LENGTH = 3;

    @Override
    public String getCommandToken() {
        return "remove-token";
    }

    @Override
    public String getCommandDescription() {
        return "Removes a user's auth token and roles from the local datastore.";
    }

    @Override
    public CommandResult perform(String[] arguments) {

        if (arguments.length != ARGUMENTS_LENGTH) {
            return new InvalidArguments("The token remover expects three string arguments.");
        }

        CommandResult result;

        try {
            final ReposeLocalCacheMBean reposeLocalCacheMBeanProxy = new ReposeJMXClient(arguments[0]);

            if (reposeLocalCacheMBeanProxy.removeTokenAndRoles(arguments[1], arguments[2])) {
                result = new MessageResult("Removed auth token and roles for user " + arguments[1]);
            } else {
                result = new CommandFailure(StatusCodes.SYSTEM_PRECONDITION_FAILURE.getStatusCode(),
                        "Failure to remove auth token and roles for user " + arguments[1]);
            }
        } catch (Exception e) {
            LOG.trace("Unable to connect to repose MBean Server", e);
            result = new CommandFailure(StatusCodes.NOTHING_TO_DO.getStatusCode(),
                    "Unable to connect to Repose MBean Server: " + e.getMessage());
        }

        return result;
    }
}
