package com.rackspace.papi.commons.util.http.header;


import com.rackspace.papi.commons.util.http.HeaderConstant;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class SplitableHeaderUtil {

    // Headers available for splitting (According to RFC2616
    public static final String[] DEFAULT_SPLIT = {"accept", "accept-charset", "accept-language", "allow",
            "cache-control", "connection", "content-encoding", "content-language", "expect", "pragma",
            "proxy-authenticate", "te", "trailer", "transfer-encoding", "upgrade", "www-authenticate",
            "warning"};

    public static final Comparator<String> CASE_INSENSITIVE_COMPARE = new Comparator<String>() {
        @Override
        public int compare(String s1, String s2) {
            return s1.toLowerCase().compareTo(s2.toLowerCase());
        }
    };


    private Set<String> splitableHeaders;

    public SplitableHeaderUtil() {

        setDefaultSplitable();

    }

    public SplitableHeaderUtil(HeaderConstant... constant) {
        setDefaultSplitable();

        for (HeaderConstant ct : constant) {
            splitableHeaders.add(ct.toString());
        }

    }

    public SplitableHeaderUtil(HeaderConstant[]... constant) {
        setDefaultSplitable();

        for (HeaderConstant[] cts : constant) {
            for (HeaderConstant ct : cts) {
                splitableHeaders.add(ct.toString());
            }
        }

    }

    private void setDefaultSplitable() {
        // Using a set which us so as to pass a comparator
        splitableHeaders = new TreeSet<String>(CASE_INSENSITIVE_COMPARE);
        splitableHeaders.addAll(Arrays.asList(DEFAULT_SPLIT));
    }


    public boolean isSplitable(String st) {

        return splitableHeaders.contains(st.toLowerCase());
    }
}

