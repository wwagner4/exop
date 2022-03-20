FROM openjdk:17-bullseye

RUN mkdir /build
RUN mkdir /app
RUN mkdir /out

WORKDIR /exop
COPY build.gradle.kts .
COPY gradle.properties .
COPY settings.gradle.kts .
COPY gradlew .
COPY src src
COPY gradle gradle

RUN ./gradlew assemble --no-daemon
RUN tar -xf build/distributions/exop-1.0.tar

RUN mv exop-1.0 /app/exop/

WORKDIR /app
RUN git clone https://github.com/OpenExoplanetCatalogue/open_exoplanet_catalogue.git
ENV CATALOGUE=open_exoplanet_catalogue

ENTRYPOINT ["exop/bin/exop"]

