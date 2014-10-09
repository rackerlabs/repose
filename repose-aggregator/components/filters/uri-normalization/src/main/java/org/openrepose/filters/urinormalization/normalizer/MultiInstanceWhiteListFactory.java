package org.openrepose.filters.urinormalization.normalizer;

import org.openrepose.commons.utils.http.normal.ParameterFilter;
import org.openrepose.commons.utils.http.normal.ParameterFilterFactory;
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
