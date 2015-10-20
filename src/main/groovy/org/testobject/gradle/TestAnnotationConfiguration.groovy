package org.testobject.gradle

/**
 * Created by umutuzgur on 16/10/15.
 */
class TestAnnotationConfiguration {
    String runWithAnnotations
    String runWithOutAnnotations

    def boolean isEmpty(){
        return runWithAnnotations == null && runWithOutAnnotations == null
    }
}
