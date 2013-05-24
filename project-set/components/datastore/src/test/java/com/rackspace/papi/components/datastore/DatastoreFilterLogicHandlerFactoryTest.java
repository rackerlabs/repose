package com.rackspace.papi.components.datastore;

import com.rackspace.papi.components.datastore.hash.HashRingDatastore;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.model.*;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class DatastoreFilterLogicHandlerFactoryTest {

    public static class TestParent {
        DatastoreFilterLogicHandlerFactory datastoreFilterLogicHandlerFactory;
        MutableClusterView clusterView;
        HashRingDatastore hashRingDatastore;
        ReposeInstanceInfo reposeInstanceInfo;
        SystemModel systemModel;

        List<ReposeCluster> reposeClusterList;
        ReposeCluster reposeCluster;

        FilterList filterList;
        List<Filter> filters;
        Filter filter;

        ServicesList serviceList;
        List<Service> services;
        Service service;

        @Before
        public void setUp() throws Exception {
            serviceList = mock(ServicesList.class);
            services = new ArrayList<Service>();
            service = mock(Service.class);
            services.add(service);

            filterList = mock(FilterList.class);
            filters = new ArrayList<Filter>();
            filter = mock(Filter.class);
            filters.add(filter);

            reposeCluster = mock(ReposeCluster.class);
            reposeClusterList = new ArrayList<ReposeCluster>();
            reposeClusterList.add(reposeCluster);

            systemModel = mock(SystemModel.class);
            clusterView = mock(MutableClusterView.class);
            hashRingDatastore = mock(HashRingDatastore.class);
            reposeInstanceInfo = mock(ReposeInstanceInfo.class);
            datastoreFilterLogicHandlerFactory =
                    new DatastoreFilterLogicHandlerFactory(clusterView, hashRingDatastore, reposeInstanceInfo);
        }

        @Test(expected = IllegalArgumentException.class)
        public void shouldThrowIllegalArgumentException() {
            when(systemModel.getReposeCluster()).thenReturn(reposeClusterList);

            when(reposeCluster.getFilters()).thenReturn(filterList);
            when(filterList.getFilter()).thenReturn(filters);
            when(filter.getName()).thenReturn("dist-datastore");

            when(reposeCluster.getServices()).thenReturn(serviceList);
            when(serviceList.getService()).thenReturn(services);
            when(service.getName()).thenReturn("distributed-datastore");

            datastoreFilterLogicHandlerFactory.updateClusterMembers(systemModel);
        }
    }
}
