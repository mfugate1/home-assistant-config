properties([disableConcurrentBuilds(), parameters([string(name: 'updatedSecrets', trim: true)])])

node ('docker') {
    Map scmVars = checkout scm

    Map secrets = getAzureVaultSecrets()
    Map files = applySecrets('**/*.yaml', secrets)

    echo(files, true, '-------- Secrets in files ---------')

    String remote = "${DOCKER1_REMOTE_USER}@${secrets['DOCKER1_IP']}"
    String ssh = 'ssh -o StrictHostKeyChecking=no'
    String testConfigDir = "${HASS_TEST_CONFIG_DIR}/${BRANCH_NAME}_${BUILD_NUMBER}"

    sshagent (credentials: ['docker1-ssh']) {
        sh "rsync -e '${ssh}' -a ./ ${remote}:${HASS_TEST_CONFIG_DIR}"
        sh "${ssh} ${remote} rm -rf ${HASS_TEST_CONFIG_DIR}/custom_components"
        sh "${ssh} ${remote} cp -r ${HASS_CONFIG_DIR}/custom_components ${testConfigDir}/"
        sh "${ssh} ${remote} docker exec home-assistant hass --script check_config -c ${testConfigDir} -f"
    }

    if (BRANCH_NAME != 'main') return

    sshagent (credentials: ['docker1-ssh']) {
        sh "rsync -e '${ssh}' -a ./ ${remote}:${HASS_CONFIG_DIR}"
    }

    boolean scheduleRestart = false
    List reloadServices = []

    List affectedFiles = getAffectedFiles(files)
    echo(affectedFiles, true, '----------- Affected files -----------')

    for (String file in affectedFiles) {
        reloadServices += getReloadServices(file)
    }

    reloadServices = reloadServices.unique()

    if (!reloadServices) {
        echo "Nothing to reload"
        return
    }

    String hassUrl = "http://${secrets.DOCKER1_IP}:${secrets.HASS_PORT}"

    if (reloadServices.contains('restart')) {
        echo "Scheduling a full restart"
        reload(hassUrl, secrets.HASS_TOKEN, 'homeassistant', 'restart')
        return
    }

    echo "Restarting the following services: ${reloadServices}"
    for (String platform in reloadServices) {
        reload(hassUrl, secrets.HASS_TOKEN, platform)
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
    if (currentBuild.changeSets[0] == null) return []

    List affectedFiles = currentBuild.changeSets[0].items.collectMany{it.affectedPaths}

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
    List reloadablePlatforms = ['history_stats', 'mqtt', 'ping', 'rest', 'time_date', 'universal']
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
    List restartDomains = ['switch']
    List reloadPlatforms = ['history_stats', 'mqtt', 'ping', 'rest', 'time_date', 'universal']
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