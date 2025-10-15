# Etapa 1 — Build do JAR com Maven
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia os arquivos do projeto
COPY pom.xml .
COPY src ./src

# Gera o jar (sem testar)
RUN mvn clean package -DskipTests

# Etapa 2 — Imagem final da Lambda
FROM public.ecr.aws/lambda/java:21

# Copia o JAR gerado para o diretório da Lambda
COPY --from=build /app/target/*.jar ${LAMBDA_TASK_ROOT}/application.jar

# Define o handler padrão (para Spring Boot REST, é org.springframework.boot.loader.launch.JarLauncher)
CMD ["org.springframework.boot.loader.launch.JarLauncher"]
