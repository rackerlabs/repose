package org.openrepose.services.datastore.api.distributed;

import org.openrepose.services.datastore.api.Patch;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: adrian
 * Date: 1/22/14
 * Time: 12:47 PM
 */
public interface SerializablePatch<T> extends Patch<T>, Serializable {
}
