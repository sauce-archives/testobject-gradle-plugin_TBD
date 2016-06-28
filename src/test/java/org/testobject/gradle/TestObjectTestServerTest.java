package org.testobject.gradle;

import org.gradle.api.logging.Logger;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import static org.mockito.Mockito.mock;

public class TestObjectTestServerTest {

	@Test
	public void test(){
		TestObjectExtension extension = new TestObjectExtension();
		extension.setUsername("testobject");
		extension.setPassword("UFsqW71e4a5sV3k9q");
		extension.setApp("basic-espresso-sample");
		extension.setTestSuite(11l);

		URL appApk = TestObjectTestServerTest.class.getResource("app.apk");
		URL testApk = TestObjectTestServerTest.class.getResource("test.apk");

		TestObjectTestServer server = new TestObjectTestServer(extension, mock(Logger.class), "");
		server.uploadApks(null, new File(testApk.getFile()), new File(appApk.getFile()));

	}
}
