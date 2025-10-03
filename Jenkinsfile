pipeline {
    agent any

    environment {
        AWS_DEFAULT_REGION = "us-east-1"
        LAMBDA_NAME = "microsservico-atentdimento"
        JAR_FILE = "target/service-0.0.1-SNAPSHOT-aws.jar"
        SONARQUBE = "SonarQube" 
        PATH = "/usr/sbin:${env.PATH}"
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'master', url: 'https://github.com/Gustavo12386/deploy-microsservico-service-sistema-gerenciamento-de-atendimento'
            }
        }        

        stage('Start Services') {
            steps {
               sh 'docker compose -f infra/docker-compose.yml up -d'
            }
        }

         stage('Prepare Maven Wrapper') {
            steps {
                sh 'chmod +x mvnw'
            }
        }

        stage('Build') {
            steps {
               sh './mvnw clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh './mvnw test'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv("${SONARQUBE}") {
                    sh "./mvnw sonar:sonar -Dsonar.projectKey=meu-projeto -Dsonar.host.url=http://localhost:9000 -Dsonar.login=${env.SONAR_AUTH_TOKEN}"
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 1, unit: 'HOURS') { 
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Deploy') {
            when {
                expression {                    
                    currentBuild.result == null || currentBuild.result == 'SUCCESS'
                }
            } 
            steps {
                withAWS(credentials: 'aws-jenkins-credentials', region: "${AWS_DEFAULT_REGION}") {
                    sh """
                    aws lambda update-function-code \
                        --function-name ${LAMBDA_NAME} \
                        --zip-file fileb://${JAR_FILE}
                    """
                }
            }
        }

        stage('Stop Infra') {
            steps {
                echo " Finalizando serviços de apoio"
                sh 'docker compose -f infra/docker-compose.yml down || true'
            }
        }
    }

    post {
        success {
            echo "🚀 Deploy realizado com sucesso!"
        }
        failure {
            echo "❌ Deploy falhou!"
        }
    }
}
