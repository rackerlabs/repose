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
package org.openrepose.commons.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexList {

    private final List<Pattern> regexMatchers;

    public RegexList() {
        this.regexMatchers = new LinkedList<Pattern>();
    }

    public void add(String newRegexTarget) {
        regexMatchers.add(Pattern.compile(newRegexTarget));
    }

    public Matcher find(String target) {
        for (Pattern targetPattern : regexMatchers) {
            final Matcher matcherRef = targetPattern.matcher(target);

            if (matcherRef.find()) {
                return matcherRef;
            }
        }

        return null;
    }

    public Matcher matches(String target) {
        for (Pattern targetPattern : regexMatchers) {
            final Matcher matcherRef = targetPattern.matcher(target);

            if (matcherRef.matches()) {
                return matcherRef;
            }
        }

        return null;
    }
}
