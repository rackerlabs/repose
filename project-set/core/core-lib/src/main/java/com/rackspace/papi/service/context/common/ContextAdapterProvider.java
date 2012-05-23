package com.rackspace.papi.service.context.common;

import javax.naming.Context;

public interface ContextAdapterProvider {
   ContextAdapter newInstance(Context context);
}
