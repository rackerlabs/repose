#!/usr/bin/env groovy

// 1. Copy to one level up from the project you wish to cleanup
// 2. Update the files you want to keep
// 3. Swap commenting on lines 54 & 55
// 4. Run ../cleanup.groovy from the project directory to confirm what files are going to be deleted
// 5. Swap the lines again
// 6. Run this from the project directory
//  git filter-branch --tree-filter '~/dev/projects/cleanup.groovy' --tag-name-filter cat --prune-empty -- --all

import static groovy.io.FileType.FILES

def filesToKeep = [
    './.gitignore',
    './gradlew',
    './gradlew.bat',
    './build.gradle',
    './settings.gradle',
    './versions.properties',
    './repose-aggregator/build.gradle',
    './repose-aggregator/tests/functional-tests/build.gradle',
    './repose-aggregator/src/docs/asciidoc/filters/attribute-mapping-policy-validation.adoc',
    './repose-aggregator/src/docs/asciidoc/filters/rackspace-auth-user.adoc',
    './repose-aggregator/src/docs/asciidoc/filters/saml-policy.adoc',
    './.git',
    './gradle',
    './repose-aggregator/components/filters/attribute-mapping-policy-validation-filter',
    './repose-aggregator/components/filters/rackspace-auth-user-filter',
    './repose-aggregator/components/filters/saml-policy-translation-filter',
    './repose-aggregator/tests/functional-tests/src/integrationTest/resources',
    './repose-aggregator/tests/functional-tests/src/integrationTest/groovy/features/filters/attributemappingvalidation',
    './repose-aggregator/tests/functional-tests/src/integrationTest/groovy/features/filters/rackspaceauthuser',
    './repose-aggregator/tests/functional-tests/src/integrationTest/groovy/features/filters/samlpolicy',
    './repose-aggregator/tests/functional-tests/src/integrationTest/configs/common',
    './repose-aggregator/tests/functional-tests/src/integrationTest/configs/features/filters/attributemappingvalidation',
    './repose-aggregator/tests/functional-tests/src/integrationTest/configs/features/filters/rackspaceauthuser',
    './repose-aggregator/tests/functional-tests/src/integrationTest/configs/features/filters/samlpolicy',
    './repose-aggregator/artifacts/extensions-filter-bundle',
    './repose-aggregator/src/config',
    './repose-aggregator/components/filters/rackspace-auth-user',
    './repose-aggregator/functional-tests/spock-functional-test/src/test/groovy/features/filters/rackspaceauthuser',
    './repose-aggregator/functional-tests/spock-functional-test/src/test/configs/common',
    './repose-aggregator/functional-tests/spock-functional-test/src/test/configs/features/filters/rackspaceauthuser',
    ]

def dir = new File("./")

def files = []

dir.traverse(type: FILES, maxDepth: 100) { files.add(it) }

files.each { fileToCheck ->
    if(!filesToKeep.any({ fileToCheck.toString().contains(it) })) {
         fileToCheck.delete()
//       println fileToCheck.toString()
    }
}
