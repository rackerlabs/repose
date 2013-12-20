package com.rackspace.papi.servlet.boot.service.config;

import com.rackspace.papi.service.config.impl.ParserListenerPair;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 6/23/11
 * Time: 2:12 PM
 */
@RunWith(Enclosed.class)
public class ParserListenerPairTest {
    public static class WhenCreatingNewInstancesWithNullParameters {
        private ParserListenerPair pair;

        @Test
        public void shouldNotVomitWhenGivenNulls() {
            new ParserListenerPair(null, null,null,null);
        }

        @Test
        public void shouldNotVomitWhenGettingListener() {
            pair = new ParserListenerPair(null, null,null,null);

            pair.getListener();
        }
    }
    // TODO: Need to unit test.  I removed previous tests because they were testing equals and hashode. However,
    // I refactored the ParserListenerPair and a couple of other classes that were accessing it such that we
    // no longer needed to override hashCode and equals.  However, you guys can take a look and if you don't like
    // the refactoring I can put it all back :)
}
