pipeline{
    agent any

    environment{
        DOCKER_IMAGE_NAME = "dmitrykaplan/auth-server"
        DOCKER_IMAGE = ""
        TIMESTAMP = sh(script: 'date +%s', returnStdout: true).trim()
        KUBECONFIG = credentials('kube-config-path')
    }

    stages{
        stage("docker build"){
            steps{
               script {
                    DOCKER_IMAGE = docker.build DOCKER_IMAGE_NAME
                }
            }
        }

        stage("pushing docker image"){
            environment{
                registryCredential = 'dockerhub-credentials'
            }
            steps{
                script {
                    docker.withRegistry( 'https://registry.hub.docker.com', registryCredential ) {
                        DOCKER_IMAGE.push(TIMESTAMP)
                    }
                }
            }
        }

        stage("update configs"){
            steps{
                sh 'kubectl apply -f auth-server.yaml'
            }
        }

        stage("deploying auth-server deployment"){
            steps {
                script {
                    sh('KUBECONFIG=${KUBECONFIG}')
                    sh('kubectl set image deployments/auth-server-deployment auth-server=$DOCKER_IMAGE_NAME:$TIMESTAMP')
                }
            }
        }
    }
}