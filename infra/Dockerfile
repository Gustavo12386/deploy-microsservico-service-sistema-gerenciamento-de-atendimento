# Use a imagem oficial do Jenkins LTS
FROM jenkins/jenkins:lts

# Trocar para root para instalar pacotes
USER root

# Atualizar pacotes e instalar Docker e Docker Compose
RUN apt-get update && \
    apt-get install -y \
        docker.io \
        docker-compose \
        git \
        curl \
        vim \
        unzip \
        sudo \
    && rm -rf /var/lib/apt/lists/*

# Permitir que o Jenkins use Docker sem sudo
RUN usermod -aG docker jenkins

# Voltar para o usuário Jenkins
USER jenkins

# Expor porta padrão do Jenkins
EXPOSE 8080 50000
