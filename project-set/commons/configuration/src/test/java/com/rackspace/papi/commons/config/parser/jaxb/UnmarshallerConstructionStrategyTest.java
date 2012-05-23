package com.rackspace.papi.commons.config.parser.jaxb;

import com.rackspace.papi.commons.util.pooling.ConstructionStrategy;
import com.rackspace.papi.commons.util.pooling.ResourceConstructionException;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class UnmarshallerConstructionStrategyTest {

    public static class WhenUsingUnmarshallerConstructionStrategy {

        @Test(expected= ResourceConstructionException.class)
        public void shouldThrowResourceConstructionException() throws JAXBException {
            JAXBContext jaxbContext = mock(JAXBContext.class);
            when(jaxbContext.createUnmarshaller()).thenThrow(new JAXBException("mock jaxb exception"));
                        
            ConstructionStrategy<Unmarshaller> constructionStrategy = new UnmarshallerConstructionStrategy(jaxbContext);

            constructionStrategy.construct();
        }
    }
}
