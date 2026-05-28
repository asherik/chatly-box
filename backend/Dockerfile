FROM rust:1.92-bookworm AS native-builder
RUN apt-get update \
    && apt-get install -y --no-install-recommends clang libclang-dev cmake \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /src/llamalib
COPY llamalib/Cargo.toml llamalib/Cargo.lock ./
COPY llamalib/src ./src
RUN cargo build --release

FROM maven:3.9.9-eclipse-temurin-22 AS java-builder
WORKDIR /src/backend
COPY backend/pom.xml ./
RUN mvn -q dependency:go-offline
COPY backend/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:22-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-rus tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
ENV CHATLY_NATIVE_LIBRARY=/app/lib/libchatly_llamalib.so
ENV JAVA_TOOL_OPTIONS="--enable-native-access=ALL-UNNAMED"
COPY --from=native-builder /src/llamalib/target/release/libchatly_llamalib.so /app/lib/libchatly_llamalib.so
COPY --from=java-builder /src/backend/target/backend-0.1.0.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
