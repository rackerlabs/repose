# Quick Start

## Build from source

Requires Java 11+.

```bash
./gradlew clean buildAll -x test -x integrationTest
```

## Docker image

The Dockerfile does a full source build internally, so you don't need to build locally first:

```bash
docker build -t repose:9.1.0.5-java11 .
```

Or with compose:

```bash
docker-compose up -d
```

## Run with Docker

```bash
docker run -d \
  --name repose \
  -p 8080:8080 \
  -v /path/to/config:/etc/repose \
  -e JAVA_OPTS="-Xmx1024m" \
  repose:9.1.0.5-java11
```

## Verify

```bash
# Java version
docker run --rm repose:9.1.0.5-java11 java -version

# Container logs
docker logs repose
```

## What was upgraded

- Java 8 → Java 11 (Eclipse Temurin) — fixes CVE-2023-41993
- Ubuntu 18.04 → Ubuntu 22.04 LTS
- Gradle 4.x → Gradle 6.9.4
- Scalastyle → Scalafix
- Removed defunct plugins (HTTP Builder NG, org.ajoberstar gradle-git)

## Further reading

- `BUILD-DOCKER.md` — Docker build details and history of the original pipeline
- `PLUGIN-ALTERNATIVES.md` — Plugin migration record
