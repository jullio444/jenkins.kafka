≈
    stages {
      stage('checkout') {
        node {
          cleanWs() //clean the web space to make sure we are running in a clean folder
          checkout scm   //checkout the repository -- clone the repo
        }
      }
        stage('Build') {
            steps {
                sh 'mvn -B -DskipTests clean package'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        stage('Deliver') {
            steps {
                sh './jenkins/scripts/deliver.sh'
            }
        }
    }
