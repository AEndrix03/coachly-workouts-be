FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -q -DskipTests clean package

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN useradd -r -u 10001 -g root spring
COPY --from=build /workspace/target/*.jar /app/app.jar
RUN chown -R spring:root /app
USER spring

EXPOSE 8080

ENV SERVER_PORT=8080
ENV SPRING_THREADS_VIRTUAL_ENABLED=true
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.threads.virtual.enabled=${SPRING_THREADS_VIRTUAL_ENABLED} -jar /app/app.jar"]
