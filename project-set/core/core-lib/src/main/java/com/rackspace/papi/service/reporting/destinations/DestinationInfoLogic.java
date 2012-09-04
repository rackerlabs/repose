package com.rackspace.papi.service.reporting.destinations;


public class DestinationInfoLogic extends DestinationInfoStore implements DestinationInfo {

    private static final int INT_ONE = 1;
    private static final long LONG_ZERO = 0l;
    private static final long LONG_ONE = 1l;
    private static final double DOUBLE_ZERO = 0.0d;
    private static final double DOUBLE_THOUSAND = 1000d;

    public DestinationInfoLogic(String destinationAuthority) {
        super(destinationAuthority);
    }

    private DestinationInfoLogic(DestinationInfoLogic destinationInfoLogic) {
        super(destinationInfoLogic);    
    }

    @Override
    public void incrementRequestCount() {
        super.setTotalRequests(super.getTotalRequests() + INT_ONE);
    }

    @Override
    public void incrementResponseCount() {
        super.setTotalResponses(super.getTotalResponses() + INT_ONE);
    }

    @Override
    public void incrementStatusCodeCount(int statusCode) {
        Long value = super.getStatusCodeCounts().get(statusCode);

        if (value != null) {
            super.getStatusCodeCounts().put(statusCode, ++value);
        } else {
            super.getStatusCodeCounts().put(statusCode, LONG_ONE);
        }
    }

    @Override
    public void accumulateResponseTime(long responseTime) {
        super.setAccumulatedResponseTime(super.getAccumulatedResponseTime() + responseTime);
    }

    @Override
    public String getDestinationId(){
        return super.getDestinationId();
    }

    @Override
    public long getTotalRequests() {
        return super.getTotalRequests();
    }

    @Override
    public long getTotalStatusCode(int statusCode) {
        Long count = super.getStatusCodeCounts().get(statusCode);

        if (count != null) {
            return count;
        } else {
            return LONG_ZERO;
        }
    }

    @Override
    public double getAverageResponseTime() {
        double averageResponseTime = (double)super.getTotalResponses()/super.getAccumulatedResponseTime();

        if (Double.isNaN(averageResponseTime)) {
            return DOUBLE_ZERO;
        } else {
            return averageResponseTime;
        }        
    }

    @Override
    public double getThroughput() {
        double throughput = (double)super.getTotalResponses()/elapsedTimeInSeconds();

        if (Double.isNaN(throughput)) {
            return DOUBLE_ZERO;
        } else {
            return throughput;
        }        
    }

    public double elapsedTimeInSeconds() {
        return (System.currentTimeMillis() - super.getStartTime())/DOUBLE_THOUSAND;
    }

    @Override
    public DestinationInfo copy() {
        return new DestinationInfoLogic(this);
    }
}
