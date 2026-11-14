FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY infra/maven/settings.xml /workspace/infra/maven/settings.xml
RUN chmod +x mvnw \
    && ./mvnw -s /workspace/infra/maven/settings.xml -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -s /workspace/infra/maven/settings.xml -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN useradd --system --uid 10001 --create-home appuser
COPY --from=build /workspace/target/classroom-booking-0.0.1-SNAPSHOT.jar app.jar

ENV TZ=Asia/Shanghai
EXPOSE 8080
USER appuser

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-Duser.timezone=Asia/Shanghai", "-jar", "/app/app.jar"]
