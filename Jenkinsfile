pipeline {
    agent any

    environment {
        AWS_REGION = 'us-east-1'
        S3_BUCKET  = 'lambda-deploys-gustavo'
        LAMBDA_FUNCTION = 'microsservico-atendimento'
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
       
         stage('Ensure S3 Bucket Exists') {
            steps {
                withAWS(region: 'us-east-1', credentials: 'aws-credentials') {
                   script{
                      echo "🔍 Verificando se o bucket ${S3_BUCKET} existe..."
                      def result = sh(script: "aws s3api head-bucket --bucket ${S3_BUCKET} 2>&1 || true", returnStdout: true).trim()
                      if (result.contains("Not Found") || result.contains("404") || result.contains("NoSuchBucket")) {
                            echo "⚠️ Bucket ${S3_BUCKET} não encontrado. Criando..."
                            sh "aws s3 mb s3://${S3_BUCKET} --region ${AWS_REGION}"
                            echo "✅ Bucket criado com sucesso!"
                        } else {
                            echo "✅ Bucket ${S3_BUCKET} já existe!"
                        }
                   } 
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
                        echo "🚀 Enviando arquivo .jar para o S3..."
                        aws s3 cp target/service-0.0.1-SNAPSHOT.jar s3://${S3_BUCKET}/service-latest.jar --region ${AWS_REGION}
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
                                --arn:aws:iam::381492003133:role/lambda-deploy-role \
                                --handler com.service.config.handler.LambdaHandler::handleRequest \
                                --code S3Bucket=${S3_BUCKET},S3Key=service-latest.jar \
                                --region ${AWS_REGION}
                        fi
                    '''
                }
            }
        }

         stage('Create Function URL (with CORS)') {
            steps {
                withAWS(region: "${AWS_REGION}", credentials: 'aws-credentials') {
                    sh '''
                    echo "🌐 Configurando Function URL com CORS..."
                    aws lambda create-function-url-config \
                        --function-name ${LAMBDA_FUNCTION} \
                        --auth-type NONE \
                        --cors "AllowOrigins=['*'],AllowMethods=['GET','POST','PUT','DELETE','OPTIONS'],AllowHeaders=['*']" \
                        --region ${AWS_REGION} || echo "🔄 Function URL já existe."

                    echo "✅ Endpoint da Lambda:"
                    aws lambda get-function-url-config --function-name ${LAMBDA_FUNCTION} --region ${AWS_REGION}
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
