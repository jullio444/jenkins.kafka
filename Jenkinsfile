// Jenkinsfile

try {
  stage('checkout') {
    node {
      cleanWs() //clean the web space to make sure we are running in a clean folder
      checkout scm   //checkout the repository -- clone the repo
    }
  }

  if (env.BRANCH_NAME == 'master') {
        stage('build') {
            steps {
                sh 'mvn -B -DskipTests clean package'
            }
        }

        stage('test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }


//     // Run terraform apply
//         stage('Deliver') {
//             steps {
//                 sh './jenkins/scripts/deliver.sh'
//             }
//         }

  }
  currentBuild.result = 'SUCCESS'
}
catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException flowError) {
  currentBuild.result = 'ABORTED'
}
catch (err) {
  currentBuild.result = 'FAILURE'
  throw err
}
finally {
  if (currentBuild.result == 'SUCCESS') {
    currentBuild.result = 'SUCCESS'
  }
}
