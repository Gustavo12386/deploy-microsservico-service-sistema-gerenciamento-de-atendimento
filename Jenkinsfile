pipeline {
    agent any

    environment {
        AWS_REGION = 'us-east-1'
        S3_BUCKET  = 'lambda-deploys-gustavo'
        LAMBDA_FUNCTION = 'microservice-atentdimento'
        JAR_FILE = 'target/service-0.0.1-SNAPSHOT.jar'
        PATH = "/var/lib/jenkins/.local/bin:${env.PATH}"
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'master', url: 'https://github.com/Gustavo12386/deploy-microsservico-service-sistema-gerenciamento-de-atendimento'
            }
        } 

        stage('Prepare Maven Wrapper') {
            steps {                
                sh 'chmod +x mvnw'
            }
        }

        stage('Build') {
            steps {
                echo '🔨 Compilando o projeto com Maven...'
                sh './mvnw clean package -DskipTests'
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

        stage('Check AWS Account') {
            steps {
                withAWS(region: 'us-east-1', credentials: 'aws-credentials') {
                    sh '''
                        echo "🔍 Verificando conta AWS associada..."
                        aws sts get-caller-identity
                    '''
                }
            }
        }


        stage('Upload to S3') {
            steps {
                  withCredentials([[
                    $class: 'AmazonWebServicesCredentialsBinding', 
                    credentialsId: 'aws-credentials'
                ]]){
                     sh '''
                        aws lambda update-function-code \
                            --function-name ${LAMBDA_FUNCTION} \
                            --s3-bucket ${S3_BUCKET} \
                            --s3-key service-latest.jar \
                            --region ${AWS_REGION}
                    '''
                }
            }
        }

        stage('Deploy Lambda') {
            steps {
                   withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws-credentials']]) {
                    sh '''
                        if ! aws lambda get-function --function-name ${LAMBDA_FUNCTION} >/dev/null 2>&1; then
                            echo "Lambda não existe, criando..."
                            aws lambda create-function \
                                --function-name ${LAMBDA_FUNCTION} \
                                --runtime java17 \
                                --role arn:aws:iam::381492003133:role/lambda-deploy-policy \
                                --handler com.seu.pacote.MainHandler::handleRequest \
                                --code S3Bucket=${S3_BUCKET},S3Key=service-latest.jar \
                                --region ${AWS_REGION}
                        fi
                    '''
                }
            }
        }
    }

    post {
        success {
            echo '✅ Deploy realizado com sucesso!'
        }
        failure {
            echo '❌ O deploy falhou!'
        }
    }
}
