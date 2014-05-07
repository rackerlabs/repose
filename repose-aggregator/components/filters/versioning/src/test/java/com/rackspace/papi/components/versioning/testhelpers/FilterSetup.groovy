package com.rackspace.papi.components.versioning.testhelpers

/**
 * Created by dimi5963 on 5/1/14.
 */
public interface FilterSetup<T, E> {
    def buildFakeConfigXml(List<E> configList)
    T configureFilter(List<E> mappingList)


}