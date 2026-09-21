pipeline {
    agent any

    tools {
        // ⚠️ Los nombres DEBEN coincidir exactamente con Global Tool Configuration de Jenkins
        jdk 'JDK17'
        maven 'maven-3.9'
    }

    parameters {
        choice(
                name: 'ENVIRONMENT',
                choices: ['development', 'staging', 'production'],
                description: 'Selecciona el entorno para el despliegue'
        )
        string(
                name: 'BRANCH',
                defaultValue: 'develop10',
                description: 'Rama a construir'
        )
        booleanParam(
                name: 'RUN_TESTS',
                defaultValue: true,
                description: 'Ejecutar pruebas unitarias'
        )
        booleanParam(
                name: 'RUN_SONAR',
                defaultValue: false,
                description: 'Ejecutar análisis de SonarQube (requiere servidor configurado)'
        )
        booleanParam(
                name: 'BUILD_DOCKER',
                defaultValue: false,
                description: 'Construir imagen Docker (solo staging/production)'
        )
    }

    environment {
        SONAR_PROJECT_KEY  = 'kiert-backend'
        SONAR_PROJECT_NAME = 'KIERT-BACKEND'
        IMAGE_NAME         = 'kiert-backend'
        IMAGE_TAG          = "${env.BUILD_NUMBER}"
    }

    stages {

        stage('Checkout') {
            steps {
                cleanWs()
                checkout scmGit(
                        branches: [[name: "*/${params.BRANCH}"]],
                        userRemoteConfigs: [[
                                                    url: 'https://github.com/KIERT-COMMUNITY/KIERT-BACKEND.git',
                                                    credentialsId: 'Ardamins'
                                            ]]
                )
                script {
                    currentBuild.description = "Build #${BUILD_NUMBER} - ${params.BRANCH} - ${params.ENVIRONMENT}"
                }
            }
        }

        stage('Setup JDK & Maven') {
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
                    mvn clean package -DskipTests -B
                '''
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Test') {
            when {
                expression { params.RUN_TESTS == true }
            }
            steps {
                bat '''
                    echo "Ejecutando pruebas unitarias..."
                    mvn test -B
                '''
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube Analysis') {
            when {
                expression { params.RUN_SONAR == true }
            }
            steps {
                withSonarQubeEnv('SonarQube') {
                    bat """
                        echo "Ejecutando analisis de SonarQube..."
                        mvn sonar:sonar -B ^
                            -Dsonar.projectKey=${env.SONAR_PROJECT_KEY} ^
                            -Dsonar.projectName=${env.SONAR_PROJECT_NAME} ^
                            -Dsonar.projectVersion=${env.BUILD_NUMBER} ^
                            -Dsonar.sources=src/main/java ^
                            -Dsonar.tests=src/test/java ^
                            -Dsonar.java.binaries=target/classes ^
                            -Dsonar.java.test.binaries=target/test-classes
                    """
                }
            }
        }

        stage('Quality Gate') {
            when {
                expression { params.RUN_SONAR == true }
            }
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Docker Build') {
            when {
                expression {
                    params.BUILD_DOCKER == true &&
                            (params.ENVIRONMENT == 'production' || params.ENVIRONMENT == 'staging')
                }
            }
            steps {
                bat """
                    echo "Construyendo imagen Docker..."
                    docker build -t ${env.IMAGE_NAME}:${env.IMAGE_TAG} -t ${env.IMAGE_NAME}:latest .
                """
            }
        }

        stage('Deploy') {
            when {
                expression {
                    params.ENVIRONMENT == 'production' || params.ENVIRONMENT == 'staging'
                }
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