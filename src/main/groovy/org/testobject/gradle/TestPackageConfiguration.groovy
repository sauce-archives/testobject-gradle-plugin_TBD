package org.testobject.gradle

/**
 * Created by umutuzgur on 16/10/15.
 */
class TestPackageConfiguration {
    String runTestPackages

    String runWithOutTestPackages

    def boolean isEmpty(){
        return runTestPackages == null && runWithOutTestPackages == null
    }

}
