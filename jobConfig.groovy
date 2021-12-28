multibranchPipelineJob('Update-Home-Assistant-Config') {
    branchSources {
        github {
            id('home-assistant-config')
            repoOwner('mfugate1')
            repository('home-assistant-config')
            scanCredentialsId('GITHUB')
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
}
