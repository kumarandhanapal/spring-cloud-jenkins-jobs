package cloudpipelines

import org.springframework.jenkins.common.view.DashboardViewBuilder
import javaposse.jobdsl.dsl.DslFactory

DslFactory dsl = this

new DashboardViewBuilder(this).buildDashboard()

dsl.nestedView('CloudPipelines') {
	views {
		listView('CI') {
			jobs {
				regex('cloudpipelines.*-ci')
			}
			columns defaultColumns()
		}
		listView('All CloudPipelines') {
			jobs {
				regex('cloudpipelines.*')
			}
			columns defaultColumns()
		}
	}
}

private Closure defaultColumns() {
	return {
		status()
		name()
		lastSuccess()
		lastFailure()
		lastBuildConsole()
		buildButton()
	}
}