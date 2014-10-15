package org.openrepose.core.filter;

import org.openrepose.core.systemmodel.Filter;
import org.openrepose.core.systemmodel.FilterList;

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
