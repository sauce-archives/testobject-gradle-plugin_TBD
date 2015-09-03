package org.testobject.gradle;

import java.io.File;
import java.util.Iterator;

import org.gradle.api.GradleScriptException;
import org.gradle.api.logging.Logger;
import org.testobject.api.TestObjectClient;
import org.testobject.rest.api.TestSuiteReport;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.testing.api.TestServer;

public class TestObjectTestServer extends TestServer {

	private final Logger logger;
	private final TestObjectExtension extension;

	public TestObjectTestServer(@NonNull TestObjectExtension extension, @NonNull Logger logger) {
		this.extension = extension;
		this.logger = logger;
	}

	@Override
	public void uploadApks(@NonNull String variantName, @NonNull File testApk, @Nullable File appAk) {
		String baseUrl = extension.getBaseUrl();
		logger.info("using baseUrl '%s'", baseUrl);

		TestObjectClient client = TestObjectClient.Factory.create(baseUrl, getProxySettings());

		String username = extension.getUsername();
		String password = extension.getPassword();
		String app = extension.getApp();
		Long testSuite = extension.getTestSuite();
		String team = extension.getTeam() != null && extension.getTeam().isEmpty() == false ? extension.getTeam() : username;

		login(client, username, password);

		updateInstrumentationSuite(testApk, appAk, client, team, app, testSuite);
		long suiteReportId = client.startInstrumentationTestSuite(team, app, testSuite);

		TestSuiteReport suiteReport = client.waitForSuiteReport(team, app, suiteReportId);
		int errors = countErrors(suiteReport);

		String msg = String.format("test suite report %d finished with status: %s tests: %d errors: %d", suiteReportId, suiteReport.getStatus(), suiteReport
				.getReports().size(), errors);
		if (errors == 0) {
			logger.info(msg);
		} else {
			logger.error(msg);
			if (extension.getFailOnError()) {
				throw new GradleScriptException("failure during test suite execution of test suite " + testSuite, new Exception(msg));
			}
		}
	}

	private void login(TestObjectClient client, String user, String password) {
		try {
			client.login(user, password);
			logger.info("user %s successfully logged in", user);
		} catch (Exception e) {
			throw new GradleScriptException(String.format("unable to login user %s", user), e);
		}
	}

	private void updateInstrumentationSuite(File testApk, File appAk, TestObjectClient client, String team, String app, Long testSuite) {
		try {
			client.updateInstrumentationTestSuite(team, app, testSuite, appAk, testApk);
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
			logger.warn("username has not been set");
			return false;
		}
		if (extension.getPassword() == null) {
			logger.warn("password has not been set");
			return false;
		}
		if (extension.getApp() == null) {
			logger.warn("app name has not been set");
			return false;
		}
		if (extension.getTestSuite() == null) {
			logger.warn("testSuite has not been set");
			return false;
		}

		return true;
	}

	@Override
	public String getName() {
		return "testobject";
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
