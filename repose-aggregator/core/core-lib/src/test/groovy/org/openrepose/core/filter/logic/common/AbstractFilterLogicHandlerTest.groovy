package org.openrepose.core.filter.logic.common
import org.openrepose.core.filter.logic.impl.SimplePassFilterDirector
import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat

/**
 * Created by eric7500 on 6/18/14.
 */
public class AbstractFilterLogicHandlerTest {

    @Test
    public void testHandleRequest() {
        AbstractFilterLogicHandler aflh = new AbstractFilterLogicHandler();
        assertThat(aflh.handleRequest(null,null),equalTo(SimplePassFilterDirector.SINGLETON_INSTANCE));
    }

    @Test
    public void testHandleResponse() {
        AbstractFilterLogicHandler aflh = new AbstractFilterLogicHandler();
        assertThat(aflh.handleResponse(null,null),equalTo(SimplePassFilterDirector.SINGLETON_INSTANCE));
    }
}