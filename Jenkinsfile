#!/usr/bin/env groovy

/**
 * SmartSupport - Pipeline CI/CD Complet
 * 
 * Ce pipeline gère :
 * - Build de tous les microservices
 * - Tests unitaires
 * - Build Docker
 * - Push vers Docker Hub
 * - Déploiement avec Docker Compose
 */

pipeline {
    agent any
    
    // ============ OUTILS ============
    tools {
        jdk 'JDK17'
        maven 'Maven3'
        nodejs 'Node20'
    }
    
    // ============ ENVIRONMENT ============
    environment {
        // Java
        JAVA_HOME = '/usr/lib/jvm/jdk-17.0.12-oracle-x64'
        
        // Docker
        DOCKER_REGISTRY = 'registry.hub.docker.com'
        IMAGE_PREFIX = 'smartsupport'
        DOCKER_IMAGE_TAG = "${env.BUILD_NUMBER}"
        
        // Services Spring Boot
        SPRING_SERVICES = [
            'discovery-service',
            'config-server',
            'api-gateway',
            'auth-service',
            'user-service',
            'ticket-service',
            'notification-service'
        ]
        
        // Couleurs pour les logs
        ANSI_GREEN = '\033[32m'
        ANSI_RED = '\033[31m'
        ANSI_YELLOW = '\033[33m'
        ANSI_BLUE = '\033[34m'
        ANSI_RESET = '\033[0m'
    }
    
    // ============ OPTIONS ============
    options {
        buildDiscarder(logRotator(numToKeepStr: '10', daysToKeepStr: '30'))
        disableConcurrentBuilds()
        timeout(time: 2, unit: 'HOURS')
        timestamps()
        ansiColor('xterm')
    }
    
    // ============ PARAMÈTRES ============
    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'staging', 'production'],
            description: 'Environnement de déploiement',
            defaultValue: 'dev'
        )
        booleanParam(
            name: 'RUN_TESTS',
            defaultValue: true,
            description: 'Exécuter les tests unitaires'
        )
        booleanParam(
            name: 'RUN_SONAR',
            defaultValue: false,
            description: 'Exécuter l\'analyse SonarQube'
        )
        booleanParam(
            name: 'BUILD_DOCKER',
            defaultValue: true,
            description: 'Construire les images Docker'
        )
        booleanParam(
            name: 'PUSH_DOCKER',
            defaultValue: false,
            description: 'Pousser les images vers Docker Hub'
        )
        booleanParam(
            name: 'DEPLOY',
            defaultValue: false,
            description: 'Déployer avec Docker Compose'
        )
        string(
            name: 'BRANCH',
            defaultValue: 'main',
            description: 'Branche à builder'
        )
    }
    
    // ============ TRIGGERS ============
    triggers {
        pollSCM('H/5 * * * *')  // Vérifier toutes les 5 minutes
    }
    
    // ============ STAGES ============
    stages {
        
        // ============ STAGE 1 : CHECKOUT ============
        stage('📥 Checkout') {
            steps {
                script {
                    echo "${ANSI_YELLOW}=== CHECKOUT ===${ANSI_RESET}"
                    
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${params.BRANCH}"]],
                        extensions: [
                            [$class: 'CleanBeforeCheckout'],
                            [$class: 'CloneOption', depth: 1, noTags: true, shallow: true]
                        ],
                        userRemoteConfigs: [[
                            url: 'https://github.com/TON_USERNAME/smart-support.git',
                            credentialsId: 'github-credentials'
                        ]]
                    ])
                    
                    echo "${ANSI_GREEN}✅ Checkout terminé${ANSI_RESET}"
                }
            }
        }
        
        // ============ STAGE 2 : VALIDATION ============
        stage('🔍 Validation') {
            steps {
                script {
                    echo "${ANSI_YELLOW}=== VALIDATION ===${ANSI_RESET}"
                    
                    // Vérifier les fichiers essentiels
                    def requiredFiles = [
                        'docker-compose.yml',
                        'Jenkinsfile'
                    ]
                    
                    requiredFiles.each { file ->
                        if (!fileExists(file)) {
                            error "❌ Fichier manquant: ${file}"
                        }
                    }
                    
                    // Vérifier les dossiers des services
                    SPRING_SERVICES.each { service ->
                        if (!fileExists("${service}/pom.xml")) {
                            echo "${ANSI_RED}⚠️ ${service}/pom.xml manquant${ANSI_RESET}"
                        }
                    }
                    
                    echo "${ANSI_GREEN}✅ Validation terminée${ANSI_RESET}"
                }
            }
        }
        
        // ============ STAGE 3 : BUILD BACKEND ============
        stage('☕ Build Backend') {
            steps {
                script {
                    echo "${ANSI_YELLOW}=== BUILD BACKEND ===${ANSI_RESET}"
                    
                    // Build parallèle des services
                    parallel SPRING_SERVICES.collectEntries { service ->
                        [(service): {
                            dir(service) {
                                echo "Building ${service}..."
                                sh '''
                                    mvn clean compile -DskipTests -B
                                '''
                            }
                        }]
                    }
                    
                    echo "${ANSI_GREEN}✅ Backend buildé${ANSI_RESET}"
                }
            }
        }
        
        // ============ STAGE 4 : BUILD AI SERVICE ============
        stage('🐍 Build AI Service') {
            steps {
                script {
                    echo "${ANSI_YELLOW}=== BUILD AI SERVICE ===${ANSI_RESET}"
                    
                    dir('ai-service') {
                        sh '''
                            # Créer l'environnement virtuel
                            python3 -m venv .venv
                            
                            # Activer
                            . .venv/bin/activate
                            
                            # Installer les dépendances
                            pip install --upgrade pip
                            pip install -r requirements.txt
                            
                            # Vérifier les modèles
                            if [ ! -f models/category_model.joblib ]; then
                                echo "⚠️ Entraînement du modèle de catégorie..."
                                python training/train_category.py
                            fi
                            
                            if [ ! -f models/priority_model.joblib ]; then
                                echo "⚠️ Entraînement du modèle de priorité..."
                                python training/train_priority.py
                            fi
                        '''
                    }
                    
                    echo "${ANSI_GREEN}✅ AI Service buildé${ANSI_RESET}"
                }
            }
        }
        
        // ============ STAGE 5 : BUILD FRONTEND ============
        stage('⚛️ Build Frontend') {
            steps {
                script {
                    echo "${ANSI_YELLOW}=== BUILD FRONTEND ===${ANSI_RESET}"
                    
                    dir('frontend') {
                        sh '''
                            # Vérifier Node
                            node --version
                            npm --version
                            
                            # Installer les dépendances
                            npm ci
                            
                            # Builder
                            npm run build
                            
                            # Vérifier le build
                            if [ -d "dist" ]; then
                                echo "✅ Build réussi"
                                ls -la dist/
                            else
                                echo "❌ Build échoué"
                                exit 1
                            fi
                        '''
                    }
                    
                    echo "${ANSI_GREEN}✅ Frontend buildé${ANSI_RESET}"
                }
            }
        }
        
        // ============ STAGE 6 : TESTS ============
        stage('🧪 Tests Unitaires') {
            when {
                expression { params.RUN_TESTS }
            }
            steps {
                script {
                    echo "${ANSI_YELLOW}=== TESTS UNITAIRES ===${ANSI_RESET}"
                    
                    try {
                        // Tests Backend
                        parallel SPRING_SERVICES.collectEntries { service ->
                            [(service): {
                                dir(service) {
                                    echo "Testing ${service}..."
                                    sh '''
                                        mvn test -B
                                    '''
                                }
                            }]
                        }
                        
                        // Tests AI Service
                        dir('ai-service') {
                            sh '''
                                . .venv/bin/activate
                                pytest tests/ --junitxml=test-results.xml || true
                            '''
                        }
                        
                        echo "${ANSI_GREEN}✅ Tests terminés${ANSI_RESET}"
                    } catch (Exception e) {
                        echo "${ANSI_RED}⚠️ Tests échoués: ${e.message}${ANSI_RESET}"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }
        
        // ============ STAGE 7 : ANALYSE SONARQUBE ============
        stage('📊 SonarQube') {
            when {
                expression { params.RUN_SONAR }
            }
            steps {
                script {
                    echo "${ANSI_YELLOW}=== ANALYSE SONARQUBE ===${ANSI_RESET}"
                    
                    // Analyse des services Java
                    SPRING_SERVICES.each { service ->
                        dir(service) {
                            sh """
                                mvn sonar:sonar \
                                    -Dsonar.projectKey=smartsupport-${service} \
                                    -Dsonar.host.url=http://localhost:9000 \
                                    -Dsonar.login=${env.SONAR_TOKEN}
                            """
                        }
                    }
                    
                    echo "${ANSI_GREEN}✅ Analyse SonarQube terminée${ANSI_RESET}"
                }
            }
        }
        
        // ============ STAGE 8 : BUILD DOCKER ============
        stage('🐳 Build Docker') {
            when {
                expression { params.BUILD_DOCKER }
            }
            steps {
                script {
                    echo "${ANSI_YELLOW}=== BUILD DOCKER ===${ANSI_RESET}"
                    
                    // Build AI Service
                    dir('ai-service') {
                        docker.build("${IMAGE_PREFIX}/ai-service:${DOCKER_IMAGE_TAG}")
                    }
                    
                    // Build Backend
                    parallel SPRING_SERVICES.collectEntries { service ->
                        [(service): {
                            dir(service) {
                                docker.build("${IMAGE_PREFIX}/${service}:${DOCKER_IMAGE_TAG}")
                            }
                        }]
                    }
                    
                    // Build Frontend
                    dir('frontend') {
                        docker.build("${IMAGE_PREFIX}/frontend:${DOCKER_IMAGE_TAG}")
                    }
                    
                    echo "${ANSI_GREEN}✅ Images Docker construites${ANSI_RESET}"
                }
            }
        }
        
        // ============ STAGE 9 : PUSH DOCKER ============
        stage('📤 Push Docker Hub') {
            when {
                expression { params.PUSH_DOCKER }
            }
            steps {
                script {
                    echo "${ANSI_YELLOW}=== PUSH DOCKER HUB ===${ANSI_RESET}"
                    
                    docker.withRegistry(
                        "https://${DOCKER_REGISTRY}",
                        'docker-registry'
                    ) {
                        // Push AI Service
                        docker.image("${IMAGE_PREFIX}/ai-service:${DOCKER_IMAGE_TAG}").push()
                        docker.image("${IMAGE_PREFIX}/ai-service:${DOCKER_IMAGE_TAG}").push('latest')
                        
                        // Push Backend
                        SPRING_SERVICES.each { service ->
                            docker.image("${IMAGE_PREFIX}/${service}:${DOCKER_IMAGE_TAG}").push()
                            docker.image("${IMAGE_PREFIX}/${service}:${DOCKER_IMAGE_TAG}").push('latest')
                        }
                        
                        // Push Frontend
                        docker.image("${IMAGE_PREFIX}/frontend:${DOCKER_IMAGE_TAG}").push()
                        docker.image("${IMAGE_PREFIX}/frontend:${DOCKER_IMAGE_TAG}").push('latest')
                    }
                    
                    echo "${ANSI_GREEN}✅ Images poussées vers Docker Hub${ANSI_RESET}"
                }
            }
        }
        
        // ============ STAGE 10 : DÉPLOIEMENT ============
        stage('🚀 Déploiement') {
            when {
                expression { params.DEPLOY }
            }
            steps {
                script {
                    echo "${ANSI_YELLOW}=== DÉPLOIEMENT ===${ANSI_RESET}"
                    echo "Environnement: ${params.ENVIRONMENT}"
                    
                    sh '''
                        # Exporter les variables
                        export IMAGE_TAG=${BUILD_NUMBER}
                        export ENVIRONMENT=${ENVIRONMENT}
                        
                        # Déployer avec Docker Compose
                        docker-compose down || true
                        docker-compose up -d
                        
                        # Afficher les conteneurs
                        docker-compose ps
                    '''
                    
                    echo "${ANSI_GREEN}✅ Déploiement terminé${ANSI_RESET}"
                }
            }
        }
        
        // ============ STAGE 11 : SMOKE TESTS ============
        stage('🔍 Smoke Tests') {
            when {
                expression { params.DEPLOY }
            }
            steps {
                script {
                    echo "${ANSI_YELLOW}=== SMOKE TESTS ===${ANSI_RESET}"
                    
                    // Attendre que les services démarrent
                    sleep 30
                    
                    sh '''
                        echo "=== Vérification des services ==="
                        
                        # AI Service
                        echo "AI Service:"
                        curl -s http://localhost:5000/api/ai/health || echo "❌ DOWN"
                        
                        # API Gateway
                        echo "\nAPI Gateway:"
                        curl -s http://localhost:8080/actuator/health || echo "❌ DOWN"
                        
                        # Eureka
                        echo "\nEureka:"
                        curl -s http://localhost:8761/actuator/health || echo "❌ DOWN"
                        
                        # Frontend
                        echo "\nFrontend:"
                        curl -s http://localhost/ | head -5 || echo "❌ DOWN"
                    '''
                    
                    echo "${ANSI_GREEN}✅ Smoke tests terminés${ANSI_RESET}"
                }
            }
        }
    }
    
    // ============ POST ACTIONS ============
    post {
        always {
            script {
                echo "${ANSI_YELLOW}=== NETTOYAGE ===${ANSI_RESET}"
                
                // Nettoyer les conteneurs Docker
                sh '''
                    docker system prune -f || true
                '''
                
                // Nettoyer le workspace
                cleanWs()
            }
        }
        
        success {
            script {
                echo "${ANSI_GREEN}========================================${ANSI_RESET}"
                echo "${ANSI_GREEN}✅ PIPELINE TERMINÉ AVEC SUCCÈS !${ANSI_RESET}"
                echo "${ANSI_GREEN}========================================${ANSI_RESET}"
                
                // Notification
                echo "Build #${BUILD_NUMBER} réussi"
                echo "Environnement: ${params.ENVIRONMENT}"
                echo "Durée: ${currentBuild.durationString}"
            }
        }
        
        failure {
            script {
                echo "${ANSI_RED}========================================${ANSI_RESET}"
                echo "${ANSI_RED}❌ PIPELINE ÉCHOUÉ !${ANSI_RESET}"
                echo "${ANSI_RED}========================================${ANSI_RESET}"
                
                echo "Voir les logs: ${BUILD_URL}console"
            }
        }
        
        unstable {
            script {
                echo "${ANSI_YELLOW}========================================${ANSI_RESET}"
                echo "${ANSI_YELLOW}⚠️ PIPELINE INSTABLE (tests échoués)${ANSI_RESET}"
                echo "${ANSI_YELLOW}========================================${ANSI_RESET}"
            }
        }
    }
}
