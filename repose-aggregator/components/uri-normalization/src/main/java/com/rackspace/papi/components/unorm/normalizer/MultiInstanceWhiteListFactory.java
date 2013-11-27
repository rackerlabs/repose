package com.rackspace.papi.components.unorm.normalizer;

import com.rackspace.papi.commons.util.http.normal.ParameterFilter;
import com.rackspace.papi.commons.util.http.normal.ParameterFilterFactory;
import com.rackspace.papi.components.uri.normalization.config.HttpUriParameterList;

/**
 *
 * @author zinic
 */
public class MultiInstanceWhiteListFactory implements ParameterFilterFactory {

   private final HttpUriParameterList parameterList;

   public MultiInstanceWhiteListFactory(HttpUriParameterList parameterList) {
      this.parameterList = parameterList;
   }

   @Override
   public ParameterFilter newInstance() {
      return new MultiInstanceWhiteList(parameterList);
   }
}
