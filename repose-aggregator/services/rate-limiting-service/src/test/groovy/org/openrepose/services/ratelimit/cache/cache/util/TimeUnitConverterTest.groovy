package org.openrepose.services.ratelimit.cache.cache.util

import com.rackspace.repose.service.limits.schema.TimeUnit
import org.openrepose.services.ratelimit.cache.util.TimeUnitConverter
import spock.lang.Specification

class TimeUnitConverterTest extends Specification {
    def "fromSchemaTypeToConcurrent_shouldConvertTimeUnits"() {
        when:
        java.util.concurrent.TimeUnit returnedUnit = TimeUnitConverter.fromSchemaTypeToConcurrent(unitFromSchema)

        then:
        returnedUnit == unitFromConcurrent

        where:
        unitFromSchema  | unitFromConcurrent
        TimeUnit.SECOND | java.util.concurrent.TimeUnit.SECONDS
        TimeUnit.MINUTE | java.util.concurrent.TimeUnit.MINUTES
        TimeUnit.HOUR   | java.util.concurrent.TimeUnit.HOURS
        TimeUnit.DAY    | java.util.concurrent.TimeUnit.DAYS
    }
}
