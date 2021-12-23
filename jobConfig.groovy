multibranchPipelineJob('Update-Home-Assistant-Config') {
    branchSources {
        git {
            id('home-assistant-config')
            remote('https://github.com/mfugate1/home-assistant-config.git')
            credentialsId('github-credentials')
        }
    }
    factory {
        workflowBranchProjectFactory {
            scriptPath('Jenkinsfile')
        }
    }
    orphanedItemStrategy {
        defaultOrphanedItemStrategy {
            daysToKeepStr('1')
            numToKeepStr('-1')
            pruneDeadBranches(true)
        }
        discardOldItems {
            numToKeep(10)
        }
    }
}