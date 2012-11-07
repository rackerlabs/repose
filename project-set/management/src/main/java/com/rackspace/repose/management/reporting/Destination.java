package com.rackspace.repose.management.reporting;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by IntelliJ IDEA.
 * User: fran
 * Date: Oct 24, 2012
 * Time: 2:01:25 PM
 */
@XmlRootElement(name="destination")
@XmlAccessorType(XmlAccessType.FIELD)
public class Destination {

    @XmlElement
    private String destinationId;

    @XmlElement
    private String totalRequests;

    @XmlElement
    private String total400s;

    @XmlElement
    private String total500s;

    @XmlElement
    private String responseTimeInMillis;

    @XmlElement
    private String throughputInSeconds;

    public String getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(String destinationId) {
        this.destinationId = destinationId;
    }

    public String getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(String totalRequests) {
        this.totalRequests = totalRequests;
    }

    public String getTotal400s() {
        return total400s;
    }

    public void setTotal400s(String total400s) {
        this.total400s = total400s;
    }

    public String getTotal500s() {
        return total500s;
    }

    public void setTotal500s(String total500s) {
        this.total500s = total500s;
    }

    public String getResponseTimeInMillis() {
        return responseTimeInMillis;
    }

    public void setResponseTimeInMillis(String responseTimeInMillis) {
        this.responseTimeInMillis = responseTimeInMillis;
    }

    public String getThroughputInSeconds() {
        return throughputInSeconds;
    }

    public void setThroughputInSeconds(String throughputInSeconds) {
        this.throughputInSeconds = throughputInSeconds;
    }
}
