package com.rackspace.papi.components.datastore.impl.replicated.data;

import com.rackspace.papi.components.datastore.impl.replicated.data.Message.KeyValue;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.Serializable;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class MessageTest {

    public static class WhenCreatingMessages {

        @Test
        public void shouldStoreBasicData() {
            String key = "key";
            int ttl = 42;
            String data = "0, 1, 2";

            Message message = new Message(Operation.JOINING, key, data, ttl);

            assertEquals(Operation.JOINING, message.getOperation());
            assertEquals("*", message.getTargetId());
            assertEquals(key, message.getKey());

            assertEquals("Should return our data", data, (String)message.getData());
        }

        @Test
        public void shouldStoreComplexData() {
            final int ROWS = 10;
            final int COLS = 20;
            
            Operation[] operation = new Operation[ROWS];
            String[] keys = new String[ROWS];
            int[] ttl = new int[ROWS];
            Serializable[] data = new Serializable[ROWS];

            for (int i = 0; i < ROWS; i++) {
                keys[i] = "key" + i;
                ttl[i] = i;
                operation[i] = Operation.JOINING;
                data[i] = new byte[COLS];

                data[i] = Integer.toString(i);
            }

            Message message = new Message(operation, keys, data, ttl);

            assertEquals(Operation.JOINING, message.getOperation());
            assertEquals("*", message.getTargetId());
            assertEquals(ROWS, message.getValues().size());
            
            Iterator<KeyValue> iterator = message.getValues().iterator();
            int i = 0;
            while(iterator.hasNext()) {
                KeyValue value = iterator.next();

                assertEquals(keys[i], value.getKey());
                assertEquals(ttl[i], value.getTtl());
                
                assertEquals("Should return our data", data[i], value.getData());
                i++;
            }
        }
    }
}
