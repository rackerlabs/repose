package org.openrepose.core.services.event
import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.not
import static org.junit.Assert.*

public class ComparableClassWrapperTest {

    @Test
    public void testNonNullHash() {
        Integer num = new Integer(5);
        ComparableClassWrapper<Number> wrap = new ComparableClassWrapper<Number>(num.getClass());

        assertThat(wrap.hashCode(), equalTo((7 *89) + num.getClass().hashCode()))
    }

    @Test
    public void testNullWrappedHash() {
        ComparableClassWrapper<Enum> wrap = new ComparableClassWrapper<Enum>(null);
        assertThat(wrap.hashCode(), equalTo(7 *89));
    }

    @Test
    public void testEqualsDifferentClass() {
        Integer num = new Integer(5);
        ComparableClassWrapper<Number> wrap = new ComparableClassWrapper<Number>(num.getClass());
        LinkedList<Integer> list = new LinkedList<Integer>();
        assertThat(wrap, not(list));
    }

    @Test
    public void test4() {
        Integer num = new Integer(5);
        ComparableClassWrapper<Number> wrap = new ComparableClassWrapper<Number>(num.getClass());
        Double num2 = new Double(5.0);
        ComparableClassWrapper<Number> wrap2 = new ComparableClassWrapper<Number>(num2.getClass());
        assertThat(wrap, not(wrap2))
    }
}
