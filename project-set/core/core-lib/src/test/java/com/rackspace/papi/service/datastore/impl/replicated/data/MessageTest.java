package com.rackspace.papi.service.datastore.impl.replicated.data;

import com.rackspace.papi.service.datastore.impl.replicated.data.Operation;
import com.rackspace.papi.service.datastore.impl.replicated.data.Message;
import com.rackspace.papi.service.datastore.impl.replicated.data.Message.KeyValue;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class MessageTest {

    public static class WhenCreatingMessages {

        @Before
        public void setUp() {
        }

        @Test
        public void shouldStoreBasicData() {
            String key = "key";
            int ttl = 42;
            byte[] data = new byte[]{0, 1, 2};

            Message message = new Message(Operation.JOINING, key, data, ttl);

            assertEquals(Operation.JOINING, message.getOperation());
            assertEquals("*", message.getTargetId());
            assertEquals(key, message.getKey());

            for (int i = 0; i < message.getData().length; i++) {
                assertEquals("Should return our data", data[i], message.getData()[i]);
            }
        }

        @Test
        public void shouldStoreComplexData() {
            final int ROWS = 10;
            final int COLS = 20;
            
            String[] keys = new String[ROWS];
            int[] ttl = new int[ROWS];
            byte[][] data = new byte[ROWS][];

            for (int i = 0; i < ROWS * COLS; i++) {
                int row = i % ROWS;
                int col = i / ROWS;

                if (data[row] == null) {
                    keys[row] = "key" + i;
                    ttl[row] = i;
                    data[row] = new byte[COLS];
                }

                data[row][col] = (byte)i;
            }

            Message message = new Message(Operation.JOINING, keys, data, ttl);

            assertEquals(Operation.JOINING, message.getOperation());
            assertEquals("*", message.getTargetId());
            assertEquals(ROWS, message.getValues().length);

            for (int i = 0; i < message.getValues().length; i++) {
                KeyValue value = message.getValues()[i];
                assertEquals(keys[i], value.getKey());
                assertEquals(ttl[i], value.getTtl());
                
                for (int j = 0; j < value.getData().length; j++) {
                    assertEquals("Should return our data", data[i][j], value.getData()[j]);
                }
            }

        }
    }
}
