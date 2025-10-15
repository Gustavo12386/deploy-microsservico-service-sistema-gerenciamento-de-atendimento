# Etapa 1 — Build com Maven
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src

# Compila e empacota (gera JAR reempacotado do Spring Boot)
RUN mvn clean package spring-boot:repackage -DskipTests


# Etapa 2 — Imagem final Lambda
FROM public.ecr.aws/lambda/java:21

# Copia o JAR final
COPY --from=build /app/target/service-0.0.1-SNAPSHOT.jar ${LAMBDA_TASK_ROOT}/app.jar

# Handler completo do Spring Boot (AWS + Lambda container)
CMD ["com.service.config.handler.StreamLambdaHandler"]



