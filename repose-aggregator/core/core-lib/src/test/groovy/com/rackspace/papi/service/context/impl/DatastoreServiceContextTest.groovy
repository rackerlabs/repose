package com.rackspace.papi.service.context.impl
import com.rackspace.papi.service.ServiceRegistry
import com.rackspace.papi.service.context.ServiceContext
import com.rackspace.papi.service.datastore.impl.DatastoreServiceImpl
import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.nullValue
import static org.hamcrest.collection.IsArrayContaining.hasItemInArray
import static org.junit.Assert.assertThat
/**
 * Created by eric7500 on 6/19/14.
 */
public class DatastoreServiceContextTest {

    @Test
    public void testRegisterNotNull() {
        DatastoreServiceContext dsc = new DatastoreServiceContext(null,new ServiceRegistry(),null);
        int initialSize = dsc.registry.boundServiceContexts.size();
        dsc.register();
        ServiceContext[] registries = dsc.registry.boundServiceContexts.toArray();
        assertThat(dsc.registry.boundServiceContexts.size(),equalTo(initialSize+1));
        assertThat(registries,hasItemInArray(dsc));
    }

    @Test(expected = NullPointerException)
    public void testRegisterNull() {
        DatastoreServiceContext dsc = new DatastoreServiceContext(null,null,null);
        int initialSize = dsc.registry.boundServiceContexts.size(); //this line should produce the exception
        dsc.register();
    }

    @Test
    public void testNullDatastoreService() {
        DatastoreServiceContext dsc = new DatastoreServiceContext(null,null,null);
        assertThat(dsc.getService(),nullValue());
    }


    @Test
    public void testNotNullDatastoreService() {
        DatastoreServiceImpl dsi = new DatastoreServiceImpl();
        DatastoreServiceContext dsc = new DatastoreServiceContext(dsi,null,null);
        assertThat(dsc.getService(),equalTo(dsi));
    }

    @Test
    public void testServiceName() {
        DatastoreServiceContext dsc = new DatastoreServiceContext(null,null,null);
        assertThat(dsc.getServiceName(),equalTo("powerapi:/datastore/service"));
    }

    @Test
    public void testContextInitialized() {
        DatastoreServiceContext dsc = new DatastoreServiceContext(null,new ServiceRegistry(),null);
        int initialSize = dsc.registry.boundServiceContexts.size();
        dsc.contextInitialized(null);
        ServiceContext[] registries = dsc.registry.boundServiceContexts.toArray();
        assertThat(dsc.registry.boundServiceContexts.size(),equalTo(initialSize+1));
        assertThat(registries,hasItemInArray(dsc));
    }


}
