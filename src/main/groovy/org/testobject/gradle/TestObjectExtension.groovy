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

    int testTimeout = 60
    int checkFrequency = 30

    boolean failOnUnknown = false
    boolean failOnError = true
    boolean runAsPackage = false

}
