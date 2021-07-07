String hassConfigDir = '/docker/home-assistant-v2'

properties (
    [
        disableConcurrentBuilds(),
        pipelineTriggers([
            [$class: 'GenericTrigger',
                token: 'Home-Assistant-Deployment',
                causeString: 'Github Webhook'
            ]
        ]),
    ]
)

node ('docker') {
    Map scmVars = checkout scm
    echo(scmVars, true)
    sh 'env'

    sh "mkdir -p ${hassConfigDir}"


}