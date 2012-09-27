package org.openrepose.components.xsdvalidator.servlet;

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import javax.servlet.http.HttpServletRequest;
import com.rackspace.com.papi.components.checker.Validator;
import com.rackspace.com.papi.components.checker.step.Result;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.HeaderValueImpl;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XsdValidatorHandler extends AbstractFilterLogicHandler {

   private static final Logger LOG = LoggerFactory.getLogger(XsdValidatorHandler.class);
   private final Map<String, Validator> validators;
   private final Validator defaultValidator;
   private FilterChain chain;

   public XsdValidatorHandler(Validator defaultValidator, Map<String, Validator> validators) {
      this.validators = new HashMap<String, Validator>();
      this.validators.putAll(validators);
      this.defaultValidator = defaultValidator;

   }
   
   public void setFilterChain(FilterChain chain) {
      this.chain = chain;
   }
   
   private static class ValidationFilterChain implements FilterChain {

      @Override
      public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
      }
   }

   protected Validator getValidatorForRole(List<? extends HeaderValue> roles) {

      if (validators != null) {
         for (String validatorRole: validators.keySet()) {
            for (HeaderValue role : roles) {
               if (validatorRole.contains(role.getValue())) {
                  return validators.get(validatorRole);
               }
            }
         }
      }
      
      return defaultValidator;
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      final FilterDirector myDirector = new FilterDirectorImpl();
      myDirector.setFilterAction(FilterAction.PASS);
      MutableHttpServletRequest mutableRequest = MutableHttpServletRequest.wrap(request);
      
      List<HeaderValue> groups = mutableRequest.getPreferredHeaderValues(PowerApiHeader.GROUPS.toString(), new HeaderValueImpl(""));
      Validator validator = getValidatorForRole(groups);
     
      if (validator != null) {
         Result validate = validator.validate(request, response, new ValidationFilterChain());         
         if (!validate.valid()) {
            myDirector.setFilterAction(FilterAction.RETURN);
            myDirector.setResponseStatusCode(response.getStatus());
         }
      }
      
      return myDirector;
   }
}
