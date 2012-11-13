package org.openrepose.components.apivalidator.filter;

import com.rackspace.com.papi.components.checker.Validator;
import com.rackspace.com.papi.components.checker.step.ErrorResult;
import com.rackspace.com.papi.components.checker.step.MultiFailResult;
import com.rackspace.com.papi.components.checker.step.Result;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.OpenStackServiceHeader;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.HeaderValueImpl;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiValidatorHandler extends AbstractFilterLogicHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiValidatorHandler.class);
    private final List<ValidatorInfo> validators;
    private final ValidatorInfo defaultValidator;
    private FilterChain chain;
    private boolean  multiRoleMatch=false;
   

    public ApiValidatorHandler(ValidatorInfo defaultValidator, List<ValidatorInfo> validators, boolean  multiRoleMatch) {
        this.validators = new ArrayList<ValidatorInfo>(validators.size());
        if (validators != null) {
            this.validators.addAll(validators);
        }
        this.multiRoleMatch=multiRoleMatch;
        this.defaultValidator = defaultValidator;
       
    }
    

    public void setFilterChain(FilterChain chain) {
        this.chain = chain;
    }

    
    protected List<ValidatorInfo> getValidatorsForRole(List<? extends HeaderValue> listRoles) {

       Set<String> roles=new HashSet();
       List<ValidatorInfo> matchedValidators=new ArrayList<ValidatorInfo>();
        
       Iterator it = listRoles.iterator();
        while (it.hasNext()) {
            roles.add(((HeaderValue)it.next()).getValue());
        
        }
        
        if (validators != null) {
            for (ValidatorInfo validator : validators) {
                 if(roles.contains(validator.getRole())){
                 
                           matchedValidators.add(validator);
                            if(!multiRoleMatch){
                                break;
              
                  
                   }
                }
            }
        }
        
        if(!multiRoleMatch && matchedValidators.isEmpty() && defaultValidator!=null){
            matchedValidators.add(defaultValidator);
        }
        if(multiRoleMatch && defaultValidator!=null && !matchedValidators.contains(defaultValidator)){
            matchedValidators.add(defaultValidator);
        }
        
        return matchedValidators;
    }
    

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
        final FilterDirector myDirector = new FilterDirectorImpl();
        myDirector.setFilterAction(FilterAction.PASS);
        MutableHttpServletRequest mutableRequest = MutableHttpServletRequest.wrap(request);
        boolean isValid=false;
        List<HeaderValue> listRoles = mutableRequest.getPreferredHeaderValues(OpenStackServiceHeader.ROLES.toString(), new HeaderValueImpl(""));
  
        
        
       Result lastValidatorResult=null;
       myDirector.setFilterAction(FilterAction.RETURN);        
       
       try{
            if (validators != null) {
                List<ValidatorInfo> matchedValidators=getValidatorsForRole(listRoles);
              if(!matchedValidators.isEmpty()){
                 for (ValidatorInfo validatorInfo : matchedValidators) {

                         Validator validator= validatorInfo.getValidator();
                         if(validator ==null){
                              LOG.warn("Validator not available for request:", validatorInfo.getUri());
                              myDirector.setResponseStatus(HttpStatusCode.BAD_GATEWAY);

                         } else{


                                      lastValidatorResult= validator.validate(request, response, chain);
                                      isValid= lastValidatorResult.valid();
                                      myDirector.setResponseStatusCode(response.getStatus());

                                      if(isValid){

                                          break;
                                      }
                         }

                 }


                    if(!isValid && multiRoleMatch){


                              if(lastValidatorResult!=null && lastValidatorResult instanceof MultiFailResult){

                                   ErrorResult validatorMultiErrors= (ErrorResult)((MultiFailResult)lastValidatorResult).reduce().get();
                                   myDirector.setResponseStatusCode(validatorMultiErrors.code());
                                   response.sendError(validatorMultiErrors.code(), validatorMultiErrors.message());

                                 }else if(lastValidatorResult!=null && lastValidatorResult instanceof ErrorResult) {

                                   myDirector.setResponseStatusCode(((ErrorResult)lastValidatorResult).code());
                                   response.sendError(((ErrorResult)lastValidatorResult).code(), ((ErrorResult)lastValidatorResult).message());

                               }

                    }

            }else{

                myDirector.setResponseStatus(HttpStatusCode.FORBIDDEN);
                response.sendError(HttpStatusCode.FORBIDDEN.intValue());

            }
          }
        } catch (Throwable t) {

            LOG.error("Some error", t);
            myDirector.setResponseStatus(HttpStatusCode.BAD_GATEWAY);
        }
        return myDirector;
    }
}
