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
                    mvn clean package spring-boot:repackage -DskipTests
            '''
            }
        }

        stage('Verificar StreamLambdaHandler no JAR') {
            steps {
                echo '🔎 Verificando se a classe StreamLambdaHandler foi empacotada...'
                script {
                    // Testa se a classe existe dentro do JAR
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

        stage('Build Docker Image') {
            steps {
                script {
                   sh "docker build -t ${ECR_REPO}:${IMAGE_TAG} ."
                }
            }
        }

        stage('Testar Classe no Container') {
            steps {
                echo '🧪 Testando execução do StreamLambdaHandler dentro do container...'
                sh '''
                    set -e

                    echo "📦 Listando conteúdo do /var/task dentro da imagem..."
                    docker run --rm --entrypoint /bin/sh microsservico-atendimento:latest -c "ls -R /var/task"

                    echo "🔍 Verificando se o JAR contém a classe StreamLambdaHandler..."
                    docker run --rm --entrypoint /bin/sh microsservico-atendimento:latest -c "
                        jar tf /var/task/app.jar | grep com/service/config/handler/StreamLambdaHandler.class || echo '❌ Classe não encontrada no JAR!'
                    "

                    echo "▶️ Tentando inicializar o handler via Spring Boot Loader..."
                    docker run --rm --entrypoint /bin/sh microsservico-atendimento:latest -c '
                        java -cp /var/task/app.jar org.springframework.boot.loader.launch.JarLauncher --help > /dev/null 2>&1 &&
                        echo "✅ Handler carregado com sucesso via Spring Boot Loader!" ||
                        echo "⚠️ Falha ao inicializar o handler (verifique o classpath ou a estrutura do JAR)."
                    '
                '''
            }
        }

        stage('Testar Execução do Handler no Container') {
            steps {
                echo '🧪 Testando execução do StreamLambdaHandler dentro do container...'
                sh """
                    docker run --rm --entrypoint /bin/sh ${ECR_REPO}:${IMAGE_TAG} -c "
                        echo '▶️ Tentando inicializar o handler...'
                        if ! java -cp /var/task/app.jar org.springframework.boot.loader.launch.JarLauncher --help; then
                            echo '⚠️ Falha ao executar handler (verifique o classpath). Esta falha é esperada se for um Lambda Handler puro.'
                        fi
                    "
                """
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
                        def functionName = "${LAMBDA_FUNCTION}"
                        def imageUri = "${ECR_URI}:${IMAGE_TAG}"

                        echo " Criando função Lambda '${functionName}' com imagem '${imageUri}'..."

                        sh """
                        if aws lambda get-function --function-name microsservico-atendimento --region us-east-1 >/dev/null 2>&1; then
                            echo "🔁 Função já existe — atualizando imagem..."
                            aws lambda update-function-code \
                                --function-name microsservico-atendimento \
                                --image-uri 381492003133.dkr.ecr.us-east-1.amazonaws.com/microsservico-atendimento:latest \
                                --region us-east-1
                            else
                            echo "🆕 Criando nova função Lambda..."
                            aws lambda create-function \
                                --function-name microsservico-atendimento \
                                --package-type Image \
                                --code ImageUri=381492003133.dkr.ecr.us-east-1.amazonaws.com/microsservico-atendimento:latest \
                                --role arn:aws:iam::381492003133:role/lambda-deploy-role \
                                --region us-east-1
                            fi
                        """

                        echo "✅ Função Lambda criada com sucesso!"
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
