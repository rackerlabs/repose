package com.rackspace.papi.service.filterchain;

import com.rackspace.papi.filter.PowerFilterChainBuilder;

public interface FilterChainGarbageCollectorService {

    void retireFilterChainBuilder(PowerFilterChainBuilder builder);
}
