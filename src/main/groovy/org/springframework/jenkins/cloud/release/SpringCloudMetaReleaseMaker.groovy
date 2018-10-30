package org.springframework.jenkins.cloud.release

import groovy.transform.builder.Builder
import javaposse.jobdsl.dsl.DslFactory

import org.springframework.jenkins.cloud.common.AllCloudConstants
import org.springframework.jenkins.cloud.common.SpringCloudJobs
import org.springframework.jenkins.cloud.common.SpringCloudNotification
import org.springframework.jenkins.common.job.JdkConfig
import org.springframework.jenkins.common.job.TestPublisher

/**
 * @author Marcin Grzejszczak
 */
class SpringCloudMetaReleaseMaker implements JdkConfig, TestPublisher,
		SpringCloudJobs {
	private static final String RELEASER_CONFIG_PARAM = "RELEASER_CONFIG"
	private static final String START_FROM_PARAM = "START_FROM"
	private static final String TASK_NAMES_PARAM = "TASK_NAMES"
	private static final String RELEASER_POM_THIS_TRAIN_BOM= 'RELEASER_POM_THIS_TRAIN'
	private static final String RELEASER_SAGAN_UPDATE_VAR= 'RELEASER_SAGAN_UPDATE'
	private static final String RELEASER_GIT_UPDATE_DOCUMENTATION_REPOS_VAR = 'RELEASER_GIT_UPDATE_DOCUMENTATION_REPOS'
	private static final String RELEASER_GIT_UPDATE_SPRING_PROJECTS_VAR = 'RELEASER_GIT_UPDATE_SPRING_PROJECTS'
	private static final String RELEASER_GIT_UPDATE_RELEASE_TRAIN_WIKI_VAR = 'RELEASER_GIT_UPDATE_RELEASE_TRAIN_WIKI'
	private static final String RELEASER_GIT_RUN_UPDATED_SAMPLES_VAR = 'RELEASER_GIT_RUN_UPDATED_SAMPLES'
	private static final String RELEASER_GIT_UPDATE_ALL_TEST_SAMPLES_VAR = 'RELEASER_GIT_UPDATE_ALL_TEST_SAMPLES'
	private static final String RELEASER_GIT_UPDATE_RELEASE_TRAIN_DOCS_VAR = 'RELEASER_GIT_UPDATE_RELEASE_TRAIN_DOCS'
	private static final String RELEASER_GIT_UPDATE_SPRING_GUIDES_VAR = 'RELEASER_GIT_UPDATE_SPRING_GUIDES'
	private static final String RELEASER_RELEASE_TRAIN_PROJECT_NAME_VAR = 'RELEASER_META_RELEASE_RELEASE_TRAIN_PROJECT_NAME'
	private static final String RELEASER_GIT_RELEASE_TRAIN_BOM_URL_VAR= 'RELEASER_GIT_RELEASE_TRAIN_BOM'
	private static final String RELEASER_PROJECTS_TO_SKIP_VAR= 'RELEASER_PROJECTS_TO_SKIP'

	private final DslFactory dsl

	SpringCloudMetaReleaseMaker(DslFactory dsl) {
		this.dsl = dsl
	}

	void release(String jobName, ReleaserOptions options = new ReleaserOptions()) {
		dsl.job(jobName) {
			parameters {
				textParam(RELEASER_CONFIG_PARAM, options.releaserVersions, "Properties file used by the meta-releaser")
				stringParam(START_FROM_PARAM, "", "Project name from which you'd like to start the meta-release process. E.g. spring-cloud-sleuth")
				stringParam(TASK_NAMES_PARAM, "", "Comma separated list of project names. E.g. spring-cloud-sleuth,spring-cloud-contract")
				booleanParam(RELEASER_SAGAN_UPDATE_VAR, options.updateSagan, 'If true then will update documentation repository with the current URL')
				booleanParam(RELEASER_GIT_UPDATE_DOCUMENTATION_REPOS_VAR, options.updateDocumentationRepos, 'If true then will update documentation repository with the current URL')
				booleanParam(RELEASER_GIT_UPDATE_SPRING_PROJECTS_VAR, options.updateSpringProjects, 'If true then will update Project Sagan with the current release train values')
				booleanParam(RELEASER_GIT_UPDATE_RELEASE_TRAIN_WIKI_VAR, options.updateReleaseTrainWiki, 'If true then will update the release train wiki page with the current release train values')
				booleanParam(RELEASER_GIT_RUN_UPDATED_SAMPLES_VAR, options.runUpdatedSamples, 'If true then will update samples and run the the build')
				booleanParam(RELEASER_GIT_UPDATE_ALL_TEST_SAMPLES_VAR, options.updateAllTestSamples, ' If true then will update samples with bumped snapshots after release')
				booleanParam(RELEASER_GIT_UPDATE_RELEASE_TRAIN_DOCS_VAR, options.updateReleaseTrainDocs, ' If true then will update the release train documentation project and run the generation')
				booleanParam(RELEASER_GIT_UPDATE_SPRING_GUIDES_VAR, options.updateSpringGuides, ' If true then will update the release train documentation project and run the generation')
				stringParam(RELEASER_RELEASE_TRAIN_PROJECT_NAME_VAR, options.releaseTrainProjectName, 'Name of the project that represents the BOM of the release train')
				stringParam(RELEASER_GIT_RELEASE_TRAIN_BOM_URL_VAR, options.releaseTrainBomUrl, 'Subfolder of the pom that contains the versions for the release train')
				stringParam(RELEASER_POM_THIS_TRAIN_BOM, options.releaseThisTrainBom, 'URL to a project containing a BOM. Defaults to Spring Cloud Release Git repository')
				stringParam(RELEASER_PROJECTS_TO_SKIP_VAR, options.projectsToSkip, 'Names of projects to skip deployment for meta-release')
			}
			jdk jdk8()
			scm {
				git {
					remote {
						url "https://github.com/spring-cloud/spring-cloud-release-tools"
						branch masterBranch()
					}
					extensions {
						wipeOutWorkspace()
					}
				}
			}
			label(releaserLabel())
			wrappers {
				timestamps()
				colorizeOutput()
				maskPasswords()
				credentialsBinding {
					usernamePassword(dockerhubUserNameEnvVar(),
							dockerhubPasswordEnvVar(),
							dockerhubCredentialId())
					usernamePassword(githubRepoUserNameEnvVar(),
							githubRepoPasswordEnvVar(),
							githubUserCredentialId())
					file(gpgSecRing(), "spring-signing-secring.gpg")
					file(gpgPubRing(), "spring-signing-pubring.gpg")
					string(gpgPassphrase(), "spring-gpg-passphrase")
					string(githubToken(), githubTokenCredId())
					usernamePassword(sonatypeUser(), sonatypePassword(),
							"oss-token")
				}
				timeout {
					noActivity(300)
					failBuild()
					writeDescription('Build failed due to timeout after {0} minutes of inactivity')
				}
			}
			steps {
				// build the releaser
				shell("""#!/bin/bash
				mkdir -p target
				echo "Building the releaser. Please wait..."
				./mvnw clean install > "target/releaser.log"
				echo "Run the meta-releaser!"
				${setupGitCredentials()}
				rm -rf config && mkdir -p config && echo "\$${RELEASER_CONFIG_PARAM}" > config/releaser.properties
				set +x
				SYSTEM_PROPS="-Dgpg.secretKeyring="\$${gpgSecRing()}" -Dgpg.publicKeyring="\$${gpgPubRing()}" -Dgpg.passphrase="\$${gpgPassphrase()}" -DSONATYPE_USER="\$${sonatypeUser()}" -DSONATYPE_PASSWORD="\$${sonatypePassword()}""
				if [[ \${$START_FROM_PARAM} != "" ]]; then
					START_FROM_OPTS="--start-from '\${$START_FROM_PARAM}'"
				fi
				if [[ \${$TASK_NAMES_PARAM} != "" ]]; then
					TASK_NAMES_OPTS="--task-names '\${$TASK_NAMES_PARAM}'"
				fi
				echo "Start from opts [\${START_FROM_OPTS}], task names [\${TASK_NAMES_OPTS}]"
				java -Dreleaser.git.username="\$${githubRepoUserNameEnvVar()}" \\
						-Dreleaser.git.password="\$${githubRepoPasswordEnvVar()}" \\
						-jar spring-cloud-release-tools-spring/target/spring-cloud-release-tools-spring-1.0.0.BUILD-SNAPSHOT.jar \\
						--releaser.meta-release.release-train-project-name=\${$RELEASER_RELEASE_TRAIN_PROJECT_NAME_VAR} \\ 
						--releaser.meta-release.projects-to-skip=\${$RELEASER_PROJECTS_TO_SKIP_VAR} \\ 
						--releaser.git.release-train-bom-url=\${$RELEASER_GIT_RELEASE_TRAIN_BOM_URL_VAR} \\ 
						--releaser.pom.this-train-bom=\${$RELEASER_POM_THIS_TRAIN_BOM} \\
						--releaser.maven.wait-time-in-minutes=180 \\
						--spring.config.name=releaser \\
						--releaser.maven.system-properties="\${SYSTEM_PROPS}" \\
						--interactive=false \\
						--meta-release=true \\
						--releaser.sagan.update-sagan=\${$RELEASER_SAGAN_UPDATE_VAR} \\
						--releaser.git.update-documentation-repo=\${$RELEASER_GIT_UPDATE_DOCUMENTATION_REPOS_VAR} \\
						--releaser.git.update-spring-project=\${$RELEASER_GIT_UPDATE_SPRING_PROJECTS_VAR} \\
						--releaser.git.update-release-train-wiki=\${$RELEASER_GIT_UPDATE_RELEASE_TRAIN_WIKI_VAR} \\
						--releaser.git.run-updated-samples=\${$RELEASER_GIT_RUN_UPDATED_SAMPLES_VAR} \\
						--releaser.git.update-all-test-samples=\${$RELEASER_GIT_UPDATE_ALL_TEST_SAMPLES_VAR} \\
						--releaser.git.update-release-train-docs=\${$RELEASER_GIT_UPDATE_RELEASE_TRAIN_DOCS_VAR} \\
						--releaser.git.update-spring-guides=\${$RELEASER_GIT_UPDATE_SPRING_GUIDES_VAR}
						--full-release \${START_FROM_OPTS} \${TASK_NAMES_OPTS} || exit 1
				${cleanGitCredentials()}
				""")
			}
			configure {
				SpringCloudNotification.cloudSlack(it as Node) {
					notifyFailure()
					notifySuccess()
					notifyUnstable()
					includeFailedTests(false)
					includeTestSummary(false)
				}
			}
			publishers {
				archiveJunit mavenJUnitResults()
			}
		}
	}

	private String gpgSecRing() {
		return 'FOO_SEC'
	}

	private String gpgPubRing() {
		return 'FOO_PUB'
	}

	private String gpgPassphrase() {
		return 'FOO_PASSPHRASE'
	}

	private String sonatypeUser() {
		return 'SONATYPE_USER'
	}

	private String sonatypePassword() {
		return 'SONATYPE_PASSWORD'
	}

	private String githubToken() {
		return 'RELEASER_GIT_OAUTH_TOKEN'
	}

	private String githubTokenCredId() {
		return '7b3ebbea-7001-479b-8578-b8c464dab973'
	}

}
