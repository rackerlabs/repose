pipeline {
    agent any
    stages {
        stage('Test') {
            steps {
                retry(3) {
                    sh 'echo "Inside retry block."'
                }

                sh 'echo "Hello World"'
                sh '''
                    echo "Multiline shell steps works too"
                    ls -lah
                '''
            }
        }
    }
}
