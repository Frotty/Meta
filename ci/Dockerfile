FROM eclipse-temurin:25-jdk
USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends git gh curl \
    && rm -rf /var/lib/apt/lists/*

# Use the wrapper's Gradle version without fetching its distribution during a CI job.
COPY --from=gradle:9.7.0-jdk25@sha256:35b2b4054977b95348bdd126e808e245fc1e28e56537b51cfd90c7f355b5a105 /opt/gradle /opt/gradle
ENV PATH="/opt/gradle/bin:${PATH}"
