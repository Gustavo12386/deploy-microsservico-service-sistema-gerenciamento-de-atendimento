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

# Extrai o conteúdo do JAR e coloca classes/dependências diretamente em /var/task
# O runtime do Lambda adiciona jars em /var/task/* ao classpath, mas não procura em BOOT-INF/*
RUN cd ${LAMBDA_TASK_ROOT} && \
    jar -xf /tmp/app.jar && \
    # move application classes to task root so they are on the classpath
    if [ -d BOOT-INF/classes ]; then cp -r BOOT-INF/classes/* .; fi && \
    # move dependency jars to task root so they match /var/task/* wildcard
    if [ -d BOOT-INF/lib ]; then cp -r BOOT-INF/lib/* .; fi && \
    rm -rf /tmp/app.jar BOOT-INF

# Define o handler no formato esperado (Class::method)
ENV _HANDLER=com.service.config.handler.StreamLambdaHandler::handleRequest

# Não sobrescrever o ENTRYPOINT padrão da imagem base; o runtime usará _HANDLER










