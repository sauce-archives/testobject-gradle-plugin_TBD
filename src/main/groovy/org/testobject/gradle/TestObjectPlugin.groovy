package org.testobject.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging;

class TestObjectPlugin implements Plugin<Project> {

	public static final String PLUGIN_NAME = 'testobject'

	private TestObjectExtension extension

	@Override
	void apply(Project project) {
		extension = project.extensions.create(PLUGIN_NAME, TestObjectExtension)
		project.logger
		if (project.plugins.hasPlugin('android') || project.plugins.hasPlugin('android-library')) {
			project.android.testServer(new TestObjectTestServer(extension, Logging.getLogger("testobject")))
		} else {
			project.task('testobjectUpload') << { 
				new TestObjectTestServer(extension, new DelegatingLogger(project.logger)).uploadApks(null, null, null)
			}
		}
	}
	
	class DelegatingLogger {
		
		private Logger logger;
		
		DelegatingLogger(Logger logger){
			this.logger = logger;
		}
		
		transient void info(String arg0, Object[] arg1) {
			logger.log(LogLevel.INFO, String.format(arg0, arg1));
		};
		transient void error(Throwable arg0, String arg1, Object[] arg2) {
			if(arg0 == null){
				logger.log(LogLevel.ERROR, String.format(arg1, arg2));
			} else {
				logger.log(LogLevel.ERROR, String.format(arg1, arg2), arg0);
			}
		};
		transient void verbose(String arg0, Object[] arg1) {
			logger.log(LogLevel.DEBUG, String.format(arg0, arg1));
		};
		transient void warning(String arg0, Object[] arg1) {
			logger.log(LogLevel.WARN, String.format(arg0, arg1));
		};
	}
}
