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

# Copia o jar gerado
COPY --from=build /app/target/service-0.0.1-SNAPSHOT.jar /tmp/app.jar

# Extrai o conteúdo do JAR para o diretório da Lambda
RUN cd ${LAMBDA_TASK_ROOT} && \
    jar -xf /tmp/app.jar && \
    rm /tmp/app.jar

# Define o handler correto
ENV _HANDLER=com.service.config.handler.StreamLambdaHandler

# Comando padrão do Lambda
CMD ["com.service.config.handler.StreamLambdaHandler"]










