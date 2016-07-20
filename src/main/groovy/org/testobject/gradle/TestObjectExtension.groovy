package org.testobject.gradle

class TestObjectExtension {

    String baseUrl = "https://app.testobject.com/api/rest"
    String username
    String password
    String team
    String app
    Long testSuite
    List<String> tests
    List<String> classes
    List<String> annotations
    List<String> sizes
    Boolean runAsPackage

    boolean failWhenUnavailable = false
    boolean failOnError = true

}
