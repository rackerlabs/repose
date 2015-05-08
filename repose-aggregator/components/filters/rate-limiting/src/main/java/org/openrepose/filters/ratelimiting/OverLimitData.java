/**
 * 
 */
package org.openrepose.filters.ratelimiting;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.core.services.ratelimit.exception.OverLimitException;

/**
 * @author ganesh
 *
 */
public class OverLimitData {

    private String user;
    
    private String groups;
    
    private Date nextAvailableTime;
    
    private int responseCode;
    
    private int dataStoreWarnLimit;
    
    private int currentLimitAmount;
    
    private String configuredLimit;
    
    private HttpServletRequest request;
    
    /**
     * @param exception
     * @param dataStoreWarnLimit
     * @param request
     * @param responseCode
     */
    public OverLimitData(OverLimitException exception, int dataStoreWarnLimit, HttpServletRequest request, int responseCode) {
        this.user = exception.getUser();
        this.nextAvailableTime = exception.getNextAvailableTime();
        this.currentLimitAmount = exception.getCurrentLimitAmount();
        this.configuredLimit = exception.getConfiguredLimit();
        this.groups = request.getHeader(PowerApiHeader.GROUPS.toString());
        this.request = request;
        this.responseCode = responseCode;
        this.dataStoreWarnLimit = dataStoreWarnLimit;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @return the groups
     */
    public String getGroups() {
        return groups;
    }

    /**
     * @return the nextAvailableTime
     */
    public Date getNextAvailableTime() {
        return nextAvailableTime;
    }

    /**
     * @return the responseCode
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * @return the currentLimitAmount
     */
    public int getCurrentLimitAmount() {
        return currentLimitAmount;
    }

    /**
     * @return the configuredLimit
     */
    public String getConfiguredLimit() {
        return configuredLimit;
    }

    /**
     * @return the request
     */
    public HttpServletRequest getRequest() {
        return request;
    }

    /**
     * @return the dataStoreWarnLimit
     */
    public int getDataStoreWarnLimit() {
        return dataStoreWarnLimit;
    }
}
