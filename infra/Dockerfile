FROM jenkins/jenkins:lts

USER root

# Instala Docker CLI (se ainda não estiver)
RUN apt-get update && apt-get install -y docker.io curl

# Instala Docker Compose V2
RUN DOCKER_CONFIG=/var/jenkins_home/.docker \
    && mkdir -p $DOCKER_CONFIG/cli-plugins \
    && curl -SL https://github.com/docker/compose/releases/download/v2.24.1/docker-compose-linux-x86_64 -o $DOCKER_CONFIG/cli-plugins/docker-compose \
    && chmod +x $DOCKER_CONFIG/cli-plugins/docker-compose

USER jenkins


# Expor porta padrão do Jenkins
EXPOSE 8080 50000
