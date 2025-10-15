# Etapa 1 — Build com Maven
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia pom e baixa dependências
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia o código-fonte
COPY src ./src

# Compila e empacota o JAR Spring Boot (skip testes)
RUN mvn clean package spring-boot:repackage -DskipTests


# Etapa 2 — Imagem final Lambda
FROM public.ecr.aws/lambda/java:21

# Copia o JAR final do build
COPY --from=build /app/target/service-0.0.1-SNAPSHOT.jar ${LAMBDA_TASK_ROOT}/app.jar

# Define o handler correto para Spring Boot
CMD ["com.service.config.handler.StreamLambdaHandler"]




