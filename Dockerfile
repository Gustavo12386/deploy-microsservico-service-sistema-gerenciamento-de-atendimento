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

# Compila e empacota o JAR Spring Boot (com repackage)
RUN ./mvnw clean package spring-boot:repackage -DskipTests

# Etapa 2 — Imagem final (AWS Lambda Java)
FROM public.ecr.aws/lambda/java:21

# Copia o JAR para o diretório padrão da Lambda
COPY --from=build /app/target/service-0.0.1-SNAPSHOT.jar ${LAMBDA_TASK_ROOT}/app.jar

# Define o handler da Lambda (via Spring Boot Loader)
CMD ["org.springframework.boot.loader.launch.JarLauncher"]








