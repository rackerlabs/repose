package com.rackspace.auth.openstack.ids;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.papi.commons.util.pooling.ConstructionStrategy;
import com.rackspace.papi.commons.util.pooling.GenericBlockingResourcePool;
import com.rackspace.papi.commons.util.pooling.ResourceConstructionException;
import com.rackspace.papi.commons.util.pooling.ResourceContextException;


import com.rackspace.papi.commons.util.pooling.Pool;
import com.rackspace.papi.commons.util.pooling.ResourceContext;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

/**
 * @author fran
 */
public class ResponseUnmarshaller {

   private final JAXBContext jaxbContext;
   private final Pool<Unmarshaller> pool;

   public ResponseUnmarshaller() {
      try {
         jaxbContext = JAXBContext.newInstance(org.openstack.docs.identity.api.v2.ObjectFactory.class);
         pool = new GenericBlockingResourcePool<Unmarshaller>(new ConstructionStrategy<Unmarshaller>() {

            @Override
            public Unmarshaller construct() throws ResourceConstructionException {
               try {
                  return jaxbContext.createUnmarshaller();
               } catch (JAXBException ex) {
                  throw new ResourceConstructionException("Unable to build jaxb unmarhshaller", ex);
               }
            }
         });
      } catch (JAXBException jaxbe) {
         throw new AuthServiceException(
                 "Possible deployment problem! Unable to build JAXB Context for Auth v1.1 schema types. Reason: "
                 + jaxbe.getMessage(), jaxbe);
      }
   }

   /*
   public <T> T unmarshall(final GetMethod method, final Class<T> expectedType) {
      return pool.use(new ResourceContext<Unmarshaller, T>() {

         @Override
         public T perform(Unmarshaller resource) throws ResourceContextException {
            try {
               final Object o = resource.unmarshal(new StringReader(method.getResponseBodyAsString()));

               if (o instanceof JAXBElement && ((JAXBElement) o).getDeclaredType().equals(expectedType)) {
                  return ((JAXBElement<T>) o).getValue();
               } else if (o instanceof FullToken) {
                  return expectedType.cast(o);
               } else {
                  throw new AuthServiceException("Failed to unmarshall response body. Unexpected element encountered. Body output is in debug.");

               }
            } catch (IOException ioe) {
               throw new AuthServiceException("Failed to get response body from response.", ioe);
            } catch (JAXBException jaxbe) {
               throw new AuthServiceException("Failed to unmarshall response body. Body output is in debug. Reason: "
                       + jaxbe.getMessage(), jaxbe);
            }
         }
      });
   }
    *
    */

   public <T> T unmarshall(final String data, final Class<T> expectedType) {
      return pool.use(new ResourceContext<Unmarshaller, T>() {

         @Override
         public T perform(Unmarshaller resource) throws ResourceContextException {
            try {
               final Object o = resource.unmarshal(new StringReader(data));

               if (o instanceof JAXBElement && ((JAXBElement) o).getDeclaredType().equals(expectedType)) {
                  return ((JAXBElement<T>) o).getValue();
               } else if (o instanceof Groups) {
                  return expectedType.cast(o);
               } else {
                  throw new AuthServiceException("Failed to unmarshall response body. Unexpected element encountered. Body output is in debug.");

               }
            } catch (JAXBException jaxbe) {
               throw new AuthServiceException("Failed to unmarshall response body. Body output is in debug. Reason: "
                       + jaxbe.getMessage(), jaxbe);
            }
         }
      });
   }

   public <T> T unmarshall(final InputStream data, final Class<T> expectedType) {
      return pool.use(new ResourceContext<Unmarshaller, T>() {

         @Override
         public T perform(Unmarshaller resource) throws ResourceContextException {
            try {
               final Object o = resource.unmarshal(new InputStreamReader(data));

               if (o instanceof JAXBElement && ((JAXBElement) o).getDeclaredType().equals(expectedType)) {
                  return ((JAXBElement<T>) o).getValue();
               } else if (o instanceof Groups) {
                  return expectedType.cast(o);
               } else {
                  throw new AuthServiceException("Failed to unmarshall response body. Unexpected element encountered. Body output is in debug.");

               }
            } catch (JAXBException jaxbe) {
               throw new AuthServiceException("Failed to unmarshall response body. Body output is in debug. Reason: "
                       + jaxbe.getMessage(), jaxbe);
            }
         }
      });
   }
}

