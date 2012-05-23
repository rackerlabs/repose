package com.rackspace.papi.service.context;

import javax.naming.Context;

public interface ContextAdapterProvider {
   ContextAdapter newInstance(Context context);
}
