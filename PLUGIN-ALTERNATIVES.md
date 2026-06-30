# Plugin Migration Record

This document records the Gradle plugin changes made during the Java 11 upgrade.

## Replaced: Scalastyle → Scalafix

**Old plugin**: `org.github.ngbinh.scalastyle:gradle-scalastyle-plugin_2.11:1.0.1`  
**New plugin**: `io.github.cosmicsilence:gradle-scalafix:0.2.6`

Scalastyle was deprecated and incompatible with Scala 2.12+. It was replaced by Scalafix, which provides both linting (`checkScalafix`) and auto-fix (`scalafix`) capabilities.

Configuration lives at `repose-aggregator/src/config/styles/.scalafix.conf`. The plugin is applied to leaf subprojects only, with generated, scoverage, test, and integrationTest source sets excluded.

## Removed: HTTP Builder NG

**Old plugin**: `gradle.plugin.io.github.http-builder-ng:http-plugin:0.1.1`

This plugin provided an `HttpTask` type used exclusively by the `publishToPackageRepo` task, which uploaded built DEB/RPM packages to an internal package repository via HTTP. It was only invoked during the `release` task chain and had no role in normal builds or tests.

The plugin is dormant (maintainers stepped away) and the task has been removed. If OS package publishing is needed in the future, a CI pipeline step or shell script would be a better fit than a Gradle plugin.

## Removed: Gradle Git (org.ajoberstar)

**Old plugin**: `org.ajoberstar:gradle-git:1.7.2`

This plugin is archived and was redundant — the build already uses `com.netflix.nebula:gradle-git-scm-plugin:3.0.1` for Git operations (tagging releases via `scmFactory.create().tag(version)`). No replacement was needed.
