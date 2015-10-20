package org.testobject.gradle

/**
 * Created by umutuzgur on 16/10/15.
 */
class TestClassConfiguration {

    String runTestClasses;
    String runWithOutTestClasses;

    def boolean isEmpty(){
        return runTestClasses == null && runWithOutTestClasses == null
    }
}
