# Creating a new filter

* Add a directory in `<REPOSE_DIR>/repose-aggregator/components/filters/` with the name of the filter.
  * By convention, all directories in there should end with `-filter`.
* Create a `build.gradle` in the new directory or copy one from another similar filter.
* Create the standard directory structure:
  * `src/main/resources/META-INF/schema/config`
  * `src/main/resources/META-INF/schema/examples`
  * `src/main/<LANG>/org/openrepose/filters/<FILTER_NAME>`
  * `src/test/<LANG>/org/openrepose/filters/<FILTER_NAME>`
* Add the new filter to the `<REPOSE_DIR>/settings.gradle` so it can be referenced by the bundle project.
* Add the new filter to the `<REPOSE_DIR>/repose-aggregator/artifacts/<BUNDLE_NAME>/build.gradle`'s list of dependencies.
* Add the new filter to the `<REPOSE_DIR>/repose-aggregator/artifacts/<BUNDLE_NAME>/src/main/application/WEB-INF/web-fragment.xml`:
```xml
    <filter>
        <filter-name>Example name</filter-name>
        <filter-class>org.openrepose.filters.someThing.SomeThingFilter</filter-class>
    </filter>
```
* Then go make the filter

Be careful with refactoring functions if you copied the files directly.
Add any new dependencies to the `<REPOSE_DIR>/versions.properties` so anything else using it in the future will bring in the same one.
