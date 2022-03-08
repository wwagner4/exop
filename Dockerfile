FROM gradle:jdk17

RUN mkdir /app

WORKDIR /app

COPY build.gradle.kts .
COPY src .
COPY open_exoplanet_catalogue .

RUN gradle test --no-daemon
RUN gradle assemble --no-daemon

