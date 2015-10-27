testobject-gradle-plugin
===================

TestObject Gradle Plugin

Use this plugin to upload your apk and test apk to app.testobject.com and execute the provided test cases.

See the sample build.gradle config below:

```
buildscript {
        repositories {
                mavenCentral()
                maven { url 'http://nexus.testobject.org/nexus/content/repositories/testobject-public-repo' }
        }
 
        dependencies {
                classpath 'com.android.tools.build:gradle:0.12+'
                classpath group: 'org.testobject', name: 'testobject-gradle-plugin', version: '0.0.35'
        }
}

apply plugin: 'android'
apply plugin: 'testobject'

android {
	...
}

testobject {
	username "your-username" // the username you use for login into testobject, not your email
	password "your-password" // your password you use for login into testobject
	team "a-team-name" //the name of the team the user belongs to, see nr. 1 in screenshot below,  (optional, if the user is not part of a team)
	app "your-app-name" // name of your app, see nr. 2 in screenshot below
	testSuite 17 // id of your test suite, see nr. 3 in screenshot below
	runAsPackage true // This is a new feature we recently brought. If you are using custom runners and doing internal filterings we reccomend you to use it like this. If not this option can be deleted or set as false
}
```

![](https://github.com/testobject/testobject-gradle-plugin/blob/gh-pages/images/ScreenGradlePlugin.png)
