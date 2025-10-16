# Etapa 1 - Build do JAR com Maven
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package spring-boot:repackage -DskipTests

# Etapa 2 - Imagem final para AWS Lambda (Java 21)
FROM public.ecr.aws/lambda/java:21

# Copia o JAR reempacotado pelo Spring Boot
COPY --from=build /app/target/service-0.0.1-SNAPSHOT.jar ${LAMBDA_TASK_ROOT}/application.jar

# Define o handler (classe Java principal)
CMD ["com.service.config.handler.StreamLambdaHandler"]





