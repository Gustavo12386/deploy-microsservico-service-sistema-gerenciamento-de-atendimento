# Etapa 1 — Build com Maven
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2 — Imagem final da Lambda
FROM public.ecr.aws/lambda/java:21
COPY --from=build /app/target/*SNAPSHOT.jar ${LAMBDA_TASK_ROOT}/application.jar

# Handler da Lambda (classe + método)
CMD ["com.service.config.handler.StreamLambdaHandler::handleRequest"]

