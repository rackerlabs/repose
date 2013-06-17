package com.rackspace.papi.service.reporting.metrics;

import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.service.reporting.metrics.impl.MeterByCategorySum;
import com.rackspace.papi.service.reporting.metrics.impl.MetricsServiceImpl;
import com.rackspace.papi.spring.ReposeJmxNamingStrategy;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Meter;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class MetricsServiceImplTest {

    public static class Register {

        protected MetricsService metricsService;
        protected ReposeJmxNamingStrategy reposeStrat;

        @Before
        public void setUp() {

            ReposeInstanceInfo reposeInstanceInfo = new ReposeInstanceInfo();
            reposeInstanceInfo.setNodeId( "node1" );
            reposeInstanceInfo.setClusterId( "cluster1" );

            reposeStrat = new ReposeJmxNamingStrategy( new AnnotationJmxAttributeSource(),
                                                       reposeInstanceInfo );

            metricsService = new MetricsServiceImpl( reposeStrat );

        }

        protected Object getAttribute( Class klass, String name, String scope, String att )
              throws
              MalformedObjectNameException,
              AttributeNotFoundException,
              MBeanException,
              ReflectionException,
              InstanceNotFoundException {

            Hashtable<String, String> hash = new Hashtable<String, String>();
            hash.put( "name", "\"" + name + "\"" );
            hash.put( "scope", "\"" + scope + "\"" );
            hash.put( "type", "\"" + klass.getSimpleName() + "\"" );

            // Lets you see all registered MBean ObjectNames
            //Set<ObjectName> set = ManagementFactory.getPlatformMBeanServer().queryNames(null, null);

            ObjectName on =
                  new ObjectName( "\"" + reposeStrat.getDomainPrefix() + klass.getPackage().getName() + "\"", hash );

            return ManagementFactory.getPlatformMBeanServer().getAttribute( on, att );
        }

        @Test
        public void testServiceMeter()
              throws
              MalformedObjectNameException,
              AttributeNotFoundException,
              MBeanException,
              ReflectionException,
              InstanceNotFoundException {

            Meter m = metricsService.newMeter( this.getClass(), "meter1", "scope1", "hits", TimeUnit.SECONDS );

            m.mark();
            m.mark();
            m.mark();

            long l = (Long) getAttribute( this.getClass(), "meter1", "scope1", "Count" );

            assertEquals( (long) 3, l );
        }

        @Test
        public void testServiceCounter()
              throws
              MalformedObjectNameException,
              AttributeNotFoundException,
              MBeanException,
              ReflectionException,
              InstanceNotFoundException {

            Counter c = metricsService.newCounter( this.getClass(), "counter1", "scope1" );

            c.inc();
            c.inc();
            c.inc();
            c.inc();
            c.dec();

            long l = (Long) getAttribute( this.getClass(), "counter1", "scope1", "Count" );

            assertEquals( (long) 3, l );
        }

        @Test
        public void testMeterByCategory() throws
              MalformedObjectNameException,
              AttributeNotFoundException,
              MBeanException,
              ReflectionException,
              InstanceNotFoundException {

            MeterByCategory m = metricsService.newMeterByCategory( this.getClass(), "scope1", "hits", TimeUnit.SECONDS );

            m.mark( "meter1" );
            m.mark( "meter2", (long)4 );
            m.mark( "meter1" );

            long l = (Long) getAttribute( this.getClass(), "meter1", "scope1", "Count" );
            assertEquals( (long) 2, l );

            l = (Long) getAttribute( this.getClass(), "meter2", "scope1", "Count" );
            assertEquals( (long) 4, l );
        }

        @Test
        public void testMeterByCategorySum() throws
              MalformedObjectNameException,
              AttributeNotFoundException,
              MBeanException,
              ReflectionException,
              InstanceNotFoundException {

            MeterByCategory m = metricsService.newMeterByCategorySum( this.getClass(), "scope1", "hits", TimeUnit.SECONDS );

            m.mark( "meter1" );
            m.mark( "meter2", (long)4 );
            m.mark( "meter1" );

            long l = (Long) getAttribute( this.getClass(), "meter1", "scope1", "Count" );
            assertEquals( (long) 2, l );

            l = (Long) getAttribute( this.getClass(), "meter2", "scope1", "Count" );
            assertEquals( (long) 4, l );

            l = (Long) getAttribute( this.getClass(), MeterByCategorySum.ALL, "scope1", "Count" );
            assertEquals( (long) 6, l );
        }

        @Test( expected = IllegalArgumentException.class )
        public void testNoAllowALL() {

            MeterByCategory m = metricsService.newMeterByCategorySum( this.getClass(), "scope1", "hits", TimeUnit.SECONDS );

            m.mark( MeterByCategorySum.ALL );
        }

        @Test( expected = IllegalArgumentException.class )
        public void testNoAllowALL2() {

            MeterByCategory m = metricsService.newMeterByCategorySum( this.getClass(), "scope1", "hits", TimeUnit.SECONDS );

            m.mark( MeterByCategorySum.ALL, 2 );
        }
    }
}
