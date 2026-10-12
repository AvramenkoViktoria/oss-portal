# Build stage: compiles the application; tests are run by the CI pipeline before the image is built.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package \
    && java -Djarmode=tools -jar target/oss-portal-*.jar extract --layers --destination target/layers

# Runtime stage
FROM eclipse-temurin:21-jre
RUN useradd --system --create-home --uid 10001 oss
WORKDIR /app
COPY --from=build /workspace/target/layers/dependencies/ ./
COPY --from=build /workspace/target/layers/spring-boot-loader/ ./
COPY --from=build /workspace/target/layers/snapshot-dependencies/ ./
COPY --from=build /workspace/target/layers/application/ ./
RUN mkdir -p /data/storage && chown -R oss:oss /data
USER oss
ENV STORAGE_PATH=/data/storage
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "oss-portal-0.1.0-SNAPSHOT.jar"]
