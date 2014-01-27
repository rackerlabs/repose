package com.rackspace.papi.components.datastore;

import com.rackspace.papi.components.datastore.distributed.SerializablePatch;

import java.io.Serializable;

/**
 * A simple patchable implementation that works with strings.
 *
 * Created with IntelliJ IDEA.
 * User: adrian
 * Date: 1/27/14
 * Time: 10:47 AM
 */
public class StringValue implements Patchable<StringValue, StringValue.Patch>, Serializable {
    private String value;

    public StringValue(String value) {
        this.value = value;
    }

    /**
     * Append the value of the patch onto the existing value.
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

    public static class Patch implements SerializablePatch<StringValue> {
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

