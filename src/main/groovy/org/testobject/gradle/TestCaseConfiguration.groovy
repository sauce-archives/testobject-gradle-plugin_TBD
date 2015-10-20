package org.testobject.gradle

/**
 * Created by umutuzgur on 16/10/15.
 */
class TestCaseConfiguration {

        String runTestCases;
    def boolean isEmpty(){
        return runTestCases == null
    }

}
