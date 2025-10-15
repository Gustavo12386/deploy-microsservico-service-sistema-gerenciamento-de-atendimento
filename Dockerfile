# Etapa 1 — Build do JAR com Maven
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia os arquivos do projeto
COPY pom.xml .
COPY src ./src

# Gera o JAR executável (reempacotado com o Spring Boot plugin)
RUN mvn clean package -DskipTests

# Verifica o conteúdo do target para debug
RUN ls -lh target/

# Etapa 2 — Imagem final para AWS Lambda
FROM public.ecr.aws/lambda/java:21

# Copia o JAR gerado para o diretório padrão da Lambda
# ⚠️ Aqui usamos o nome EXATO do JAR gerado no target
COPY --from=build /app/target/*SNAPSHOT.jar ${LAMBDA_TASK_ROOT}/application.jar

# Define o handler padrão do Spring Boot
CMD ["org.springframework.boot.loader.launch.JarLauncher"]
