pipeline {
    agent any

    parameters {
        choice(
                name: 'ENVIRONMENT',
                choices: ['development', 'staging', 'production'],
                description: 'Selecciona el entorno para el despliegue'
        )
        string(
                name: 'BRANCH',
                defaultValue: 'develop',
                description: 'Rama a construir'
        )
    }

    stages {

        stage('Checkout') {
            steps {
                cleanWs()
                checkout scmGit(
                        branches: [[name: "*/${params.BRANCH}"]],
                        userRemoteConfigs: [[
                                                    url: 'https://github.com/KIERT-COMMUNITY/KIERT-BACKEND.git',
                                                    credentialsId: 'github-credentials'
                                            ]]
                )
                script {
                    currentBuild.description = "Build #${BUILD_NUMBER} - ${params.BRANCH} - ${params.ENVIRONMENT}"
                }
            }
        }

        stage('Setup JDK 21 & Maven') {
            steps {
                bat '''
                    echo "Verificando Java..."
                    java -version
                    echo "Verificando Maven..."
                    mvn -version
                '''
            }
        }

        stage('Build JAR') {
            steps {
                bat '''
                    echo "Construyendo JAR..."
                    mvn clean package -DskipTests
                '''
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Deploy') {
            when {
                expression { params.ENVIRONMENT == 'production' || params.ENVIRONMENT == 'staging' }
            }
            steps {
                bat """
                    echo "Desplegando a ${params.ENVIRONMENT}..."
                    echo "Build #${BUILD_NUMBER} - ${params.BRANCH}"
                """
            }
        }
    }

    post {
        success {
            bat """
                echo "Pipeline completado exitosamente!"
                echo "Build: #${BUILD_NUMBER}"
                echo "Rama: ${params.BRANCH}"
                echo "Entorno: ${params.ENVIRONMENT}"
                echo "URL: ${BUILD_URL}"
            """
        }
        failure {
            bat """
                echo "Pipeline fallo!"
                echo "Build: #${BUILD_NUMBER}"
                echo "Rama: ${params.BRANCH}"
                echo "Entorno: ${params.ENVIRONMENT}"
                echo "URL: ${BUILD_URL}"
            """
        }
        always {
            cleanWs()
            bat 'echo "Limpiando workspace..."'
        }
    }
}