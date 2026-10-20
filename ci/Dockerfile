FROM eclipse-temurin:25-jdk
USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends git gh curl \
    && rm -rf /var/lib/apt/lists/*
