# Development

## Requirements
* JDK 16+
* node v16+
* npm  "any version"

## Data requirements

```shell
cd .../exop/..
git clone git clone https://github.com/OpenExoplanetCatalogue/open_exoplanet_catalogue.git
```

## Kotlin development

```shell
./gradlew build
./gradlew test
./gradlew run --arg "follow output or --help"
```

## React development

### start the kotlin server

./gradlew run --arg server

```cd src/exop-react```

### Install npm dependencies

```npm install```

### Start the development server on localhost:8080

./start_development_server

# Run exop using docker (podman)

```shell
podman build -t exop
podman run -v <OUT_DIR>:/out exop --help
podman run -v <OUT_DIR>:/out exop i01
```

Keep in mind: The default output of 'exop' is /out

e.g.

```shell
podman run -v /home/wwagner4/work/exop-out:/out exop i01
podman run -v $(pwd):/out exop i01
podman run -v $(pwd):/out exop --help
```

Start the server

```shell
podman run -p 8000:8080 -t exop exop/bin/exop server
```

# Deployment

Deployment is handled by the ```build-all.sh``` script in