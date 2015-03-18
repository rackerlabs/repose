/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrepose.commons.utils.test;

import org.apache.commons.cli.*;

public class ReposeContainerUtil {

    private ReposeContainerUtil(){
    }

    public static ReposeContainerProps parseArgs(String[] args) throws ParseException {

        Options options = new Options();
        CommandLineParser parser = new BasicParser();

        Option portOpt = new Option("p", true, "Repose port to listen on");
        Option rootwarOpt = new Option("w", true, "Location of ROOT.war");
        Option applicationWarsOpt = new Option("war", true, "");

        portOpt.setRequired(true);
        rootwarOpt.setRequired(true);
        options.addOption(portOpt).addOption(rootwarOpt).addOption(applicationWarsOpt);
        final CommandLine cmdline;

        cmdline = parser.parse(options, args);

        return new ReposeContainerProps(cmdline.getOptionValue("p"), cmdline.getOptionValue("w"), cmdline.getOptionValues("war"));
    }
}
