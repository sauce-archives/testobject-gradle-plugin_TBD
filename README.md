testobject-gradle-plugin
===================

TestObject Gradle Plugin

Use this plugin to upload your apk and test apk to app.testobject.com and execute the provided test cases.

See the sample build.gradle config above:

'''
buildscript {
        repositories {
                mavenCentral()
                maven { url 'http://nexus.testobject.org/nexus/content/repositories/testobject-public-repo' }
        }
 
        dependencies {
                classpath 'com.android.tools.build:gradle:0.12+'
                classpath group: 'org.testobject', name: 'testobject-gradle-plugin', version: '0.0.2'
        }
}

apply plugin: 'android'
apply plugin: 'testobject'

android {
	...
}

testobject {
	username "your-username"
	password "your-password"
	app "your-app-name"
	testSuite 17 // id of your test suite 
}

'''
