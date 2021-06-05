pipeline {
  agent any
  tools {nodejs}
     stages {
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
       stage('Code Quality Check via SonarQube') {
       steps {
           script {
           def scannerHome = tool 'sonarqube';
               withSonarQubeEnv("sonarqube-container") {
               sh "${tool("sonarqube")}/bin/sonar-scanner \
               -Dsonar.projectKey=test-node-js \
               -Dsonar.sources=. \
               -Dsonar.css.node=. \
               -Dsonar.host.url=http://209.200.39.233:9000 \
               -Dsonar.login=df9b5d31c30a7866fca8b5a7c1d63bf51e76a608"
                   }
               }
           }
       }
        stage("Install Project Dependencies") {
        steps {
            nodejs(nodeJSInstallationName: 'nodenv'){
                sh "npm install"
                }
            }
        }
     }
}




