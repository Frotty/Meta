FROM eclipse-temurin:25-jdk
USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends git gh curl \
    && rm -rf /var/lib/apt/lists/*

# Use the wrapper's Gradle version without fetching its distribution during a CI job.
COPY --from=gradle:9.1.0-jdk25@sha256:d2f954187670397de6dd42c5c3a9d4535409b590059c6d248ff2a59ba67cecc3 /opt/gradle /opt/gradle
ENV PATH="/opt/gradle/bin:${PATH}"
