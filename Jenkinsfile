pipeline {
    agent any

    environment {
        AWS_REGION = 'us-east-1'
        ECR_REPO   = 'microsservico-atendimento'
        AWS_ACCOUNT_ID = '381492003133'
        LAMBDA_FUNCTION = 'microsservico-atendimento'
        PATH = "/var/lib/jenkins/.local/bin:${env.PATH}"
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'master', url: 'https://github.com/Gustavo12386/deploy-microsservico-service-sistema-gerenciamento-de-atendimento'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    sh 'docker build -t ${ECR_REPO}:latest .'
                }
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
                    '''
                }
            }
        }

        stage('Deploy Lambda Image') {
            steps {
                withAWS(region: "${AWS_REGION}", credentials: 'aws-credentials') {
                    script {
                        sh '''
                        IMAGE_URI=${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}:latest

                        if aws lambda get-function --function-name ${LAMBDA_FUNCTION} >/dev/null 2>&1; then
                            echo "🔄 Atualizando função existente..."
                            aws lambda update-function-code \
                                --function-name ${LAMBDA_FUNCTION} \
                                --image-uri $IMAGE_URI \
                                --region ${AWS_REGION}
                        else
                            echo "🆕 Criando nova função Lambda com imagem..."
                            aws lambda create-function \
                                --function-name ${LAMBDA_FUNCTION} \
                                --package-type Image \
                                --code ImageUri=$IMAGE_URI \
                                --role arn:aws:iam::${AWS_ACCOUNT_ID}:role/lambda-deploy-role \
                                --region ${AWS_REGION}
                        fi
                        '''
                    }
                }
            }
        }

        stage('Create Function URL (with CORS)') {
            steps {
                withAWS(region: "${AWS_REGION}", credentials: 'aws-credentials') {
                    sh '''
                    echo "🌐 Configurando Function URL com CORS..."
                    cat > cors-config.json <<EOF
                    {
                        "AllowOrigins": ["*"],
                        "AllowMethods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
                        "AllowHeaders": ["*"]
                    }
                    EOF

                    aws lambda create-function-url-config \
                        --function-name ${LAMBDA_FUNCTION} \
                        --auth-type NONE \
                        --cors file://cors-config.json \
                        --region ${AWS_REGION} || echo "🔄 Function URL já existe."

                    aws lambda get-function-url-config --function-name ${LAMBDA_FUNCTION} --region ${AWS_REGION}
                    '''
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
