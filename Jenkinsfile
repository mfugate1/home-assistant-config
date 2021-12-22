String hassConfigDir = '/docker/home-assistant-v2'
String remoteUser = 'jenkins'

node ('docker') {
    Map scmVars = checkout scm

    Map secrets = getAzureVaultSecrets()
    Map files = applySecrets(secrets)

    sshagent (credentials: ['docker1-ssh']) {
        sh "rsync -e 'ssh -o StrictHostKeyChecking=no' -a ./ ${remoteUser}@${secrets['DOCKER1-IP']}:${hassConfigDir}"
    }

    boolean scheduleRestart = false
    List reloadServices = []

    for (String file in getAffectedFiles(files)) {
        reloadServices += files[file].reload
    }

    reloadServices = reloadServices.unique()

    echo "Restarting the following services: ${reloadServices}"

    if (reloadServices.contains('restart')) {
        echo "Scheduling a full restart"
    }
}


@NonCPS
List getAffectedFiles(Map files) {
    List affectedFiles = currentBuild.changeSets[0].items.collectMany{it.affectedPaths}

    if (params.updatedSecrets) {
        List updatedSecrets = params.updatedSecrets.tokenize(',')
        files.each { file, info ->
            for (String secret in updatedSecrets) {
                if (info.secrets.contains(secret)) {
                    affectedFiles += file
                    break
                }
            }
        }
    }

    return affectedFiles.unique()
}

// Returns a map of files with their secrets and services
Map applySecrets(Map secrets) {
    Map files = [:]
    echo(secrets, true, '-------- Secrets --------')
    for (String file in findFiles(glob: '**/*.yaml').collect{it.path}) {
        echo "Adding secrets to ${file}"
        String contents = readFile(file)
        writeFile (
            file: file,
            text: secretReplacement(contents, secrets)
        )
        files[file] = [
            secrets: getSecretsInFile(contents, secrets),
            reload: getReloadServices(file, contents)
        ]
    }
}

@NonCPS
String secretReplacement(String contents, Map secrets) {
    secrets.each { secret, value ->
        contents = contents.replaceAll('\${' + secret + '}', value)
    }
    return contents
}

@NonCPS
List getSecretsInFile(String contents, Map secrets) {
    return secrets.findAll{contents.contains("\${${it.key}}")}.collect{it.key}
}

List getReloadServices(String file, String contents) {
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

    if (file.startsWith('packages/')) return parseYaml(readYaml(file: file))

    List reload = []
    List reloadablePlatforms = ['history_stats', 'mqtt', 'ping', 'rest', 'time_date', 'universal']
    for (String line in contents.tokenize('\n').findAll{it.contains('- platform:')}) {
        for (String platform in reloadablePlatforms) {
            if (line.contains(platform)) {
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