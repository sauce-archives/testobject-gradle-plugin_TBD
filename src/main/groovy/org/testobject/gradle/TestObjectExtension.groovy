package org.testobject.gradle

class TestObjectExtension {

	String baseUrl = "https://app.testobject.com/api/rest"
	String username
	String password
	String team
	String app
	Long testSuite
	
	boolean failOnError = true

}
