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
public class StringWrapper implements JCharSequence {

    private final String string;

    public StringWrapper(String string) {
        this.string = string;
    }

    @Override
    public int indexOf(String seq) {
        return string.indexOf(seq);
    }

    @Override
    public int indexOf(String seq, int fromIndex) {
        return string.indexOf(seq, fromIndex);
    }

    @Override
    public CharSequence asCharSequence() {
        return string;
    }

    @Override
    public String toString() {
        return string;
    }

    @Override
    public CharSequence subSequence(int i, int i1) {
        return string.subSequence(i, i1);
    }

    @Override
    public int length() {
        return string.length();
    }

    @Override
    public int hashCode() {
        return string.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        final boolean rtn;
        if (this == o) {
            rtn = true;
        } else if (o == null || getClass() != o.getClass()) {
            rtn = false;
        } else {
            StringWrapper that = (StringWrapper) o;
            rtn = string.equals(that.string);
        }
        return rtn;
    }

    @Override
    public char charAt(int i) {
        return string.charAt(i);
    }
}
