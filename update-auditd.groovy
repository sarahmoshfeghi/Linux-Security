pipeline {
    agent { label 'node01' }

    parameters {
        string(name: 'TARGET_IP', defaultValue: '', description: 'Enter a valid IPv4 address (e.g., 192.168.0.1). Format: XXX.XXX.XXX.XXX, where each XXX is between 0 and 255.')
    }

    environment {
        GIT_REPO = 'https://github.com/sarahmoshfeghi/Linux-Security.git'
        PLAYBOOK_NAME = 'linux-auditd-update-configuration.yml'
        USER_NAME = 'user'
        GIT_REPO_CREDENTIAL ='credintial'


        GITLAB_FILE_BASE_URL = 'https://github.com/saramoshfeghi/linux-Security/api/v4/projects/id/repository/files'
        GITLAB_FILE_NAME = 'auditd.rules'
        GITLAB_BRANCH = 'main'
        GITLAB_TOKEN_ID = 'tokenid'
    }

    stages {
        stage('Validate IP Address') {
            steps {
                script {
                    def ipPattern = ~/^(([0-9]{1,3})\.){3}([0-9]{1,3})$/

                    if (!params.TARGET_IP ==~ ipPattern) {
                        error "Invalid IP address: ${params.TARGET_IP}. Please enter a valid IPv4 address."
                    }

                    def octets = params.TARGET_IP.tokenize('.')
                    def valid = octets.every { it.toInteger() >= 0 && it.toInteger() <= 255 }

                    if (!valid) {
                        error "Invalid IP address: ${params.TARGET_IP}. Each octet must be between 0 and 255."
                    }

                    echo "IP address ${params.TARGET_IP} is valid."
                }
            }
        }

        stage('Clean Workspace') {
            steps {
                script {
                    deleteDir()
                    echo "Workspace cleaned."
                }
            }
        }

        stage('Clone Ansible Playbooks Repo') {
            steps {
                script {
                    checkout([$class: 'GitSCM', branches: [[name: '*/main']],
                        userRemoteConfigs: [[url: env.GIT_REPO, credentialsId: env.GIT_REPO_CREDENTIAL]]
                    ])
                }
            }
        }

        stage('Download Tagging Files from GitLab') {
            steps {
                script {
                    withCredentials([string(credentialsId: env.GITLAB_TOKEN_ID, variable: 'GITLAB_TOKEN')]) {
                        def baseUrl = env.GITLAB_FILE_BASE_URL
                        def branch = env.GITLAB_BRANCH
                        // Use the environment variable as the source path
                        def sourcePath = env.GITLAB_FILE_NAME
                        
                        // Properly encode the path for the API URL
                        def encodedPath = java.net.URLEncoder.encode(sourcePath, "UTF-8")
                        def fileUrl = "${baseUrl}/${encodedPath}/raw?ref=${branch}"
                        
                        // Extract just the filename from the path (e.g., 'auditd.rules')
                        def fileName = sourcePath.substring(sourcePath.lastIndexOf('/') + 1)

                        echo "Downloading ${fileName} from ${fileUrl}"
                        try {
                            def response = httpRequest(
                                url: fileUrl,
                                customHeaders: [[name: 'PRIVATE-TOKEN', value: "${GITLAB_TOKEN}"]],
                                validResponseCodes: '200'
                            )
                            writeFile file: fileName, text: response.content
                            echo "File downloaded successfully: ${fileName}"
                        } catch (Exception e) {
                            error "Failed to download ${fileName} from ${fileUrl}: ${e.message}"
                        }
                    }
                }
            }
        }

        stage('Run Playbook') {
            steps {
                script {
                    def ansibleCommand = "ansible-playbook -i ${params.TARGET_IP}, ${PLAYBOOK_NAME} -u ${USER_NAME} -v"
                    
                    def output = sh(script: ansibleCommand, returnStdout: true).trim()

                    echo "Output from Ansible playbook:\n${output}"
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline execution finished.'
        }
    }
}
