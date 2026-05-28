FROM rust:1.92-bookworm AS native-builder
RUN apt-get update \
    && apt-get install -y --no-install-recommends clang libclang-dev cmake \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /src/llamalib
COPY llamalib/Cargo.toml llamalib/Cargo.lock ./
COPY llamalib/src ./src
RUN cargo build --release

FROM gradle:9.5.1-jdk22 AS java-builder
WORKDIR /src
COPY settings.gradle.kts build.gradle.kts ./
COPY backend/build.gradle.kts backend/build.gradle.kts
RUN gradle --no-daemon :backend:dependencies
COPY backend/src backend/src
RUN gradle --no-daemon :backend:bootJar

FROM eclipse-temurin:22-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-rus tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
ENV CHATLY_NATIVE_LIBRARY=/app/lib/libchatly_llamalib.so
ENV JAVA_TOOL_OPTIONS="--enable-native-access=ALL-UNNAMED"
COPY --from=native-builder /src/llamalib/target/release/libchatly_llamalib.so /app/lib/libchatly_llamalib.so
COPY --from=java-builder /src/backend/build/libs/backend-0.1.0.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
