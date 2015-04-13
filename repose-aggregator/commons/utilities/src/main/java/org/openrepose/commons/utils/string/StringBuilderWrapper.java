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
package org.openrepose.commons.utils.string;

/**
 * @author zinic
 */
public class StringBuilderWrapper implements JCharSequence {

    private final StringBuilder stringBuilder;

    public StringBuilderWrapper(StringBuilder stringBuilder) {
        this.stringBuilder = stringBuilder;
    }

    @Override
    public int indexOf(String seq) {
        return stringBuilder.indexOf(seq);
    }

    @Override
    public int indexOf(String seq, int fromIndex) {
        return stringBuilder.indexOf(seq, fromIndex);
    }

    @Override
    public CharSequence asCharSequence() {
        return stringBuilder;
    }

    @Override
    public char charAt(int i) {
        return stringBuilder.charAt(i);
    }

    @Override
    public int length() {
        return stringBuilder.length();
    }

    @Override
    public CharSequence subSequence(int i, int i1) {
        return stringBuilder.subSequence(i, i1);
    }
}
