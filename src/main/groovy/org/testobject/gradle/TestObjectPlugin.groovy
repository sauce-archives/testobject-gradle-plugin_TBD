package org.testobject.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging

class TestObjectPlugin implements Plugin<Project> {

	public static final String PLUGIN_NAME = 'testobject'

	private TestObjectExtension extension

	@Override
	void apply(Project project) {
		extension = project.extensions.create(PLUGIN_NAME, TestObjectExtension)
		String buildDir = project.buildDir.absolutePath
		project.logger
		project.android.testServer(new TestObjectTestServer(extension, Logging.getLogger("testobject"), buildDir))
	}

}
