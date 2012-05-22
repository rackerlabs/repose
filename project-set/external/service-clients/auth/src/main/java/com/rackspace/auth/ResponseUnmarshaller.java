package com.rackspace.auth;

import com.rackspace.papi.commons.util.pooling.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * @author fran
 */
public class ResponseUnmarshaller {

   private final JAXBContext jaxbContext;
   private final Pool<Unmarshaller> pool;

   public ResponseUnmarshaller(JAXBContext jaxbContext) {
      this.jaxbContext = jaxbContext;
      pool = new GenericBlockingResourcePool<Unmarshaller>(new ConstructionStrategy<Unmarshaller>() {

         @Override
         public Unmarshaller construct() {
            try {
               return ResponseUnmarshaller.this.jaxbContext.createUnmarshaller();
            } catch (JAXBException ex) {
               throw new ResourceConstructionException("Unable to build jaxb unmarshaller", ex);
            }
         }
      });

   }

   public <T> T unmarshall(final InputStream data, final Class<T> expectedType) {
      return pool.use(new UnmarshallerContext<T>(new InputStreamReader(data), expectedType));
   }

   private static final class UnmarshallerContext<T> implements ResourceContext<Unmarshaller, T> {

      private final Reader reader;
      private final Class<T> expectedType;

      private UnmarshallerContext(final Reader reader, final Class<T> expectedType) {
         this.reader = reader;
         this.expectedType = expectedType;
      }

      @Override
      public T perform(Unmarshaller resource) {
         try {
            final Object o = resource.unmarshal(reader);

            if (o instanceof JAXBElement && ((JAXBElement) o).getDeclaredType().equals(expectedType)) {
               return ((JAXBElement<T>) o).getValue();
            } else {
               throw new AuthServiceException("Failed to unmarshall response body. Unexpected element encountered. Body output is in debug.");

            }
         } catch (JAXBException jaxbe) {
            throw new AuthServiceException("Failed to unmarshall response body. Body output is in debug. Reason: "
                    + jaxbe.getMessage(), jaxbe);
         }
      }
   }   
}
