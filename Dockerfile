# Etapa 1 - Build do JAR com Maven
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia o Maven wrapper ou pom.xml e dependências primeiro (cache)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Dá permissão de execução ao wrapper do Maven
RUN chmod +x mvnw

# Baixa dependências para cache
RUN ./mvnw dependency:go-offline

# Copia o restante do código
COPY src ./src

# Compila e empacota o jar (Spring Boot + repackage)
RUN ./mvnw clean package spring-boot:repackage -DskipTests

# Etapa 2 — Imagem final (AWS Lambda Java)
FROM public.ecr.aws/lambda/java:21

# Copia o jar gerado para o local esperado pelo runtime da Lambda
COPY --from=build /app/target/service-0.0.1-SNAPSHOT.jar ${LAMBDA_TASK_ROOT}/app.jar

# Define a classe handler principal da Lambda
CMD ["com.service.config.handler.StreamLambdaHandler"]






