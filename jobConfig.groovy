pipelineJob ('Update-Home-Assistant-Config') {
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/mfugate1/home-assistant-config.git')
                    }
                    branch('main')
                }
                scriptPath('Jenkinsfile')
            }
        }
    }
    properties {
        parameters {
            parameterDefinitions {
                string {
                    name('updatedSecrets')
                    trim(true)
                }
            }
        }
        pipelineTriggers {
            triggers {
                GenericTrigger {
                    regexpFilterExpression('')
                    regexpFilterText('')
                    tokenCredentialId('JENKINS-HASS-CONFIG-UPDATE-TOKEN')
                }
            }
        }
    }
}