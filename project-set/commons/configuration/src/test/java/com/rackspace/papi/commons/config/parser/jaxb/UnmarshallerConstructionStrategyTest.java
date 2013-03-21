package com.rackspace.papi.commons.config.parser.jaxb;

import com.rackspace.papi.commons.util.pooling.ConstructionStrategy;
import com.rackspace.papi.commons.util.pooling.ResourceConstructionException;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class UnmarshallerConstructionStrategyTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    public static class WhenUsingUnmarshallerConstructionStrategy {

        @Test(expected= ResourceConstructionException.class)
        public void testConstruct() throws JAXBException {
            JAXBContext jaxbContext = mock(JAXBContext.class);
            when(jaxbContext.createUnmarshaller()).thenThrow(new JAXBException("mock jaxb exception"));
                        
            ConstructionStrategy<Unmarshaller> constructionStrategy = new UnmarshallerConstructionStrategy(jaxbContext);

            constructionStrategy.construct();
        }
    }


}
