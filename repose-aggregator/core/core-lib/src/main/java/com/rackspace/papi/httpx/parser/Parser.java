package com.rackspace.papi.httpx.parser;

import com.rackspace.httpx.MessageDetail;

import java.io.InputStream;
import java.util.List;

public interface Parser<T, U> {
    InputStream parse(T input, List<MessageDetail> messageFidelity, List<U> headFidelity, List<String> headersFidelity, boolean jsonProcessing);
}
