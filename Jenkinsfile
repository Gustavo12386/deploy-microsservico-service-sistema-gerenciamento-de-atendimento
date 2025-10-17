pipeline {
    agent any

    environment {
        AWS_REGION = 'us-east-1'
        AWS_ACCOUNT_ID = '381492003133'
        ECR_REPO = 'microsservico-atendimento'
        ECR_URI = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}"
        IMAGE_TAG = 'latest'
        ROLE_ARN = 'arn:aws:iam::381492003133:role/lambda-deploy-role'
        LAMBDA_FUNCTION = 'microsservico-atendimento'
        PATH = "/var/lib/jenkins/.local/bin:${env.PATH}"        
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'master', url: 'https://github.com/Gustavo12386/deploy-microsservico-service-sistema-gerenciamento-de-atendimento'
            }
        }

         stage('Build JAR com Maven') {
            steps {
                echo '🔨 Compilando projeto e empacotando JAR...'
                sh '''
                docker run --rm \
                    -v $(pwd):/app \
                    -w /app \
                    maven:3.9.9-eclipse-temurin-21 \
                    mvn clean package -DskipTests
            '''
            }
        }

        stage('Verificar StreamLambdaHandler no JAR') {
            steps {
                echo '🔎 Verificando se a classe StreamLambdaHandler foi empacotada...'
                script {                    
                    def result = sh(
                        script: '''
                            docker run --rm \
                            -v $(pwd)/target:/target \
                            eclipse-temurin:21 \
                            bash -c "jar tf /target/service-0.0.1-SNAPSHOT.jar | grep StreamLambdaHandler || echo '❌ Classe não encontrada'"
                        ''',
                        returnStdout: true
                    ).trim()

                    if (!result.contains("StreamLambdaHandler")) {
                        error("❌ A classe StreamLambdaHandler não foi encontrada dentro do JAR! Abortando pipeline.")
                    } else {
                        echo "✅ Classe StreamLambdaHandler encontrada com sucesso dentro do JAR!"
                    }
                }
            }
        }

        stage('Prepare Lambda image (explode jar in pipeline)') {
            steps {
                echo '⚙️ Preparando contexto da imagem Lambda (explode JAR e copia classes/libs para lambda-image/)'
                sh '''
                set -e

                # paths and names
                JAR=target/service-0.0.1-SNAPSHOT.jar
                CONTEXT=lambda-image

                rm -rf ${CONTEXT}
                mkdir -p ${CONTEXT}

                if [ ! -f "${JAR}" ]; then
                    echo "JAR não encontrado: ${JAR}"
                    exit 1
                fi

                # copy the fat/executable jar to the lambda image context
                cp "${JAR}" ${CONTEXT}/app.jar

                # create Dockerfile that uses the Spring Boot JarLauncher so internal BOOT-INF is respected
                cat > ${CONTEXT}/Dockerfile <<'EOF'
FROM public.ecr.aws/lambda/java:21

# Copy the fat jar (Spring Boot executable jar)
COPY app.jar /var/task/app.jar

# Tell the Lambda runtime which handler to use
ENV _HANDLER=com.service.config.handler.StreamLambdaHandler::handleRequest

# Use the Spring Boot JarLauncher to start the app from the fat jar
CMD ["org.springframework.boot.loader.JarLauncher","com.service.config.handler.StreamLambdaHandler::handleRequest"]
EOF
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                   sh "docker build -t ${ECR_REPO}:${IMAGE_TAG} lambda-image"
                }
            }
        }

        stage('Testar Classe no Container') {
            steps {
                echo '🧪 Testando execução do StreamLambdaHandler dentro do container...'
                sh '''
                    set -e

                    echo "📦 Listando conteúdo do /var/task dentro da imagem..."
                    docker run --rm --entrypoint /bin/sh microsservico-atendimento:latest -c "ls -R /var/task || true"
                    echo "🔍 Verificando se a classe StreamLambdaHandler foi copiada para /var/task..."
                    docker run --rm --entrypoint /bin/sh microsservico-atendimento:latest -c "ls /var/task/com/service/config/handler/StreamLambdaHandler.class && echo '✅ Classe encontrada' || echo '❌ Classe não encontrada'"
                    echo "🔍 Verificando se há jars de dependência em /var/task (BOOT-INF/lib)..."
                    docker run --rm --entrypoint /bin/sh microsservico-atendimento:latest -c "ls /var/task/*.jar 2>/dev/null || echo 'Nenhum jar no /var/task'; ls /var/task/lib/*.jar 2>/dev/null || echo 'Nenhum jar em /var/task/lib'"
                    echo "📄 Dockerfile gerado (para debug):"
                    docker run --rm --entrypoint /bin/sh microsservico-atendimento:latest -c "cat /var/task/Dockerfile || echo 'Dockerfile não encontrado em /var/task'"
                '''
            }
        }

        stage('Inspect built image') {
            steps {
                echo '🔎 Inspecting built Docker image (Cmd/Entrypoint/Env) and generated Dockerfile before push'
                sh '''
                set -e
                IMAGE=${ECR_REPO}:${IMAGE_TAG}

                echo "Local images (filter):"
                docker images --format "{{.Repository}}:{{.Tag}} {{.ID}}" | grep -E "^${ECR_REPO}:${IMAGE_TAG}" || echo "Image ${IMAGE} not found locally"

                echo "---- docker inspect summary ----"
                docker inspect --format='Entrypoint: {{json .Config.Entrypoint}}' ${IMAGE} || true
                docker inspect --format='Cmd: {{json .Config.Cmd}}' ${IMAGE} || true
                docker inspect --format='Env: {{json .Config.Env}}' ${IMAGE} || true

                echo "---- lambda-image/Dockerfile content ----"
                if [ -f lambda-image/Dockerfile ]; then
                    cat lambda-image/Dockerfile
                else
                    echo 'lambda-image/Dockerfile not found'
                fi
                '''
            }
        }

        stage('Runtime classloader test (compile+run)') {
            steps {
               echo '🧪 Verificando se a classe ContainerInitializationException pode ser carregada a partir de /var/task jars (usando JDK container)'
                                        sh '''
                                            set -e
                                            # Usa um JDK container para compilar e executar um pequeno TestLoader
                                            # Vamos criar o arquivo TestLoader.java localmente e montá-lo no container
                                            mkdir -p tmp_testloader
                                            cat > tmp_testloader/TestLoader.java <<'JAVA'
public class TestLoader {
    public static void main(String[] args) {
        try {
            Class.forName("com.amazonaws.serverless.exceptions.ContainerInitializationException");
            System.out.println("FOUND: ContainerInitializationException");
        } catch (Throwable t) {
            System.out.println("NOT FOUND or error:");
            t.printStackTrace();
            System.exit(2);
        }
    }
}
JAVA

                                            docker run --rm -v $(pwd)/lambda-image:/var/task -v $(pwd)/tmp_testloader:/tmp -w /tmp maven:3.9.9-eclipse-temurin-21 bash -c "javac -cp '/var/task/*' TestLoader.java && java -cp '/var/task/*:.' TestLoader" || true
                                        '''
            }
        }

        stage('Testar Execução do Handler no Container') {
            steps {
                echo '🧪 Testando execução do StreamLambdaHandler dentro do container...'
                sh """
                    docker run --rm --entrypoint /bin/sh ${ECR_REPO}:${IMAGE_TAG} -c "
                        echo '▶️ Verificando se o container exige o handler como argumento (entrypoint) e exibe a eficiência do CMD...' &&
                        echo 'Entrypoint and CMD:' && ps aux || true
                    "
                """
            }
        }

        stage('Debug: show Lambda entrypoint script') {
            steps {
                echo '🐞 Mostrando /lambda-entrypoint.sh do base image para inspecionar classpath/entrypoint behavior'
                sh '''
                set -e
                docker run --rm --entrypoint /bin/sh ${ECR_REPO}:${IMAGE_TAG} -c "
                    echo '--- /lambda-entrypoint.sh ---' && \
                    if [ -f /lambda-entrypoint.sh ]; then cat /lambda-entrypoint.sh; else echo '/lambda-entrypoint.sh not present'; fi && \
                    echo '\n--- /var/runtime/bootstrap (if present) ---' && \
                    if [ -f /var/runtime/bootstrap ]; then cat /var/runtime/bootstrap; else echo 'bootstrap not present'; fi
                "
                '''
            }
        }

        stage('Install AWS CLI') {
            steps {
                echo ' Verificando se o AWS CLI está instalado...'
                sh '''
                if ! command -v aws &> /dev/null
                then
                    echo " Instalando AWS CLI localmente ..."
                    curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
                    unzip -o -q awscliv2.zip
                    ./aws/install --update -i /var/lib/jenkins/aws-cli -b /var/lib/jenkins/.local/bin
                else
                    echo " AWS CLI já está instalado."
                fi

                echo " Versão atual do AWS CLI:"
                export PATH=/var/lib/jenkins/.local/bin:$PATH
                aws --version
                '''
            }
        }

        stage('Login to ECR') {
            steps {
                withAWS(region: "${AWS_REGION}", credentials: 'aws-credentials') {
                    script {
                        sh '''
                        aws ecr describe-repositories --repository-names ${ECR_REPO} || \
                        aws ecr create-repository --repository-name ${ECR_REPO}

                        aws ecr get-login-password --region ${AWS_REGION} | \
                        docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
                        '''
                    }
                }
            }
        }

        stage('Push Image to ECR') {
            steps {
                script {
                    sh '''
                    docker tag ${ECR_REPO}:latest ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}:latest
                    docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}:latest
                    echo "⏳ Aguardando propagação da imagem no ECR..."
                    sleep 15
                    '''
                }
            }
        }


       stage('Create Lambda Function') {
            steps {
                withAWS(region: "${AWS_REGION}", credentials: 'aws-credentials') {
                    script {
                       
                        echo "🚀 Criando ou atualizando função Lambda '${LAMBDA_FUNCTION}' com imagem '${ECR_URI}:${IMAGE_TAG}'..."

                        sh """
                        # Usando as variáveis de ambiente no sh
                        if aws lambda get-function --function-name ${LAMBDA_FUNCTION} --region ${AWS_REGION} >/dev/null 2>&1; then
                            echo "🔁 Função já existe — atualizando imagem..."
                            aws lambda update-function-code \
                                --function-name ${LAMBDA_FUNCTION} \
                                --image-uri ${ECR_URI}:${IMAGE_TAG} \
                                --region ${AWS_REGION}
                        else
                            echo "🆕 Criando nova função Lambda..."
                            aws lambda create-function \
                                --function-name ${LAMBDA_FUNCTION} \
                                --package-type Image \
                                --code ImageUri=${ECR_URI}:${IMAGE_TAG} \
                                --role ${ROLE_ARN} \
                                --region ${AWS_REGION}
                        fi
                        """

                        echo "✅ Função Lambda criada ou atualizada com sucesso!"
                    }
                }
            }
        }

        
    }

    post {
        success {
            echo '✅ Deploy via container concluído com sucesso!'
        }
        failure {
            echo '❌ Falha no deploy!'
        }
    }
}
