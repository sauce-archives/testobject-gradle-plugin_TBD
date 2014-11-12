package org.testobject.gradle;

import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;

import org.gradle.api.GradleScriptException;
import org.testobject.api.TestObjectClient;
import org.testobject.rest.api.TestSuiteReport;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.testing.api.TestServer;
import com.android.utils.ILogger;

public class TestObjectTestServer extends TestServer {

	private final ILogger logger;
	private final TestObjectExtension extension;

	TestObjectTestServer(@NonNull TestObjectExtension extension, @NonNull ILogger logger) {
		this.extension = extension;
		this.logger = logger;
	}

	@Override
	public void uploadApks(@NonNull String variantName, @NonNull File testApk, @Nullable File appAk) {
		String baseUrl = extension.getBaseUrl();
		info("using baseUrl '%s'", baseUrl);

		TestObjectClient client = TestObjectClient.Factory.create(baseUrl, getProxySettings());

		String username = extension.getUsername();
		String password = extension.getPassword();
		String app = extension.getApp();
		Long testSuite = extension.getTestSuite();

		login(client, username, password);

		updateInstrumentationSuite(testApk, appAk, client, username, app, testSuite);
		long suiteReportId = client.startInstrumentationTestSuite(username, app, testSuite);

		TestSuiteReport suiteReport = client.waitForSuiteReport(username, app, suiteReportId);
		int errors = countErrors(suiteReport);

		String msg = String.format("test suite report %d finished with status: %s tests: %d errors: %d", suiteReportId, suiteReport.getStatus(), suiteReport
				.getReports().size(), errors);
		if (errors == 0) {
			info(msg);
		} else {
			error(msg);
			if (extension.getFailOnError()) {
				throw new GradleScriptException("failure during test suite execution of test suite " + testSuite, new Exception(msg));
			}
		}
	}

	private void login(TestObjectClient client, String username, String password) {
		try {
			client.login(username, password);
			info("user %s successfully logged in", username);
		} catch (Exception e) {
			throw new GradleScriptException(String.format("unable to login user %s", username), e);
		}
	}

	private void updateInstrumentationSuite(File testApk, File appAk, TestObjectClient client, String username, String app, Long testSuite) {
		try {
			client.updateInstrumentationTestSuite(username, app, testSuite, new FileInputStream(appAk), new FileInputStream(testApk));
		} catch (Exception e) {
			throw new GradleScriptException(String.format("unable to update testSuite %s", testSuite), e);
		}
	}
	
	private static int countErrors(TestSuiteReport suiteReport) {
		int errors = 0;
		Iterator<TestSuiteReport.ReportEntry> reportsIterator = suiteReport.getReports().iterator();
		while (reportsIterator.hasNext()) {
			TestSuiteReport.ReportEntry reportEntry = (TestSuiteReport.ReportEntry) reportsIterator.next();
			if (reportEntry.getView().getStatus() == TestSuiteReport.Status.FAILURE) {
				errors++;
			}
		}
		return errors;
	}

	@Override
	public boolean isConfigured() {
		if (extension.getUsername() == null) {
			logger.warning("username has not been set");
			return false;
		}
		if (extension.getPassword() == null) {
			logger.warning("password has not been set");
			return false;
		}
		if (extension.getApp() == null) {
			logger.warning("app name has not been set");
			return false;
		}
		if (extension.getTestSuite() == null) {
			logger.warning("testSuite has not been set");
			return false;
		}

		return true;
	}

	@Override
	public String getName() {
		return "testobject";
	}

	private void info(String format, Object... args) {
		logger.info(format, args);
	}

	private void error(String format, Object... args) {
		logger.error(null, format, args);
	}

	private static TestObjectClient.ProxySettings getProxySettings() {
		String proxyHost = System.getProperty("http.proxyHost");
		String proxyPort = System.getProperty("http.proxyPort");
		String proxyUser = System.getProperty("http.proxyUser");
		String proxyPassword = System.getProperty("http.proxyPassword");

		return proxyHost != null ? new TestObjectClient.ProxySettings(proxyHost, Integer.parseInt(proxyPort), proxyUser, proxyPassword)
				: null;
	}

}