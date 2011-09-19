package com.rackspace.papi.components.datastore.integration;

import com.rackspace.papi.service.datastore.Coalescent;

/**
 *
 * @author zinic
 */
public class CacheableValue implements Coalescent<CacheableValue> {

    private final int value;
    private final boolean shouldCoalesce;

    public CacheableValue(int value, boolean shouldCoalesce) {
        this.value = value;
        this.shouldCoalesce = shouldCoalesce;
    }

    @Override
    public Coalescent<CacheableValue> coalesce(Coalescent<CacheableValue>... targets) {
        if (!shouldCoalesce) {
            return targets.length > 0 ? targets[targets.length] : this;
        }

        int largest = value;

        for (Coalescent<CacheableValue> target : targets) {
            final int nextValue = ((CacheableValue) target).value;

            if (nextValue > largest) {
                largest = nextValue;
            }
        }

        return new CacheableValue(largest, shouldCoalesce);
    }

    public int getValue() {
        return value;
    }

    public CacheableValue next() {
        return new CacheableValue(value + 1, shouldCoalesce);
    }
}