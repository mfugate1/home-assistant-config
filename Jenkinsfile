properties([
    disableConcurrentBuilds(), 
    parameters([string(name: 'updatedSecrets', trim: true)]),
    buildDiscarder(logRotator(numToKeepStr: '10'))
])

echo(params, true, '------ Job Parameters ------')

node ('built-in') {
    Map scmVars = checkout scm

    Map secrets = getAzureVaultSecrets()

    String hassUrl = "http://${secrets.DOCKER1_IP}:${secrets.HASS_PORT}"

    try {
        statusUpdate(hassUrl, secrets.HASS_TOKEN, "Checking Configuration")

        Map files = applySecrets('**/*.yaml', secrets)

        echo(files, true, '-------- Secrets in files ---------')

        String remote = "${DOCKER1_REMOTE_USER}@${secrets['DOCKER1_IP']}"
        String ssh = 'ssh -o StrictHostKeyChecking=no'
        String uniqueDirName = "${BRANCH_NAME}_${BUILD_NUMBER}"
        String testConfigDir = "${HASS_TEST_CONFIG_DIR}/${uniqueDirName}"

        sshagent (credentials: [JENKINS_SSH_KEY]) {
            try {
                sh "${ssh} ${remote} mkdir -p ${testConfigDir}"
                sh "rsync -e '${ssh}' -a --exclude '.git*' ./ ${remote}:${testConfigDir}"
                sh "${ssh} ${remote} cp -r ${HASS_CONFIG_DIR}/custom_components ${testConfigDir}/"
                String results = sh (
                    script: "${ssh} ${remote} docker exec home-assistant hass --script check_config -c /test-config/${uniqueDirName} -f",
                    returnStdout: true
                ).trim()
                echo results
                if (results.contains('ERROR')) error 'Invalid home assistant configuration'
            } finally {
                sh "${ssh} ${remote} rm -rf ${testConfigDir}"
            }
        }

        if (BRANCH_NAME != 'main') return

        statusUpdate(hassUrl, secrets.HASS_TOKEN, "Deploying Configuration")

        String protectFilters = [
            'alexa*',
            'blueprints*',
            'custom_components*',
            '*google*',
            '.HA_VERSION',
            'known_devices.yaml',
            '*.log*',
            'rtsp*',
            '.storage*',
            'themes*',
            'w*'
        ].collect{"--filter 'P ${it}'"}.join(' ')

        sshagent (credentials: [JENKINS_SSH_KEY]) {
            sh "rsync -e '${ssh}' -a --delete ${protectFilters} ./ ${remote}:${HASS_CONFIG_DIR}"
        }

        boolean scheduleRestart = false
        List reloadServices = []

        List affectedFiles = getAffectedFiles(files)
        echo(affectedFiles, true, '----------- Affected files -----------')

        for (String file in affectedFiles) {
            reloadServices += getReloadServices(file)
        }

        reloadServices = reloadServices.unique()

        if (reloadServices.contains('restart')) {
            echo "Scheduling a full restart"
            statusUpdate(hassUrl, secrets.HASS_TOKEN, "Restarting Server")
            reload(hassUrl, secrets.HASS_TOKEN, 'homeassistant', 'restart')
            return
        }

        echo "Restarting the following services: ${reloadServices}"
        for (String platform in reloadServices) {
            reload(hassUrl, secrets.HASS_TOKEN, platform)
        }

        statusUpdate(hassUrl, secrets.HASS_TOKEN, "Up To Date")
    } catch (Exception ex) {
        if (!(ex instanceof org.jenkinsci.plugins.workflow.steps.FlowInterruptedException)) {
            statusUpdate(hassUrl, secrets.HASS_TOKEN, "Error")
        }
        throw ex
    }
}

void reload(String url, String token, String platform, String service = 'reload') {
    httpRequest (
        url: "${url}/api/services/${platform}/${service}",
        httpMode: 'POST',
        customHeaders: [[name: 'Authorization', value: "Bearer ${token}", maskValue: true]]
    )
}

@NonCPS
List getAffectedFiles(Map files) {
    List affectedFiles = []

    if (currentBuild.changeSets[0]) {
        affectedFiles += currentBuild.changeSets[0].items.collectMany{it.affectedPaths}
    }

    if (params.updatedSecrets) {
        List updatedSecrets = params.updatedSecrets.tokenize(',')
        files.each { file, secrets ->
            for (String secret in updatedSecrets) {
                if (secrets.contains(secret)) {
                    affectedFiles += file
                    break
                }
            }
        }
    }

    return affectedFiles.unique()
}

List getReloadServices(String file) {
    if (file.startsWith('automation/')) return ['automation']
    if (file.startsWith('input/')) return [file.tokenize('/')[-1] - '.yaml']
    if (file.startsWith('light/')) return ['restart']
    if (file.startsWith('lovelace/')) return []
    if (file.startsWith('misc/')) {
        if (file.endsWith('person.yaml')) return ['person']
        if (file.endsWith('group.yaml')) return ['group']
        return ['restart']
    }
    if (file.startsWith('script/')) return ['script']
    if (file.startsWith('template/')) return ['template']

    if (file == 'configuration.yaml') return ['restart']

    if (!file.endsWith('.yaml')) return []

    if (!fileExists(file)) return []

    String contents = readFile(file)

    if (file.startsWith('packages/')) return parseYaml(readYaml(file: file))

    List reload = []
    List reloadablePlatforms = ['history_stats', 'mqtt', 'ping', 'rest', 'template', 'time_date', 'universal']
    for (String line in contents.tokenize('\n').findAll{it.contains('- platform:')}) {
        for (String platform in reloadablePlatforms) {
            if (line.contains(platform) && platform != 'time_date') {
                reload += platform
            } else {
                reload += 'restart'
            }
        }
    }

    return reload.unique()
}

@NonCPS
List parseYaml(Map yaml) {
    List reloadDomains = ['automation', 'group', 'template']
    List restartDomains = ['shell_command', 'switch']
    List reloadPlatforms = ['command_line', 'history_stats', 'mqtt', 'ping', 'rest', 'time_date', 'universal']
    List reload = []
    yaml.each { key, value ->
        if (reloadDomains.contains(key) || key.startsWith('input_')) {
            reload += key
        } else if (restartDomains.contains(key)) {
            reload += 'restart'
        } else {
            value.each { platform ->
                if (reloadPlatforms.contains(platform.platform)) {
                    reload += platform.platform
                } else {
                    reload += 'restart'
                }
            }
        }
    }
    return reload
}

void statusUpdate(String hassUrl, String token, String option) {
    if (BRANCH_NAME == 'main') {
        httpRequest (
            url: "${hassUrl}/api/services/input_select/select_option",
            httpMode: 'POST',
            requestBody: """{"entity_id": "input_select.hass_deployment_status", "option": "${option}"}""",
            customHeaders: [[name: 'Authorization', value: "Bearer ${token}", maskValue: true]]
        )
    }
}
