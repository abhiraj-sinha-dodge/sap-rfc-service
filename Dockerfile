# ─── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Install JCo jar into local Maven repo so dependency:resolve doesn't fail
# (system-scoped deps don't resolve from Central)
COPY lib/sapjco3.jar lib/sapjco3.jar
RUN mvn install:install-file \
      -Dfile=lib/sapjco3.jar \
      -DgroupId=com.sap.conn.jco \
      -DartifactId=sapjco3 \
      -Dversion=3.1.13 \
      -Dpackaging=jar \
      -q

# Resolve all other dependencies (cached layer — only re-runs if pom.xml changes)
COPY pom.xml .
COPY .mvn/ .mvn/
RUN mvn dependency:resolve -q

# Build fat jar (sapjco3.jar excluded by pom.xml plugin config)
COPY src/ src/
RUN mvn package -DskipTests -q

# ─── Stage 2: Runtime ────────────────────────────────────────────────────────
# Jammy (Ubuntu 22.04) — JCo requires glibc, NOT musl/Alpine
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# JCo native library (Linux x86_64) and JAR — kept outside the fat jar
# so JCo can verify its own filename ("sapjco3.jar") at startup
COPY lib/sapjco3.jar   lib/sapjco3.jar
COPY lib/libsapjco3.so lib/libsapjco3.so

COPY --from=builder /build/target/rfc-service.jar app.jar

ENV LD_LIBRARY_PATH=/app/lib

EXPOSE 8090

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -sf http://localhost:8090/rfc/health || exit 1

# PropertiesLauncher (ZIP layout) + loader.path loads sapjco3.jar by its real name
ENTRYPOINT ["java", \
  "-Djava.library.path=/app/lib", \
  "-Dloader.path=/app/lib", \
  "-jar", "app.jar"]
