package com.rackspace.papi.service.metrics;

import com.rackspace.papi.service.metrics.impl.MeterByCategorySum;
import com.rackspace.papi.service.metrics.impl.MetricsServiceImpl;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class MetricsServiceImplTest {

    public static class Register {

        protected MetricsService metricsService;
        protected Object reposeStrat;

        @Before
        public void setUp() {

            reposeStrat = mock(Object.class);

            when(reposeStrat.toString()).thenReturn("cluster1-node1");

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
                  new ObjectName( "\"" + reposeStrat.toString() + klass.getPackage().getName() + "\"", hash );

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

            Assert.assertEquals((long) 3, l);
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

            Assert.assertEquals((long) 3, l);
        }

        @Test
        public void testServiceTimer() throws
                MalformedObjectNameException,
                AttributeNotFoundException,
                MBeanException,
                ReflectionException,
                InstanceNotFoundException {

            Timer t = metricsService.newTimer( this.getClass(), "name1", "scope1", TimeUnit.MILLISECONDS, TimeUnit.MILLISECONDS );

            TimerContext tc = t.time();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {}
            tc.stop();

            Assert.assertEquals(1L, ((Long) getAttribute(this.getClass(), "name1", "scope1", "Count")).longValue());
            Assert.assertTrue(((Double) getAttribute(this.getClass(), "name1", "scope1", "Mean")).doubleValue() > 0);

            t.update(1000L, TimeUnit.MILLISECONDS);

            Assert.assertEquals(2L, ((Long) getAttribute(this.getClass(), "name1", "scope1", "Count")).longValue());
            Assert.assertTrue(((Double) getAttribute(this.getClass(), "name1", "scope1", "Mean")).doubleValue() > 0);
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
            Assert.assertEquals((long) 2, l);

            l = (Long) getAttribute( this.getClass(), "meter2", "scope1", "Count" );
            Assert.assertEquals((long) 4, l);
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
            Assert.assertEquals((long) 2, l);

            l = (Long) getAttribute( this.getClass(), "meter2", "scope1", "Count" );
            Assert.assertEquals((long) 4, l);

            l = (Long) getAttribute( this.getClass(), MeterByCategorySum.ALL, "scope1", "Count" );
            Assert.assertEquals((long) 6, l);
        }

        @Test
        public void testTimerByCategory() throws
                MalformedObjectNameException,
                AttributeNotFoundException,
                MBeanException,
                ReflectionException,
                InstanceNotFoundException {

            TimerByCategory t = metricsService.newTimerByCategory( this.getClass(), "scope1", TimeUnit.MILLISECONDS, TimeUnit.MILLISECONDS );

            TimerContext tc = t.time("key1");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {}
            tc.stop();

            Assert.assertEquals(1L, ((Long) getAttribute(this.getClass(), "key1", "scope1", "Count")).longValue());
            Assert.assertTrue(((Double) getAttribute(this.getClass(), "key1", "scope1", "Mean")).doubleValue() > 0);

            tc = t.time("key2");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {}
            tc.stop();

            Assert.assertEquals(1L, ((Long) getAttribute(this.getClass(), "key2", "scope1", "Count")).longValue());
            Assert.assertTrue(((Double) getAttribute(this.getClass(), "key2", "scope1", "Mean")).doubleValue() > 0);

            t.update("key1", 1000L, TimeUnit.MILLISECONDS);

            Assert.assertEquals(2L, ((Long) getAttribute(this.getClass(), "key1", "scope1", "Count")).longValue());
            Assert.assertTrue(((Double) getAttribute(this.getClass(), "key1", "scope1", "Mean")).doubleValue() > 0);

            t.update("key2", 1000L, TimeUnit.MILLISECONDS);

            Assert.assertEquals(2L, ((Long) getAttribute(this.getClass(), "key2", "scope1", "Count")).longValue());
            Assert.assertTrue(((Double) getAttribute(this.getClass(), "key2", "scope1", "Mean")).doubleValue() > 0);
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

        @Test
        public void testServiceEnabledDisabled()
                throws
                MalformedObjectNameException,
                AttributeNotFoundException,
                MBeanException,
                ReflectionException,
                InstanceNotFoundException {

            metricsService.setEnabled(false);
            Assert.assertFalse(metricsService.isEnabled());

            metricsService.setEnabled(true);
            Assert.assertTrue(metricsService.isEnabled());
        }
    }
}
