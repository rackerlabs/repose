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
package org.openrepose.commons.utils.regex;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zinic
 */
public class KeyedRegexExtractor<K> {

    private final List<SelectorPattern<K>> compiledPatterns;

    public KeyedRegexExtractor() {
        compiledPatterns = new LinkedList<SelectorPattern<K>>();
    }

    public void clear() {
        compiledPatterns.clear();
    }

    public void addPattern(String regexString) {
        compiledPatterns.add(new SelectorPattern<K>(Pattern.compile(regexString), null));
    }

    public void addPattern(String regexString, K key) {
        compiledPatterns.add(new SelectorPattern<K>(Pattern.compile(regexString), key));
    }

    public ExtractorResult<K> extract(String target) {
        for (SelectorPattern<K> selector : compiledPatterns) {
            final Matcher matcher = selector.matcher(target);

            if (matcher.find() && matcher.groupCount() > 0) {
                return new ExtractorResult<K>(matcher.group(1), selector.getKey());
            }
        }

        return null;
    }
}
