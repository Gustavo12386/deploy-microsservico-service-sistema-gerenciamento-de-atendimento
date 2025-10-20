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
        LAMBDA_MEMORY = '1024'
        LAMBDA_TIMEOUT = '60'
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
                echo 'Compilando projeto e empacotando JAR...'
                bat """
                    wsl docker run --rm ^
                        -v \$(wslpath -a %CD%):/app ^
                        -w /app ^
                        maven:3.9.9-eclipse-temurin-21 ^
                        mvn clean package -DskipTests
                """
            }
        }

        stage('Verificar StreamLambdaHandler no JAR') {
            steps {
                echo '🔎 Verificando se a classe StreamLambdaHandler foi empacotada...'
                script {
                    def result = bat(
                        returnStdout: true,
                        script: """
                            wsl bash -c "jar tf /mnt/c/${env.WORKSPACE//\\//}/target/service-0.0.1-SNAPSHOT.jar | grep StreamLambdaHandler || echo '❌ Classe não encontrada'"
                        """
                    ).trim()

                    if (!result.contains("StreamLambdaHandler")) {
                        error("❌ A classe StreamLambdaHandler não foi encontrada dentro do JAR! Abortando pipeline.")
                    } else {
                        echo "✅ Classe StreamLambdaHandler encontrada com sucesso dentro do JAR!"
                    }
                }
            }
        }


        stage('Prepare Lambda image') {
            steps {
                echo '⚙️ Preparando contexto da imagem Lambda'
                bat """
                    wsl bash -c '
                    set -e
                    JAR=target/service-0.0.1-SNAPSHOT.jar
                    CONTEXT=lambda-image
                    rm -rf \${CONTEXT} && mkdir -p \${CONTEXT}
                    cp \${JAR} \${CONTEXT}/app.jar
                    mkdir -p explode_tmp
                    (cd explode_tmp && jar xf ../\${JAR})
                    [ -d explode_tmp/BOOT-INF/classes ] && cp -a explode_tmp/BOOT-INF/classes/. \${CONTEXT}/ || true
                    [ -d explode_tmp/BOOT-INF/lib ] && cp -a explode_tmp/BOOT-INF/lib/. \${CONTEXT}/lib/ || true
                    rm -rf explode_tmp
                    cat > \${CONTEXT}/Dockerfile <<'EOF'
                    FROM public.ecr.aws/lambda/java:21
                    COPY app.jar /var/task/app.jar
                    COPY . /var/task/
                    ENV _HANDLER=com.service.config.handler.StreamLambdaHandler::handleRequest
                    CMD ["com.service.config.handler.StreamLambdaHandler::handleRequest"]
                    EOF
                    '
                """
            }
        }


          stage('Build Docker Image') {
            steps {
                bat """
                    wsl docker build -t ${ECR_REPO}:${IMAGE_TAG} lambda-image
                """
            }
        }

         stage('Testar Classe no Container') {
            steps {
                echo '🧪 Testando execução do StreamLambdaHandler dentro do container...'
                bat """
                    wsl bash -c '
                        docker run --rm --entrypoint /bin/sh ${ECR_REPO}:latest -c "ls -R /var/task || true"
                        docker run --rm --entrypoint /bin/sh ${ECR_REPO}:latest -c "ls /var/task/com/service/config/handler/StreamLambdaHandler.class && echo \\"✅ Classe encontrada\\" || echo \\"❌ Classe não encontrada\\""
                        docker run --rm --entrypoint /bin/sh ${ECR_REPO}:latest -c "ls /var/task/*.jar 2>/dev/null || echo \\"Nenhum jar no /var/task\\"; ls /var/task/lib/*.jar 2>/dev/null || echo \\"Nenhum jar em /var/task/lib\\""
                        docker run --rm --entrypoint /bin/sh ${ECR_REPO}:latest -c "cat /var/task/Dockerfile || echo \\"Dockerfile não encontrado\\"" 
                    '
                """
            }
        }

         stage('Inspect built image') {
            steps {
                echo '🔎 Inspecting Docker image'
                bat """
                    wsl bash -c '
                        IMAGE=${ECR_REPO}:${IMAGE_TAG}
                        docker images --format "{{.Repository}}:{{.Tag}} {{.ID}}" | grep -E "^${ECR_REPO}:${IMAGE_TAG}" || echo "Image ${IMAGE} not found"
                        docker inspect --format='Entrypoint: {{json .Config.Entrypoint}}' ${IMAGE} || true
                        docker inspect --format='Cmd: {{json .Config.Cmd}}' ${IMAGE} || true
                        docker inspect --format='Env: {{json .Config.Env}}' ${IMAGE} || true
                        cat lambda-image/Dockerfile || echo "Dockerfile not found"
                    '
                """
            }
        }

         stage('Runtime classloader test') {
            steps {
                echo '🧪 Testando carregamento de classes no container'
                bat """
                    wsl bash -c '
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
                        docker run --rm -v $(pwd)/lambda-image:/var/task -v $(pwd)/tmp_testloader:/tmp -w /tmp maven:3.9.9-eclipse-temurin-21 bash -c "javac -cp '/var/task/*:/var/task/lib/*' TestLoader.java && java -cp '/var/task/*:/var/task/lib/*:.' TestLoader" || true
                    '
                """
            }
        }



        stage('Testar Execução do Handler no Container') {
            steps {
                echo '🧪 Testando execução do StreamLambdaHandler dentro do container...'
                bat """
                    wsl docker run --rm --entrypoint /bin/sh ${ECR_REPO}:${IMAGE_TAG} -c "echo '▶️ Entrypoint e CMD:' && ps aux || true"
                """
            }
        }

         stage('Debug: show Lambda entrypoint script') {
            steps {
                echo '🐞 Debug Lambda entrypoint'
                bat """
                    wsl docker run --rm --entrypoint /bin/sh ${ECR_REPO}:${IMAGE_TAG} -c "
                        if [ -f /lambda-entrypoint.sh ]; then cat /lambda-entrypoint.sh; else echo '/lambda-entrypoint.sh not present'; fi
                        if [ -f /var/runtime/bootstrap ]; then cat /var/runtime/bootstrap; else echo 'bootstrap not present'; fi
                    "
                """
            }
        }

        stage('Install AWS CLI') {
            steps {
                echo 'Verificando AWS CLI'
                bat """
                    if not exist "C:\\Users\\jenkins\\.local\\bin\\aws.exe" (
                        powershell -Command "Invoke-WebRequest https://awscli.amazonaws.com/awscli-exe-windows-x86_64.zip -OutFile awscliv2.zip; Expand-Archive awscliv2.zip -DestinationPath C:\\Users\\jenkins\\aws-cli; C:\\Users\\jenkins\\aws-cli\\AWSCLI\\aws.exe --version"
                    ) else (
                        aws --version
                    )
                """
            }
        }

        stage('Login to ECR') {
            steps {
                withAWS(region: "${AWS_REGION}", credentials: 'aws-credentials') {
                    bat """
                        aws ecr get-login-password --region ${AWS_REGION} | ^
                        wsl docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
                    """
                }
            }
        }

         stage('Push Image to ECR') {
            steps {
                bat """
                    wsl docker tag ${ECR_REPO}:latest ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}:latest
                    wsl docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}:latest
                """
            }
        }

       stage('Update Lambda') {
            steps {
                withAWS(region: "${AWS_REGION}", credentials: 'aws-credentials') {
                    script {
                        def digest = bat(returnStdout: true, script: """
                            aws ecr describe-images --repository-name ${ECR_REPO} --image-ids imageTag=${IMAGE_TAG} --region ${AWS_REGION} --query 'imageDetails[0].imageDigest' --output text
                        """).trim()

                        def imageWithDigest = "${ECR_URI}@${digest}"
                        bat """
                            aws lambda update-function-code --function-name ${LAMBDA_FUNCTION} --image-uri ${imageWithDigest} --region ${AWS_REGION}
                        """
                    }
                }
            }
        }
        
    }

    stage('Create Lambda Function') {
        steps {
            withAWS(region: "${AWS_REGION}", credentials: 'aws-credentials') {
                script {
                    def deployImage = env.IMAGE_WITH_DIGEST ?: "${ECR_URI}:${IMAGE_TAG}"
                    echo "🚀 Criando ou atualizando função Lambda '${LAMBDA_FUNCTION}' com imagem '${deployImage}'..."

           
                    def checkResult = bat(
                        script: """
                            @echo off
                            powershell -Command "
                            try {
                                aws lambda get-function --function-name ${LAMBDA_FUNCTION} --region ${AWS_REGION} > \$null
                                Write-Output 'EXISTS'
                            } catch {
                                Write-Output 'MISSING'
                            }
                            "
                        """,
                        returnStdout: true
                    ).trim()

                    if (checkResult == 'EXISTS') {
                        echo "🔁 Função já existe — atualizando imagem..."

                        
                        bat """
                            @echo off
                            powershell -Command "
                            \$MAX_WAIT=600
                            \$SLEEP=5
                            \$ELAPSED=0
                            Write-Output '⏳ Waiting for any existing Lambda update to finish (max ' + \$MAX_WAIT + 's)...'
                            while (\$ELAPSED -lt \$MAX_WAIT) {
                                \$status = aws lambda get-function-configuration --function-name ${LAMBDA_FUNCTION} --region ${AWS_REGION} --query 'LastUpdateStatus' --output text
                                Write-Output ('Lambda LastUpdateStatus=' + \$status)
                                if (\$status -ne 'InProgress') { break }
                                Start-Sleep -Seconds \$SLEEP
                                \$ELAPSED += \$SLEEP
                            }
                            if (\$ELAPSED -ge \$MAX_WAIT) {
                                Write-Error '❌ Timeout waiting for existing Lambda update to finish'
                                exit 1
                            }
                            "
                        """

                        
                        bat """
                            @echo off
                            aws lambda update-function-code --function-name ${LAMBDA_FUNCTION} --image-uri ${deployImage} --region ${AWS_REGION}
                        """
                    } else {
                        echo "🆕 Criando nova função Lambda..."
                        bat """
                            @echo off
                            aws lambda create-function --function-name ${LAMBDA_FUNCTION} --package-type Image --code ImageUri=${deployImage} --role ${ROLE_ARN} --region ${AWS_REGION}
                        """
                    }

                    echo "✅ Função Lambda criada ou atualizada com sucesso!"
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
