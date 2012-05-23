package com.rackspace.papi.service.context.jndi;

import com.rackspace.papi.service.context.common.ContextAdapter;
import com.rackspace.papi.service.context.common.ContextAdapterProvider;

import javax.naming.Context;

public class JndiContextAdapterProvider implements ContextAdapterProvider {
   @Override
   public ContextAdapter newInstance(Context context) {
      return new JndiContextAdapter(context);
   }
}
