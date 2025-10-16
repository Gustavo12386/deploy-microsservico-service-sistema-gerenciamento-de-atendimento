# Etapa 1 — Build do JAR com Maven
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia o Maven Wrapper e dependências
COPY pom.xml ./
COPY mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# Copia o código-fonte
COPY src ./src

# Compila e empacota o JAR completo (repackage)
RUN ./mvnw clean package -DskipTests

# Etapa 2 — Imagem final baseada no runtime da AWS Lambda
FROM public.ecr.aws/lambda/java:21

# Copia o JAR gerado para o diretório padrão da Lambda
COPY --from=build /app/target/service-0.0.1-SNAPSHOT.jar ${LAMBDA_TASK_ROOT}/lib/app.jar

# Define o handler Java da Lambda (classe que implementa RequestStreamHandler)
ENV _HANDLER=com.service.config.handler.StreamLambdaHandler

# Define o comando padrão (Lambda já reconhece o handler automaticamente)
CMD ["app.jar"]








