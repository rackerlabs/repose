package org.openrepose.commons.utils;


import org.junit.Test;
import org.openrepose.commons.utils.ArrayUtilities;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

public class ArrayUtilitiesTest {

    @Test
    public void testNullSafeCopyNull() {
        assertThat(ArrayUtilities.nullSafeCopy((Object[])null), equalTo(null));
    }

    @Test
    public void testNullSafeCopyNonNull() {
        String [] array = {"element1", "element2"};
        assertThat(ArrayUtilities.nullSafeCopy(array), equalTo(array));
    }

    @Test
    public void testNullSafeCopyNullByte() {
        assertThat(ArrayUtilities.nullSafeCopy((byte[])null), equalTo(null));
    }

    @Test
    public void testNullSafeCopyNonNullByte(){
        byte [] array = "array".getBytes();
        assertThat(ArrayUtilities.nullSafeCopy(array), equalTo(array));
    }

}
