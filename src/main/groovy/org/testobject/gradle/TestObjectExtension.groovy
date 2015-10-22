package org.testobject.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import java.util.HashMap


class TestObjectExtension {



    String baseUrl = "https://app.testobject.com/api/rest"
	String username
	String password
	String team
	String app
	Long testSuite
    String suiteName
    HashMap<String,String> args
    String[] devices

    boolean failOnError = true
}


