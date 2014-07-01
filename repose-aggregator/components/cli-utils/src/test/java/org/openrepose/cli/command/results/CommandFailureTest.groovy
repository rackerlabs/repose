package org.openrepose.cli.command.results

import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat
/**
 * Created by eric7500 on 7/1/14.
 */
class CommandFailureTest {

    @Test
    public void testMessage() {
        CommandFailure testFailure = new CommandFailure(0, "testMessage");
        assertThat(testFailure.getStringResult(),equalTo("testMessage"));
    }

    @Test
    public void testStatus() {
        CommandFailure testFailure = new CommandFailure(0, "testMessage");
        assertThat(testFailure.getStatusCode().toInteger(),equalTo(new Integer(0)));
    }

}

