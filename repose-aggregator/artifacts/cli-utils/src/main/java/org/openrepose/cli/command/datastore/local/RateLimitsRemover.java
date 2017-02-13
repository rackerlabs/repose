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

public class RateLimitsRemover extends AbstractCommand {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitsRemover.class);

    @Override
    public String getCommandToken() {
        return "remove-limits";
    }

    @Override
    public String getCommandDescription() {
        return "Removes a user's rate limits from the local datastore.";
    }

    @Override
    public CommandResult perform(String[] arguments) {
        if (arguments.length != 2) {
            return new InvalidArguments("The limits remover expects two string arguments.");
        }

        CommandResult result;

        try {
            final ReposeLocalCacheMBean reposeLocalCacheMBeanProxy = new ReposeJMXClient(arguments[0]);

            if (reposeLocalCacheMBeanProxy.removeLimits(arguments[1])) {
                result = new MessageResult("Removed rate limits for user " + arguments[1]);
            } else {
                result = new CommandFailure(StatusCodes.SYSTEM_PRECONDITION_FAILURE,
                        "Failure to remove rate limits for user " + arguments[1]);
            }
        } catch (Exception e) {
            LOG.trace("Unable to connect to Repose MBean Server", e);
            result = new CommandFailure(StatusCodes.NOTHING_TO_DO,
                    "Unable to connect to Repose MBean Server: " + e.getMessage());
        }

        return result;
    }
}
