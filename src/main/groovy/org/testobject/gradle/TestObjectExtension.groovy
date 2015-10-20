package org.testobject.gradle


class TestObjectExtension {




    String baseUrl = "http://127.0.0.1:9000/rest"
	String username
	String password
	String team
	String app
	Long testSuite
    String name
    TestClassConfiguration testClassConfiguration = new TestClassConfiguration()
    TestCaseConfiguration testCaseConfiguration = new TestCaseConfiguration()
    TestPackageConfiguration testPackageConfiguration = new TestPackageConfiguration()
    TestAnnotationConfiguration testAnnotationConfiguration = new TestAnnotationConfiguration()
    String runSizes
    String devices

    boolean failOnError = true


}


