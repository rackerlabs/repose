package framework.spockExtensions

import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.SpecInfo


class GlobalSpecExtension implements IGlobalExtension {
    @Override
    void visitSpec(SpecInfo spec) {
        spec.addListener(new GrabReposeLogsOnFailureListener())
    }
}
