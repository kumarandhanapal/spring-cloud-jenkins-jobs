package org.springframework.jenkins.cloud.compatibility

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import javaposse.jobdsl.dsl.helpers.step.StepContext
import org.springframework.jenkins.cloud.common.AllCloudConstants

/**
 * @author Marcin Grzejszczak
 */
@PackageScope
@CompileStatic
abstract class CompatibilityTasks {

	protected static final String DEFAULT_BOOT_VERSION = AllCloudConstants.LATEST_BOOT_VERSION
	protected static final String SPRING_BOOT_VERSION_VAR = 'SPRING_BOOT_VERSION'
	protected static final String SPRING_VERSION_VAR = 'SPRING_VERSION'
	protected static final String SPRING_CLOUD_BUILD_BRANCH = 'SPRING_CLOUD_BUILD_BRANCH'

	Closure defaultStepsForBoot() {
		return buildStep {
			shell compileProductionForBoot()
			shell(printDepsForBoot())
		}
	}

	protected String printDepsForBoot() {
		return """
					echo -e "Printing the list of dependencies"
					./mvnw dependency:tree -U -Dspring-boot.version=\$${SPRING_BOOT_VERSION_VAR}
			"""
	}

	Closure defaultStepsWithTestsForBoot() {
		return buildStep {
			shell runTestsForBoot()
			shell(printDepsForBoot())
		}
	}

	protected String compileProductionForBoot() {
		return """#!/bin/bash
					${bumpBoot()}
					echo -e "Checking if prod code compiles against latest boot"
					./mvnw clean package -U -fae -Dspring-boot.version=\$${SPRING_BOOT_VERSION_VAR} -DskipTests"""
	}

	protected String runTestsForBoot() {
		return """#!/bin/bash
					${bumpBoot()}
					echo -e "Checking if the project can be built with Boot version [\$${SPRING_BOOT_VERSION_VAR}]"
					./mvnw clean install -U -fae"""
	}

	protected String bumpBoot() {
		return """
		echo -e "Will:\\n1)Download releaser\\n2)Clone SC-Build\\n3)Use releaser to bump boot for SC-Build\\n4)Install new SC-Build locally\\n5)Build the project"
		mkdir -p target
		pushd target
		echo -e "Downloading the releaser"
		../mvnw dependency:get -DremoteRepositories=http://repo.spring.io/libs-snapshot-local -Dartifact=org.springframework.cloud.internal:spring-cloud-release-tools-spring:1.0.0.BUILD-SNAPSHOT -Dtransitive=false
		../mvnw dependency:copy -Dartifact=org.springframework.cloud.internal:spring-cloud-release-tools-spring:1.0.0.BUILD-SNAPSHOT
		echo -e "Cloning Spring Cloud Build"
		git clone https://github.com/spring-cloud/spring-cloud-build.git
		pushd spring-cloud-build
		echo -e "Updating SC-Build's Boot version [\$${SPRING_BOOT_VERSION_VAR}]"
		java -jar ../dependency/spring-cloud-release-tools-spring-1.0.0*.jar --releaser.git.fetch-versions-from-git=false --"releaser.fixed-versions[spring-boot-dependencies]=\$${SPRING_BOOT_VERSION_VAR}" -u -i=false
		./mvnw clean install -DskipTests
		popd
		popd
"""
	}

	Closure defaultStepsForSpring() {
		return buildStep {
			shell compileProductionForSpring()
			shell(printDepsForSpring())
		}
	}

	protected String printDepsForSpring() {
		return """
					echo -e "Printing the list of dependencies"
					./mvnw dependency:tree -U -Dspring.version=\$${SPRING_VERSION_VAR}
			"""
	}

	Closure defaultStepsWithTestsForSpring() {
		return buildStep {
			shell runTestsForSpring()
			shell(printDepsForSpring())
		}
	}

	protected String compileProductionForSpring() {
		return """
					echo -e "Checking if prod code compiles against latest spring"
					./mvnw clean compile -U -fae -Dspring.version=\$${SPRING_VERSION_VAR}"""
	}

	protected String runTestsForSpring() {
		return """
					echo -e "Checking if prod code compiles against latest spring"
					./mvnw clean install -U -fae -Dspring.version=\$${SPRING_VERSION_VAR}"""
	}

	private Closure buildStep(@DelegatesTo(StepContext) Closure buildSteps) {
		return buildSteps
	}

}
