package com.rackspace.papi.httpx.parser;

import com.rackspace.httpx.MessageDetail;
import com.rackspace.httpx.RequestHeadDetail;

import java.io.InputStream;
import java.util.List;

public interface Parser<T> {
    public InputStream parse(T input, List<MessageDetail> messageFidelity, List<RequestHeadDetail> headFidelity, List<String> headersFidelity);  
}
