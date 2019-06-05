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
package org.openrepose.core.services.datastore.types;

import org.openrepose.core.services.datastore.Patchable;

/**
 * A simple patchable implementation that works with strings.
 * <p/>
 * Created with IntelliJ IDEA.
 * User: adrian
 * Date: 1/27/14
 * Time: 10:47 AM
 */
public class StringValue implements Patchable<StringValue, StringValue.Patch> {
    private String value;

    public StringValue(String value) {
        this.value = value;
    }

    /**
     * Append the value of the patch onto the existing value.
     *
     * @param patch the patch to apply
     * @return the patched value
     */
    @Override
    public StringValue applyPatch(Patch patch) {
        String originalValue = value;
        value = value + patch.newFromPatch().getValue();
        return new StringValue(originalValue + patch.newFromPatch().getValue());
    }

    public String getValue() {
        return value;
    }

    public static class Patch implements org.openrepose.core.services.datastore.Patch<StringValue> {
        private String value;

        public Patch(String value) {
            this.value = value;
        }

        @Override
        public StringValue newFromPatch() {
            return new StringValue(value);
        }
    }
}

