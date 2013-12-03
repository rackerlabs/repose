package com.rackspace.papi.service.httpclient.impl

import com.rackspace.papi.service.httpclient.config.PoolType


class PoolTypeHelper {

    private final static int CONN_TIMEOUT = 30000;
    private final static int MAX_HEADERS = 100;
    private final static int MAX_LINE = 50;
    private final static int SOC_TIMEOUT = 40000;
    private final static boolean TCP_NODELAY = false;
    private final static int SOC_BUFF_SZ = 1023;
    private final static int MAX_PER_ROUTE = 50;
    private final static int MAX_TOTAL = 300;

    static List<PoolType> createListOfPools(int totalPools, int defaultPool) {

        List<PoolType> pools = new ArrayList<PoolType>();

        for (i in 1..totalPools) {

            PoolType poolType = new PoolType();
            poolType.setHttpConnectionMaxHeaderCount(MAX_HEADERS);
            poolType.setHttpConnectionMaxLineLength(MAX_LINE);
            poolType.setHttpConnectionMaxStatusLineGarbage(10);
            poolType.setHttpConnectionTimeout(CONN_TIMEOUT);
            poolType.setHttpConnManagerMaxPerRoute(MAX_PER_ROUTE);
            poolType.setHttpConnManagerMaxTotal(MAX_TOTAL);
            poolType.setHttpSocketBufferSize(SOC_BUFF_SZ);
            poolType.setHttpSocketTimeout(SOC_TIMEOUT);
            poolType.setHttpTcpNodelay(TCP_NODELAY);
            poolType.setId("pool"+ i);

            if (i == defaultPool) {
                poolType.setDefault(true)
            }

            pools.add(poolType);
        }

        return pools
    }

}
