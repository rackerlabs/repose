package org.openrepose.filters.translation.httpx.parser;

import org.openrepose.core.httpx.MessageDetail;

import java.io.InputStream;
import java.util.List;

public interface Parser<T, U> {
    InputStream parse(T input, List<MessageDetail> messageFidelity, List<U> headFidelity, List<String> headersFidelity, boolean jsonProcessing);
}
