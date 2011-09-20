package com.rackspace.papi.service.datastore.hash;

import java.math.BigInteger;

public interface HashProvider {

    BigInteger maxValue();

    byte[] hash(String key);
}
