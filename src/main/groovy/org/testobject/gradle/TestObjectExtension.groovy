package org.testobject.gradle

class TestObjectExtension {

	String baseUrl = "http://app.testobject.com/api/rest"
	String username
	String password
	String app
	Long testSuite
	
	boolean failOnError = true

}
