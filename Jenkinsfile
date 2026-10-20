// Discover origin branches, origin PR heads and new v* tags. No fork PRs on shared agents.
pipeline {
    agent { dockerfile { filename 'ci/Dockerfile'; args '-u root --entrypoint=""' } }
    options {
        timestamps()
        timeout(time: 45, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactDaysToKeepStr: '14'))
    }
    stages {
        stage('Toolchain') {
            steps {
                sh '''
                    set -eu
                    java -version
                    git config --global --add safe.directory "$WORKSPACE"
                    git clean -fdx -q
                    chmod +x gradlew
                '''
            }
        }
        stage('Compile and validate') {
            steps {
                sh './gradlew verifyKotlinIterationSafety verifyNoDuplicateClasses :runtime:compileKotlin :runtime-desktop:compileKotlin :editor:compileKotlin :editor-desktop:compileKotlin --no-daemon --stacktrace'
            }
        }
        stage('Tests') {
            steps { sh './gradlew :runtime:test --no-daemon --stacktrace' }
        }
        stage('Frame budgets') {
            steps { sh './gradlew :runtime:perfCheck --no-daemon --stacktrace' }
        }
        stage('Release') {
            when { allOf { not { changeRequest() }; tag pattern: 'v*', comparator: 'GLOB' } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'token',
                        usernameVariable: 'GITHUB_ACTOR', passwordVariable: 'GH_TOKEN')]) {
                    sh '''
                        set -eu
                        gh release create "$TAG_NAME" --repo Frotty/Meta --target "$GIT_COMMIT" --generate-notes --title "$TAG_NAME"
                    '''
                }
                // JitPack warming was informational in Actions too.
                sh 'curl --fail --silent --show-error --location "https://jitpack.io/com/github/Frotty/Meta/$TAG_NAME/" -o /dev/null || echo "JitPack warm-up failed; the release was published."'
            }
        }
    }
    post {
        always {
            junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
        }
        failure {
            archiveArtifacts allowEmptyArchive: true, artifacts: '**/build/reports/tests/**'
        }
        cleanup { sh 'chmod -R a+rwX "$WORKSPACE" 2>/dev/null || true' }
    }
}
