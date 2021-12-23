multibranchPipelineJob('Update-Home-Assistant-Config') {
    branchSources {
        github {
            id('home-assistant-config')
            repoOwner('mfugate1')
            repository('home-assistant-config')
            buildOriginPRMerge(true)
            buildOriginPRHead(true)
            buildForkPRMerge(false)
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