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

                # copy the fat/executable jar to the lambda image context (kept for reference)
                cp "${JAR}" ${CONTEXT}/app.jar

                # Explode the fat jar and move BOOT-INF/classes -> ${CONTEXT}/
                # and BOOT-INF/lib -> ${CONTEXT}/lib so the Lambda base image
                # runtime will find classes under /var/task and dependency jars under /var/task/lib
                mkdir -p ${CONTEXT}/lib
                mkdir -p explode_tmp
                (cd explode_tmp && jar xf ../${JAR})
                # Move classes (if present)
                if [ -d explode_tmp/BOOT-INF/classes ]; then
                    mkdir -p ${CONTEXT}
                    cp -a explode_tmp/BOOT-INF/classes/. ${CONTEXT}/ || true
                fi
                # Move dependency jars
                if [ -d explode_tmp/BOOT-INF/lib ]; then
                    mkdir -p ${CONTEXT}/lib
                    cp -a explode_tmp/BOOT-INF/lib/. ${CONTEXT}/lib/ || true
                fi
                rm -rf explode_tmp

                # create Dockerfile that keeps handler as the single CMD argument so
                # /lambda-entrypoint.sh sets _HANDLER and the base runtime loads classes
                cat > ${CONTEXT}/Dockerfile <<'EOF'
FROM public.ecr.aws/lambda/java:21

# Copy the fat jar (Spring Boot executable jar) for reference
COPY app.jar /var/task/app.jar

# Copy exploded classes and libs from the build context into the image
# This will place BOOT-INF/classes content at /var/task and BOOT-INF/lib jars at /var/task/lib
COPY . /var/task/

# Tell the Lambda runtime which handler to use (entrypoint expects a single argument)
ENV _HANDLER=com.service.config.handler.StreamLambdaHandler::handleRequest

## Use the base image entrypoint (/lambda-entrypoint.sh). Provide only the handler
# as CMD so the Lambda runtime sets up the process correctly and loads classes
# from /var/task and /var/task/lib (we previously exploded the jar into these paths).
CMD ["com.service.config.handler.StreamLambdaHandler::handleRequest"]
EOF
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    sh '''
                        # Force single-architecture build
                        DOCKER_BUILDKIT=1 docker build \
                            --platform linux/amd64 \
                            -t ${ECR_REPO}:${IMAGE_TAG} \
                            lambda-image
                        
                        # Verify local image architecture
                        docker inspect ${ECR_REPO}:${IMAGE_TAG} | grep Architecture
                    '''
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

                                            docker run --rm -v $(pwd)/lambda-image:/var/task -v $(pwd)/tmp_testloader:/tmp -w /tmp maven:3.9.9-eclipse-temurin-21 bash -c "javac -cp '/var/task/*:/var/task/lib/*' TestLoader.java && java -cp '/var/task/*:/var/task/lib/*:.' TestLoader" || true
                                        '''
            }
        }

        stage('Testar Execução do Handler no Container') {
            steps {
                echo '🧪 Testando execução do StreamLambdaHandler dentro do container...'
                sh '''
                    docker run --rm --entrypoint /bin/sh ${ECR_REPO}:${IMAGE_TAG} -c "
                        echo '▶️ Verificando se o container exige o handler como argumento (entrypoint) e exibe a eficiência do CMD...' &&
                        echo 'Entrypoint and CMD:' && ps aux || true
                    "
                '''
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
                    # Tag and push the image to ECR
                    docker tag ${ECR_REPO}:latest ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}:latest
                    docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}:latest
                    echo "⏳ Aguardando propagação da imagem no ECR..."
                    sleep 15

                    # Inspect the manifest in the registry and save to manifest.json for validation
                    echo '--- docker manifest inspect (remote) ---'
                    docker manifest inspect ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}:latest > manifest.json 2>/dev/null || true
                    echo '--- manifest.json ---'
                    cat manifest.json || true

                    # Validate manifest: detect if it's a manifest list or OCI index (multi-arch)
                    if grep -Eq '"manifests"|manifest.list|oci.image.index' manifest.json; then
                        echo "\n❌ Detected a multi-arch/OCI index manifest. AWS Lambda requires a single-arch image manifest (application/vnd.docker.distribution.manifest.v2+json)."
                        echo "Attempting an automatic rebuild (two attempts) with --platform linux/amd64 and re-push..."

                        # Attempt 1: normal docker build with explicit platform
                        echo "-> Rebuild attempt 1: docker build --platform linux/amd64"
                        DOCKER_BUILDKIT=1 docker build --platform linux/amd64 -t ${ECR_REPO}:${IMAGE_TAG} lambda-image || true
                        docker tag ${ECR_REPO}:${IMAGE_TAG} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}:latest || true
                        docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}:latest || true
                        echo "⏳ Aguardando propagação da imagem repush no ECR..."
                        sleep 15

                        # Re-inspect manifest
                        docker manifest inspect ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}:latest > manifest.json 2>/dev/null || true
                        echo '--- manifest.json after rebuild attempt 1 ---'
                        cat manifest.json || true

                        if grep -Eq '"manifests"|manifest.list|oci.image.index' manifest.json; then
                            echo "-> Rebuild attempt 1 produced multi-arch manifest; trying buildx --load fallback"

                            # Attempt 2: use buildx to build for linux/amd64 and --load into local daemon, then push
                            echo "-> Ensuring a buildx builder exists and is active"
                            docker buildx inspect default >/dev/null 2>&1 || docker buildx create --use --driver docker-container || true
                            echo "-> Rebuild attempt 2: docker buildx build --platform linux/amd64 --load"
                            docker buildx build --platform linux/amd64 --load -t ${ECR_REPO}:${IMAGE_TAG} lambda-image || true
                            docker tag ${ECR_REPO}:${IMAGE_TAG} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}:latest || true
                            docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}:latest || true
                            echo "⏳ Aguardando propagação da imagem repush (buildx) no ECR..."
                            sleep 15

                            # Re-inspect manifest after buildx fallback
                            docker manifest inspect ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}:latest > manifest.json 2>/dev/null || true
                            echo '--- manifest.json after buildx fallback ---'
                            cat manifest.json || true

                            if grep -Eq '"manifests"|manifest.list|oci.image.index' manifest.json; then
                                echo "\n❌ Automatic rebuilds (docker build and buildx --load) did not produce a single-arch manifest."
                                echo "Remediation: build on an amd64 host (or ensure builder loads single-arch image) or use skopeo to convert the manifest/media types."
                                exit 2
                            else
                                echo "✅ Auto-rebuild (buildx fallback) produced acceptable manifest. Proceeding."
                            fi
                        else
                            echo "✅ Auto-rebuild produced acceptable manifest. Proceeding."
                        fi
                    fi
                    '''
                }
            }
        }

      stage('Update Lambda with image digest and config') {
    steps {
        withAWS(region: "${AWS_REGION}", credentials: 'aws-credentials') {
            script {
                echo '🔁 Obtendo digest e atualizando Lambda'
                
                // Método 1: Tentar obter digest diretamente
                def digest = sh(
                    returnStdout: true,
                    script: """
                        aws ecr describe-images \
                            --repository-name ${ECR_REPO} \
                            --image-ids imageTag=${IMAGE_TAG} \
                            --region ${AWS_REGION} \
                            --query 'imageDetails[0].imageDigest' \
                            --output text
                    """
                ).trim()

                // Método 2: Fallback - obter digest via docker manifest
                if (!digest || digest == 'None') {
                    echo "⚠️  Tentando método alternativo para obter digest..."
                    digest = sh(
                        returnStdout: true,
                        script: """
                            docker manifest inspect ${ECR_URI}:${IMAGE_TAG} | \
                            grep -o '\"digest\": \"sha256:[a-f0-9]*\"' | \
                            head -1 | \
                            cut -d'"' -f4
                        """
                    ).trim()
                }

                if (!digest) {
                    error("❌ Não foi possível obter o digest da imagem")
                }

                echo "🔍 Digest: ${digest}"
                def imageWithDigest = "${ECR_URI}@${digest}"
                
                // Verificação rápida do tipo de manifest
                sh """
                    echo "🔎 Verificando tipo de manifest..."
                    docker manifest inspect ${ECR_URI}:${IMAGE_TAG} > manifest_final.json 2>/dev/null || true
                    
                    if grep -q '"mediaType".*"manifest.list"' manifest_final.json; then
                        echo "❌ ERRO: Imagem ainda é multi-arch"
                        exit 1
                    else
                        echo "✅ Imagem é single-arch - prosseguindo..."
                    fi
                """

                echo "🚀 Atualizando Lambda: ${LAMBDA_FUNCTION}"
                sh """
                    aws lambda update-function-code \
                        --function-name ${LAMBDA_FUNCTION} \
                        --image-uri ${imageWithDigest} \
                        --region ${AWS_REGION}
                """

                echo "✅ Lambda atualizada com sucesso!"
                
                // Aguardar a atualização completar
                sleep 10
                
                // Atualizar configurações
                echo "⚙️ Aplicando configurações..."
                sh """
                    aws lambda update-function-configuration \
                        --function-name ${LAMBDA_FUNCTION} \
                        --memory-size ${LAMBDA_MEMORY} \
                        --timeout ${LAMBDA_TIMEOUT} \
                        --region ${AWS_REGION}
                """
            }
        }
    }
}
        
    }

//       stage('Create Lambda Function') {
//            steps {
//                withAWS(region: "${AWS_REGION}", credentials: 'aws-credentials') {
//                    script {
//                       
//
//                        def deployImage = env.IMAGE_WITH_DIGEST ?: "${ECR_URI}:${IMAGE_TAG}"
//                        echo "🚀 Criando ou atualizando função Lambda '${LAMBDA_FUNCTION}' com imagem '${deployImage}'..."
//
//                       
//                        def checkResult = sh(returnStdout: true, script: '''
//                        if aws lambda get-function --function-name ${LAMBDA_FUNCTION} --region ${AWS_REGION} >/dev/null 2>&1; then
//                            echo "EXISTS"
//                        else
//                            echo "MISSING"
//                        fi
//                        ''').trim()
//
//                        if (checkResult == 'EXISTS') {
//                            echo "🔁 Função já existe — atualizando imagem..."
//
//                            
//                            sh '''
//                                MAX_WAIT=600
//                                SLEEP=5
//                                ELAPSED=0
//                                echo "⏳ Waiting for any existing Lambda update to finish (max $MAX_WAIT s)..."
//                                while [ $ELAPSED -lt $MAX_WAIT ]; do
//                                    status=$(aws lambda get-function-configuration --function-name ${LAMBDA_FUNCTION} --region ${AWS_REGION} --query 'LastUpdateStatus' --output text)
//                                    echo "Lambda LastUpdateStatus=$status"
//                                    if [ "$status" != "InProgress" ]; then
//                                        break
//                                    fi
//                                    sleep $SLEEP
//                                    ELAPSED=$((ELAPSED + SLEEP))
//                                done
//                                if [ $ELAPSED -ge $MAX_WAIT ]; then
//                                    echo "❌ Timeout waiting for existing Lambda update to finish after $MAX_WAIT s"
//                                    exit 1
//                                fi
//                            '''
//
//                           
//                            sh "aws lambda update-function-code --function-name ${LAMBDA_FUNCTION} --image-uri ${deployImage} --region ${AWS_REGION}"
//                        } else {
//                            echo "🆕 Criando nova função Lambda..."                            
//                            sh "aws lambda create-function --function-name ${LAMBDA_FUNCTION} --package-type Image --code ImageUri=${deployImage} --role ${ROLE_ARN} --region ${AWS_REGION}"
//                        }
//
//                        echo "✅ Função Lambda criada ou atualizada com sucesso!"
//                    }
//                }
//            }
//        }        
//
//        
//    }

    post {
        success {
            echo '✅ Deploy via container concluído com sucesso!'
        }
        failure {
            echo '❌ Falha no deploy!'
        }
    }
}
