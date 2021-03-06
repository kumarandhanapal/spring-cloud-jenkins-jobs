package org.springframework.jenkins.cloud.common

import groovy.transform.CompileStatic

/**
 * Constants used by all cloud jobs (sometimes traits can't easily pass const values)
 *
 * @author Marcin Grzejszczak
 */
@CompileStatic
class AllCloudConstants {
	/**
	 * The minor against which the compatibility builds should be checked
	 */
	public static final String BOOT_MINOR_FOR_API_COMPATIBILITY = '2.2'

	/**
	 * Latest version of Boot to be checked. Used in some E2E test (e.g. Camden vs latest Boot)
	 * and in compatibility builds
	 */
	public static final String LATEST_2_2_BOOT_VERSION = '2.2.6.BUILD-SNAPSHOT'

	/**
	 * Spring Cloud Build branch used for compatibility builds.
	 */
	public static final String SPRING_CLOUD_BUILD_BRANCH_FOR_COMPATIBILITY_BUILD = '2.3.x'

	/**
	 * Latest version of Boot to be checked. Used in some E2E test (e.g. Camden vs latest Boot)
	 * and in compatibility builds
	 */
	public static final String LATEST_SPRING_VERSION = '5.0.0.BUILD-SNAPSHOT'

	/**
	 * List of skipped projects for Spring Cloud meta release
	 */
	public static final String DEFAULT_RELEASER_SKIPPED_PROJECTS = "spring-boot,spring-cloud-stream,spring-cloud-task"

	/**
	 * Latest version of Boot to be checked. Used in some E2E test (e.g. Camden vs latest Boot)
	 * and in compatibility builds
	 */
	public static final String DEFAULT_STREAM_RELEASER_SKIPPED_PROJECTS = "spring-boot,spring-cloud-build,spring-cloud-function"

}
