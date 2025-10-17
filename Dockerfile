# Etapa 1 — Build do JAR com Maven
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package -DskipTests

# Etapa 2 — Imagem final (para AWS Lambda)
FROM public.ecr.aws/lambda/java:21

# Copia o JAR para o diretório padrão do Lambda
COPY --from=build /app/target/service-0.0.1-SNAPSHOT.jar ${LAMBDA_TASK_ROOT}/app.jar

# Define o handler
ENV _HANDLER=com.service.config.handler.StreamLambdaHandler

# Executa via Spring Boot Loader
CMD ["com.service.config.handler.StreamLambdaHandler"]










