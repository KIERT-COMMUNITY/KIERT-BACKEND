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
        booleanParam(
                name: 'RUN_TESTS',
                defaultValue: true,
                description: 'Ejecutar pruebas unitarias'
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

        stage('Clean & Install Dependencies') {
            steps {
                bat '''
                    echo "Limpiando e instalando dependencias..."
                    mvn clean install -DskipTests
                '''
            }
        }

        stage('Run Tests') {
            when {
                expression { params.RUN_TESTS == true }
            }
            steps {
                bat '''
                    echo "Ejecutando pruebas unitarias..."
                    mvn test
                '''
            }
            post {
                // ✅ SOLO ejecutar junit si existen tests
                success {
                    script {
                        def testResults = findFiles(glob: '**/target/surefire-reports/*.xml')
                        if (testResults.size() > 0) {
                            junit '**/target/surefire-reports/*.xml'
                        } else {
                            echo "No se encontraron resultados de pruebas"
                        }
                    }
                }
                failure {
                    script {
                        def testResults = findFiles(glob: '**/target/surefire-reports/*.xml')
                        if (testResults.size() > 0) {
                            junit '**/target/surefire-reports/*.xml'
                        } else {
                            echo "No se encontraron resultados de pruebas"
                        }
                    }
                }
            }
        }

        stage('Build JAR') {
            steps {
                bat '''
                    echo "Construyendo JAR..."
                    mvn package -DskipTests
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
                    echo "JAR generado en target/"
                    dir target {
                        dir /b *.jar || echo "No hay JAR"
                    }
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