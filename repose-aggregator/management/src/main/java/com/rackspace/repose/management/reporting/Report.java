package com.rackspace.repose.management.reporting;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: fran
 * Date: Oct 24, 2012
 * Time: 12:28:56 PM
 */
@XmlRootElement(name="report")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Report {

    private String lastResetDate;
    private String total400sReposeToClient;
    private String total500sReposeToClient;
    private List<Destination> destinations;

    public Report() {
    }

    @XmlElement
    public String getLastResetDate() {
        return lastResetDate;
    }

    public void setLastResetDate(String lastResetDate) {
        this.lastResetDate = lastResetDate;
    }

    @XmlElement
    public String getTotal400sReposeToClient() {
        return total400sReposeToClient;
    }

    public void setTotal400sReposeToClient(String total400sReposeToClient) {
        this.total400sReposeToClient = total400sReposeToClient;
    }

    @XmlElement
    public String getTotal500sReposeToClient() {
        return total500sReposeToClient;
    }

    public void setTotal500sReposeToClient(String total500sReposeToClient) {
        this.total500sReposeToClient = total500sReposeToClient;
    }

    @XmlElement
    public List<Destination> getDestinations() {
        return destinations;
    }

    public void setDestinations(List<Destination> destinations) {
        this.destinations = destinations;
    }
}
