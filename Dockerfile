FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

COPY src ./src
RUN ./mvnw package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache unzip curl \
    && curl -sSLo /tmp/newrelic-java.zip https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip \
    && unzip -q /tmp/newrelic-java.zip -d /app \
    && rm /tmp/newrelic-java.zip \
    && apk del unzip curl
RUN addgroup -S spring && adduser -S spring -G spring \
    && chown -R spring:spring /app/newrelic
USER spring:spring
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# Configuração do agente (licença, nome da app) vem de variáveis de ambiente
# (NEW_RELIC_LICENSE_KEY, NEW_RELIC_APP_NAME) definidas no Deployment — o
# agente funciona só com env vars, sem precisar editar newrelic.yml.
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-javaagent:/app/newrelic/newrelic.jar", \
  "-jar", "app.jar"]
