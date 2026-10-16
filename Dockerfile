FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S groundsegment && adduser -S groundsegment -G groundsegment
WORKDIR /app
COPY --from=build /build/target/satellite-ground-segment-1.0.0.jar app.jar
USER groundsegment
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s \
    CMD wget -q -O - http://localhost:8080/actuator/health | grep -q UP
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
