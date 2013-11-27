package com.rackspace.papi.commons.util.logging.apache;

import java.util.Map;
import java.util.TreeMap;

public enum LogFormatArgument {

    PERCENT("%"),
    REMOTE_ADDRESS("a"),
    LOCAL_ADDRESS("A"),
    RESPONSE_CLF_BYTES("b"),
    RESPONSE_BYTES("B"),
    REMOTE_HOST("h"),
    REQUEST_METHOD("m"),
    CANONICAL_PORT("p"),
    QUERY_STRING("q"),
    TIME_RECEIVED("t"),
    STATUS_CODE("s"),
    REMOTE_USER("u"),
    URL_REQUESTED("U"),
    REQUEST_HEADER("i"),
    REQUEST_LINE("r"),
    REQUEST_PROTOCOL("H"),
    RESPONSE_HEADER("o"),
    STRING("STRING"),
    ERROR_MESSAGE("M"),
    RESPONSE_TIME_MICROSECONDS("D"),
    RESPONSE_TIME_SECONDS("T");

    public static LogFormatArgument fromString(String st) {
        return ReverseLookup.LOOKUP_MAP.get(st);
    }

    private final String argument;

    private LogFormatArgument(String argument) {
        this.argument = argument;

        ReverseLookup.addLookup(argument, this);
    }

    @Override
    public String toString() {
        return argument;
    }

    private static final class ReverseLookup {

        public static final Map<String, LogFormatArgument> LOOKUP_MAP = new TreeMap<String, LogFormatArgument>();

        private ReverseLookup() {
        }

        public static void addLookup(String st, LogFormatArgument arg) {
            LOOKUP_MAP.put(st, arg);
        }
    }
}
