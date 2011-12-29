package com.rackspace.papi.commons.util.http;

/**
 * The HttpHeader interface represents a strongly-typed, normalized way of
 * describing an HttpHeader and its key.
 */
public interface HeaderConstant {

    /**
     * Header keys are always represented as their human readable variant.
     *
     * e.g. HeaderEnum.ACCEPT.getHeaderKey() must return "Accept"
     *
     * @return
     */
    String getHeaderKey();

    /**
     * An HttpHeader toString() method will always return the lower case variant
     * of the header key.
     * 
     * e.g. HeaderEnum.ACCEPT.toString() must return "accept"
     * 
     * @return 
     */
    @Override
    String toString();

    /**
     * This method provides a way to test equality of a given string against 
     * the key of the header. This method abstracts the character case issue that
     * plagues header key matching.
     * 
     * This is here because of a deficiency in the java enumeration contract.
     * The equals method is marked final for all enumeration types.
     * 
     * @param s
     * @return 
     */
    boolean matches(String s);
}
