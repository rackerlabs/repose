# Creating a new filter

Use an existing filter, say for example the header-translation filter as an example.

I copied the existing directory structure and then renamed the artifacts in the pom, and went through the dependencies
to be proper.

Add it to the higher level up pom's module list `repose-aggregator/components/filters/pom.xml` so it is part of the project. Also add
the new artifact as a dependencie of this same pom so it can be used.

Add it to the web-fragment.xml as a filter:
```xml
    <filter>
        <filter-name>Example name</filter-name>
        <filter-class>com.rackspace.papi.components.someThing.SomeThingFilter</filter-class>
    </filter>
```

## Then go make the filter

Be careful with refactoring functions if you copied the files directly.