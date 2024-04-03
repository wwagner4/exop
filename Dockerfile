FROM docker.io/library/openjdk:17-bullseye

RUN mkdir /app

RUN apt update
RUN apt install -y npm
RUN npm cache clean -f
RUN npm install -g n
RUN n stable
RUN n 18

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
RUN npx update-browserslist-db@latest
RUN bash run_build

WORKDIR /react
RUN cp -r /exop/src/exop-react/build/* .
ENV EXOP_REACT_DIR=/react
ENV REACT_BUILD_DIR=/react

WORKDIR /app
RUN git clone https://github.com/OpenExoplanetCatalogue/open_exoplanet_catalogue.git
ENV CATALOGUE=open_exoplanet_catalogue

ENTRYPOINT ["exop/bin/exop", "server"]
