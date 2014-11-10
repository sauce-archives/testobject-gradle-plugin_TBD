testobject-gradle-plugin
===================

TestObject Gradle Plugin

Use this plugin to upload your apk and test apk to app.testobject.com and execute the provided test cases.

See the sample build.gradle config above:

```
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
	username "your-username" // username, see nr. 1 in screenshot above, not your email
	password "your-password" // your password you use for login into testobject
	app "your-app-name" // name of your app, see nr. 2 in screenshot above
	testSuite 17 // id of your test suite, see nr. 3 in screenshot above
}
```

![](https://github.com/testobject/testobject-gradle-plugin/blob/gh-pages/images/ScreenGradlePlugin.png)
