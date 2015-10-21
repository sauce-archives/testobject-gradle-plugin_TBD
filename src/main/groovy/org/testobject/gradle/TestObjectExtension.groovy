package org.testobject.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project


class TestObjectExtension {



    String baseUrl = "http://127.0.0.1:9000/rest"
	String username
	String password
	String team
	String app
	Long testSuite
    String suiteName
    String[] args
    String[] devices

    boolean failOnError = true
}


