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
@XmlAccessorType(XmlAccessType.FIELD)
public class Report {

    @XmlElement
    protected String lastResetDate;

    @XmlElement
    protected String total400sReposeToClient;

    @XmlElement
    protected String total500sReposeToClient;

    @XmlElement
    protected List<Destination> destinations;

    public Report() {
    }

    public String getLastResetDate() {
        return lastResetDate;
    }

    public void setLastResetDate(String lastResetDate) {
        this.lastResetDate = lastResetDate;
    }

    public String getTotal400sReposeToClient() {
        return total400sReposeToClient;
    }

    public void setTotal400sReposeToClient(String total400sReposeToClient) {
        this.total400sReposeToClient = total400sReposeToClient;
    }

    public String getTotal500sReposeToClient() {
        return total500sReposeToClient;
    }

    public void setTotal500sReposeToClient(String total500sReposeToClient) {
        this.total500sReposeToClient = total500sReposeToClient;
    }

    public List<Destination> getDestinations() {
        return destinations;
    }

    public void setDestinations(List<Destination> destinations) {
        this.destinations = destinations;
    }
}
