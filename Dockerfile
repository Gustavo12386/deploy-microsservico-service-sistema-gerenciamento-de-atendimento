FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia o Maven Wrapper e o pom.xml
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml .

# Dá permissão de execução ao Maven Wrapper
RUN chmod +x mvnw

# Baixa dependências (cache)
RUN ./mvnw dependency:go-offline

# Copia o restante do código-fonte
COPY src ./src

# Compila e empacota o JAR (gera o app.jar Spring Boot reempacotado)
RUN ./mvnw clean package spring-boot:repackage -DskipTests

FROM public.ecr.aws/lambda/java:21

# Copia o JAR gerado para o diretório da Lambda
COPY --from=build /app/target/*.jar ${LAMBDA_TASK_ROOT}/app.jar

# Define o handler principal da Lambda
# (não coloque ::handleRequest — o runtime já chama automaticamente)
CMD ["com.service.config.handler.StreamLambdaHandler"]







