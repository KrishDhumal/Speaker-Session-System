// Declarative pipeline for the Speaker Session Management System.
//
// Commit -> build -> unit tests -> Docker image -> deploy container ->
// Selenium tests against that container -> publish image -> health check.
// A failure after the deploy stage rolls back to the previous image.

pipeline {
    agent any

    parameters {
        string(name: 'APP_PORT', defaultValue: '8081',
               description: 'Host port the deployed container listens on')
        string(name: 'APP_ENV', defaultValue: 'ci',
               description: 'Value shown in the application footer')
        string(name: 'REGISTRY', defaultValue: 'docker.io/krishdhumal',
               description: 'Docker Hub user or local registry host:port')
        booleanParam(name: 'PUSH_IMAGE', defaultValue: false,
               description: 'Push the image to the registry (needs credentials)')
    }

    environment {
        IMAGE_NAME    = 'speaker-session-manager'
        // A unique, traceable tag per build instead of overwriting :latest.
        IMAGE_TAG     = "1.0.${BUILD_NUMBER}"
        CONTAINER     = "speaker-session-${params.APP_ENV}"
        // Where the Selenium stage points its browser.
        APP_URL       = "http://localhost:${params.APP_PORT}"
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
    }

    triggers {
        // Ask Git every 5 minutes whether there are new commits.
        // Replace with githubPush() once a webhook is configured.
        pollSCM('H/5 * * * *')
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --pretty="%h %an %s"'
            }
        }

        stage('Build') {
            steps {
                // Integration tests are skipped here; they need the deployed app.
                sh 'mvn -B clean package -DskipITs'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.war', fingerprint: true
                }
            }
        }

        stage('Unit tests') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker image') {
            steps {
                sh """
                    docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${IMAGE_NAME}:latest .
                    docker image ls ${IMAGE_NAME}
                """
            }
        }

        stage('Deploy container') {
            steps {
                script {
                    // Remember what is currently running so we can roll back.
                    env.PREVIOUS_IMAGE = sh(
                        script: "docker inspect --format='{{.Config.Image}}' ${CONTAINER} 2>/dev/null || echo none",
                        returnStdout: true).trim()
                    echo "Previous image: ${env.PREVIOUS_IMAGE}"
                }
                sh """
                    docker rm -f ${CONTAINER} 2>/dev/null || true
                    docker run -d --name ${CONTAINER} \
                        -p ${params.APP_PORT}:8080 \
                        -e APP_ENV=${params.APP_ENV} \
                        -e SEED_DEMO_DATA=false \
                        ${IMAGE_NAME}:${IMAGE_TAG}
                """
                // Wait for the container's own HEALTHCHECK to report healthy.
                sh """
                    for i in \$(seq 1 30); do
                        status=\$(docker inspect --format='{{.State.Health.Status}}' ${CONTAINER})
                        echo "health: \$status"
                        [ "\$status" = "healthy" ] && exit 0
                        sleep 5
                    done
                    echo "Container never became healthy"; docker logs --tail 50 ${CONTAINER}; exit 1
                """
            }
        }

        stage('Selenium tests') {
            steps {
                // The same suite that runs locally, pointed at the container.
                sh "mvn -B verify -Dit.base.url=${APP_URL} -Dsurefire.skip=true"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/failsafe-reports/*.xml'
                    // Screenshots and page dumps of any failed journey.
                    archiveArtifacts artifacts: 'target/selenium-screenshots/*',
                                     allowEmptyArchive: true
                }
            }
        }

        stage('Publish image') {
            when { expression { return params.PUSH_IMAGE } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub',
                                                  usernameVariable: 'REG_USER',
                                                  passwordVariable: 'REG_PASS')]) {
                    sh """
                        echo "\$REG_PASS" | docker login ${params.REGISTRY} -u "\$REG_USER" --password-stdin
                        docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${params.REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
                        docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${params.REGISTRY}/${IMAGE_NAME}:latest
                        docker push ${params.REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
                        docker push ${params.REGISTRY}/${IMAGE_NAME}:latest
                        docker logout ${params.REGISTRY}
                    """
                }
            }
        }
    }

    post {
        success {
            echo "Deployed ${IMAGE_NAME}:${IMAGE_TAG} at ${APP_URL}"
        }
        failure {
            script {
                // Roll back only if a previous image exists; otherwise just clean up.
                if (env.PREVIOUS_IMAGE && env.PREVIOUS_IMAGE != 'none') {
                    echo "Rolling back to ${env.PREVIOUS_IMAGE}"
                    sh """
                        docker rm -f ${CONTAINER} 2>/dev/null || true
                        docker run -d --name ${CONTAINER} \
                            -p ${params.APP_PORT}:8080 \
                            -e APP_ENV=${params.APP_ENV} \
                            ${env.PREVIOUS_IMAGE}
                    """
                } else {
                    echo 'No previous image to roll back to; removing the failed container.'
                    sh "docker rm -f ${CONTAINER} 2>/dev/null || true"
                }
            }
        }
        always {
            cleanWs(deleteDirs: true, notFailBuild: true)
        }
    }
}
