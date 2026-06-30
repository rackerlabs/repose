# Docker Image: Build & History

## How It Originally Worked

The original Docker pipeline was fully integrated into Gradle via `com.bmuschko:gradle-docker-plugin`, defined in `repose-aggregator/artifacts/docker/build.gradle`. It supported two modes:

### From local packages ("file" mode)

The default during development. Gradle would:
1. Build DEB/RPM packages from the valve, filter-bundle, extensions-filter-bundle, and experimental-filter-bundle subprojects
2. Copy them into a staging directory alongside a Dockerfile
3. The Dockerfile installed them with `dpkg -R --install`

### From the package repository ("repo" mode)

Triggered by passing `-Prepose-version=X.X.X.X`. Used a different Dockerfile that pulled packages directly from the (now defunct) `nexus.openrepose.org` via APT/Yum. No local build needed.

### Publish flow

Wired into the `:release` task. A full release would:
1. Build the image locally (Ubuntu and CentOS variants)
2. Tag as `rackerlabs/repose:<version>` and `rackerlabs/repose:latest`
3. Push to Docker Hub
4. Remove the local image

---

## Current Approach

The old pipeline depended on infrastructure that no longer exists (JCenter, the Nexus package repo, Docker Hub credentials). The new approach uses self-contained multi-stage Dockerfiles that build from source.

### Image variants

| Variant | Location | Base OS | Java |
|---------|----------|---------|------|
| Ubuntu (primary) | `Dockerfile` (root) | Ubuntu 22.04 (Jammy) | Eclipse Temurin 11 JRE |
| Ubuntu | `repose-aggregator/artifacts/docker/src/docker/resources/file/ubuntu/Dockerfile` | Ubuntu 22.04 (Jammy) | Eclipse Temurin 11 JRE |
| Rocky Linux | `repose-aggregator/artifacts/docker/src/docker/resources/file/rocky/Dockerfile` | Rocky Linux 9 | OpenJDK 11 |

The `repo/` variants (for installing from a package repository) are templated but commented out, since the original package repository (`nexus.openrepose.org`) is defunct. Uncomment and point to a live repo if package hosting is restored.

> **Note:** The legacy `centos/` directories have been renamed to `rocky/` to reflect the migration from CentOS 7 (EOL June 2024) to Rocky Linux 9.

### Artifacts included in images

- `/usr/share/repose/repose.jar` — valve fat JAR (shadowJar)
- `/usr/share/repose/filters/*.ear` — filter-bundle, extensions-filter-bundle, experimental-filter-bundle
- `/etc/repose/*.xml`, `/etc/repose/*.cfg.xml` — default configs

---

## Building

### Quick build (root Dockerfile)

```bash
docker build -t repose:9.1.0.5-java11 .
```

Or with compose:

```bash
docker-compose up -d
```

### Rocky Linux variant

```bash
docker build -t repose:9.1.0.5-java11-rocky \
  -f repose-aggregator/artifacts/docker/src/docker/resources/file/rocky/Dockerfile .
```

The build takes a while — it compiles the full project (Scala + Java) inside the builder stage. Tests are skipped (`-x test -x integrationTest`).

## Running

```bash
docker run -d \
  --name repose \
  -p 8080:8080 \
  -v /path/to/config:/etc/repose \
  -e JAVA_OPTS="-Xmx1024m" \
  repose:9.1.0.5-java11
```

### Environment variables

- `JAVA_OPTS` — JVM flags (memory, GC tuning, etc.)

### Volumes

- `/etc/repose` — mount your own system-model, configs, etc.

### Ports

- `8080` — default Repose service port (configurable in `system-model.cfg.xml`)

## Verification

```bash
# Check Java version
docker run --rm repose:9.1.0.5-java11 java -version

# Check logs
docker logs repose
```

## CentOS → Rocky Linux Migration

CentOS 7 reached End of Life in June 2024. The RHEL-compatible variant now uses **Rocky Linux 9**, which is a 1:1 binary-compatible rebuild of RHEL 9 and is maintained by the Rocky Enterprise Software Foundation.

Key differences from the old CentOS 7 image:
- Uses `rockylinux:9-minimal` base (smaller footprint)
- Package manager: `microdnf` instead of `yum`
- Java: `java-11-openjdk-headless` (from Rocky's AppStream)
- User management: `shadow-utils` package provides `useradd`/`groupadd`

## Reference Files

- `Dockerfile` — the primary production Dockerfile (Ubuntu 22.04)
- `docker-compose.yaml` — convenience compose file
- `repose-aggregator/artifacts/docker/` — Docker build infrastructure
  - `src/docker/resources/file/ubuntu/Dockerfile` — Ubuntu multi-stage (from source)
  - `src/docker/resources/file/rocky/Dockerfile` — Rocky Linux multi-stage (from source)
  - `src/docker/resources/repo/ubuntu/Dockerfile` — Ubuntu from package repo (template)
  - `src/docker/resources/repo/rocky/Dockerfile` — Rocky Linux from package repo (template)
  - `build.gradle` — legacy Gradle-driven Docker pipeline (retained for reference)
