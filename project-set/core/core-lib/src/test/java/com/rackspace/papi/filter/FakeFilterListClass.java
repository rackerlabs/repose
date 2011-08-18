package com.rackspace.papi.filter;

import com.rackspace.papi.model.Filter;
import com.rackspace.papi.model.FilterList;

import java.util.ArrayList;

/**
 * @author fran
 */
public class FakeFilterListClass extends FilterList {

    public FakeFilterListClass() {
        super.filter = new ArrayList<Filter>();
    }

    public void addFilter(Filter filter) {
        super.filter.add(filter);        
    }
}
