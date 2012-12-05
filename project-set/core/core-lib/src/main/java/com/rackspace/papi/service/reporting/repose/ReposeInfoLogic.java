package com.rackspace.papi.service.reporting.repose;

import com.google.common.base.Objects;
import com.rackspace.papi.service.reporting.StatusCodeResponseStore;

import java.util.Map;

public class ReposeInfoLogic implements ReposeInfo {

    private static final long LONG_ZERO = 0l;
    private static final long LONG_ONE = 1l;
    private static final int INT_ONE = 1;
    private static final double DOUBLE_ZERO = 0.0d;
    private static final int RESPONSE_CODE_SEPERATOR = 100;
    private ReposeInfoStore dataStore;

    public ReposeInfoLogic() {
        dataStore = new ReposeInfoStore();
    }

    private ReposeInfoLogic(ReposeInfoLogic reposeInfoLogic) {
        dataStore = new ReposeInfoStore(reposeInfoLogic.dataStore);
    }

    public long getTotalRequests() {
        return dataStore.getTotalRequests();
    }

    public long getTotalResponses() {
        return dataStore.getTotalResponses();
    }

    public long getAccumulatedRequestSize() {
        return dataStore.getAccumulatedRequestSize();
    }

    public long getAccumulatedResponseSize() {
        return dataStore.getAccumulatedResponseSize();
    }

    public Map<Integer, StatusCodeResponseStore> getStatusCodeCounts() {
        return dataStore.getStatusCodeCounts();
    }

    @Override
    public long getTotalStatusCode(int statusCode) {

       Long count = LONG_ZERO;
       
       for(Integer code: dataStore.getStatusCodeCounts().keySet()){
          
          if(code%statusCode < RESPONSE_CODE_SEPERATOR){
             count += dataStore.getStatusCodeCounts().get(code).getTotalCount();
          }
       }
       
       return count;
    }

    @Override
    public void incrementStatusCodeCount(int statusCode, long time) {
        StatusCodeResponseStore value = dataStore.getStatusCodeCounts().get(statusCode);

        if (value != null) {
            dataStore.getStatusCodeCounts().put(statusCode, value.update(1, time));
        } else {
            dataStore.getStatusCodeCounts().put(statusCode, new StatusCodeResponseStore(1, time));
        }
    }

    @Override
    public void incrementRequestCount() {
        dataStore.setTotalRequests(dataStore.getTotalRequests() + INT_ONE);
    }

    @Override
    public void incrementResponseCount() {
        dataStore.setTotalResponses(dataStore.getTotalResponses() + INT_ONE);
    }
    
    @Override
    public void processRequestSize(long requestSize) {
        dataStore.processRequestSize(requestSize);
    }

    @Override
    public void processResponseSize(long responseSize) {
        dataStore.processResponseSize(responseSize);
    }

    @Override
    public long getMinimumRequestSize() {
        return dataStore.getMinRequestSize();
    }

    @Override
    public long getMaximumRequestSize() {
        return dataStore.getMaxRequestSize();
    }

    @Override
    public long getMinimumResponseSize() {
        return dataStore.getMinResponseSize();
    }

    @Override
    public long getMaximumResponseSize() {
        return dataStore.getMaxResponseSize();
    }

    @Override
    public double getAverageRequestSize() {
        double averageRequestSize = (double)dataStore.getAccumulatedResponseSize()/(double)dataStore.getTotalRequests();

        if (Double.isNaN(averageRequestSize)) {
            return DOUBLE_ZERO;
        } else {
            return averageRequestSize;
        }
    }

    @Override
    public double getAverageResponseSize() {
        double averageResponseSize = (double)dataStore.getAccumulatedResponseSize()/(double)dataStore.getTotalResponses();

        if (Double.isNaN(averageResponseSize)) {
            return DOUBLE_ZERO;
        } else {
            return averageResponseSize;
        }
    }

    @Override
    public ReposeInfo copy() {
        return new ReposeInfoLogic(this);
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o instanceof ReposeInfoLogic) {
            ReposeInfoLogic other = (ReposeInfoLogic) o;

            return Objects.equal(this.dataStore, other.dataStore);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dataStore);
    }
}
