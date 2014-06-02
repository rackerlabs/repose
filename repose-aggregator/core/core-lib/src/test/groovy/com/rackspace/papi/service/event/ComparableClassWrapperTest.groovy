package com.rackspace.papi.service.event

import spock.lang.Specification


class ComparableClassWrapperTest extends Specification {

    def "If wrapped class not null, hash code should be (7 89) + that classes's hash code"() {
        given:
        ComparableClassWrapper<Integer> wrap = new ComparableClassWrapper<Integer>(Integer);

        when:
        {}

        then:
        wrap.hashCode() == (7 *89) + Integer.hashCode();
    }

    def "If wrapped class is null, hash code should be (7 89)"() {
        given:
        ComparableClassWrapper<Integer> wrap = new ComparableClassWrapper<>();

        when:
        {}

        then:
        wrap.hashCode() == (7 *89);
    }

    def "If equals takes as an argument an instance of a different class, return false"() {

        given:
        ComparableClassWrapper<List> wrap = new ComparableClassWrapper<List>(LinkedList);

        when:
        List list = new LinkedList();

        then:
        !wrap.equals(list)
    }

    def "If equals takes as an argument an instance of the same class, but they're different values return false"() {

        given:
        ComparableClassWrapper<List> wrap = new ComparableClassWrapper<List>(LinkedList);

        when:
        ComparableClassWrapper<List> wrap2 = new ComparableClassWrapper<List>(AbstractList);

        then:
        !wrap.equals(wrap2)


    }

}
