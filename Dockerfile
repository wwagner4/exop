FROM docker.io/library/openjdk:17-bullseye

RUN apt-get update
RUN apt-get install -y npm

RUN mkdir /app

RUN apt update
RUN apt install -y npm

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

WORKDIR /exop/src/exop-react

RUN npm install
RUN bash run_build

WORKDIR /react
RUN cp -r /exop/src/exop-react/build/* .
ENV EXOP_REACT_DIR=/react
ENV REACT_BUILD_DIR=/react

WORKDIR /app
RUN git clone https://github.com/OpenExoplanetCatalogue/open_exoplanet_catalogue.git
ENV CATALOGUE=open_exoplanet_catalogue

ENTRYPOINT ["exop/bin/exop", "server"]
