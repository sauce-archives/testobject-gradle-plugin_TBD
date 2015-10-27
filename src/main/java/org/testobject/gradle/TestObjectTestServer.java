package org.testobject.gradle;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.testing.api.TestServer;
import org.gradle.api.GradleScriptException;
import org.gradle.api.logging.Logger;
import org.testobject.api.TestObjectClient;
import org.testobject.rest.api.TestSuiteReport;
import org.testobject.rest.api.TestSuiteResource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TestObjectTestServer extends TestServer {

	private final Logger logger;
	private final TestObjectExtension extension;
	private final String gradleDirectory;

	public TestObjectTestServer(@NonNull TestObjectExtension extension, @NonNull Logger logger, @NonNull String gradleDirectory) {
		this.gradleDirectory = gradleDirectory;
		this.extension = extension;
		this.logger = logger;
	}

	@Override
	public void uploadApks(@NonNull String variantName, @NonNull File testApk, @Nullable File appAk) {
		String baseUrl = extension.getBaseUrl();
		logger.info(String.format("using baseUrl '%s'", baseUrl));

		TestObjectClient client = TestObjectClient.Factory.create(baseUrl, getProxySettings());

		String username = extension.getUsername();
		String password = extension.getPassword();
		String app = extension.getApp();
		Long testSuite = extension.getTestSuite();
		String team = extension.getTeam() != null && extension.getTeam().isEmpty() == false ? extension.getTeam() : username;
		Boolean runAsPackage = extension.getRunAsPackage() != null ?  extension.getRunAsPackage() : false;

		TestSuiteResource.InstrumentationTestSuiteRequest instrumentationTestSuiteRequest = new TestSuiteResource.InstrumentationTestSuiteRequest(runAsPackage);

		login(client, username, password);

		updateInstrumentationSuite(testApk, appAk, client, team, app, testSuite, instrumentationTestSuiteRequest);

		long start = System.currentTimeMillis();

		long suiteReportId = client.startInstrumentationTestSuite(team, app, testSuite);

		TestSuiteReport suiteReport = client.waitForSuiteReport(team, app, suiteReportId);

		writeSuiteReportXML(client, username, app, suiteReportId);

		long end = System.currentTimeMillis();

		String executionTime = getExecutionTime(start, end);

		int errors = countErrors(suiteReport);
		String downloadURL = String.format("%s/users/%s/projects/%s/automationReports/%d/download/zip", baseUrl, team, app, suiteReportId);
		String reportURL = String.format("%s/#/%s/%s/espresso/%d/reports/%d", baseUrl.replace("/api/rest", ""), team, app, testSuite, suiteReportId);

		StringBuilder msg = new StringBuilder();

		msg.append("\n");
		msg.append(getTestsList(suiteReport));
		msg.append("----------------------------------------------------------------------------------");
		msg.append("\n");
		msg.append(String.format("Ran %d tests in %s", suiteReport
				.getReports().size(), executionTime));
		msg.append("\n");
		msg.append(suiteReport.getStatus());
		msg.append("\n");

		if (errors > 0) {
			msg.append(String.format("List of failed Test (Total errors : %d)", errors));
			msg.append("\n");
			msg.append(failedTestsList(suiteReport, reportURL));
			msg.append("\n");
		}

		msg.append(String.format("DownloadZIP URL: '%s'", downloadURL));
		msg.append("\n");
		msg.append(String.format("Report URL : '%s'", reportURL));

		if (errors == 0) {
			logger.info(msg.toString());
		}
		else if(errors > 0) {
			System.out.println("Some Errors");
			logger.warn("Failure during test suite execution of test suite: " + testSuite);
		}
	}

	private void writeSuiteReportXML(TestObjectClient client, String user, String app, long suiteReportId) {

		String filename = user + "-" + app + "-" + suiteReportId + ".xml";
		String xml = client.readTestSuiteXMLReport(user, app, suiteReportId);
		File file = new File(Paths.get(gradleDirectory,"testobject").toAbsolutePath().toUri());
		if(!file.isDirectory()){
			file.mkdir();
		}
		try {
			Files.write(Paths.get(gradleDirectory,"testobject",filename), xml.getBytes());
					logger.info("Wrote XML report to '" + filename + "'");
		} catch (IOException e) {
			logger.error("Failed to save XML report: " + e.getMessage());
		}
	}

	private void checkDevices(Set<String> devices) {
		if (devices == null)
			throw new IllegalArgumentException("incorrect configuration. Devices can not be empty");
	}

	private void login(TestObjectClient client, String user, String password) {
		try {

			client.login(user, password);
			logger.info("user %s successfully logged in", user);
		} catch (Exception e) {
			throw new GradleScriptException(String.format("unable to login user %s", user), e);
		}
	}

	private void updateInstrumentationSuite(File testApk, File appAk, TestObjectClient client, String team, String app, Long testSuite, TestSuiteResource.InstrumentationTestSuiteRequest request) {
		try {
			client.updateInstrumentationTestSuite(team, app, testSuite, appAk, testApk,request);
			logger.info(String.format("Uploaded appAPK : %s and testAPK : %s", appAk.getAbsolutePath(), testApk.getAbsolutePath()));
		} catch (Exception e) {
			throw new GradleScriptException(String.format("unable to update testSuite %s", testSuite), e);
		}
	}

	private long createInstrumentationSuite(File testApk, File appAk, TestObjectClient client, String team, String app, Long testSuite, TestSuiteResource.InstrumentationTestSuiteRequest instrumentationTestSuiteRequest) {
		long batchId;
		try {
			batchId = client.createInstrumentationTestSuite(team, app, testSuite, appAk, testApk, instrumentationTestSuiteRequest);
			logger.info(String.format("Uploaded appAPK : %s and testAPK : %s", appAk.getAbsolutePath(), testApk.getAbsolutePath()));
		} catch (Exception e) {
			throw new GradleScriptException(String.format("unable to create testSuite %s", testSuite), e);
		}
		return batchId;
	}

	private static int countErrors(TestSuiteReport suiteReport) {
		int errors = 0;
		Iterator<TestSuiteReport.ReportEntry> reportsIterator = suiteReport.getReports().iterator();
		while (reportsIterator.hasNext()) {
			TestSuiteReport.ReportEntry reportEntry = reportsIterator.next();
			if (reportEntry.getView().getStatus() == TestSuiteReport.Status.FAILURE) {
				errors++;
			}
		}
		return errors;
	}

	private static String getTestsList(TestSuiteReport suiteReport) {
		StringBuilder list = new StringBuilder();
		Iterator<TestSuiteReport.ReportEntry> reportsIterator = suiteReport.getReports().iterator();
		while (reportsIterator.hasNext()) {
			TestSuiteReport.ReportEntry reportEntry = reportsIterator.next();
			String testName = getTestName(suiteReport, reportEntry.getKey().getTestId());
			String deviceId = reportEntry.getKey().getDeviceId();
			list.append(String.format("%s - %s .............  %s", testName, deviceId, reportEntry.getView().getStatus().toString()));
			list.append("\n");
		}
		return list.toString();
	}

	private static String failedTestsList(TestSuiteReport suiteReport, String baseReportUrl) {
		StringBuilder list = new StringBuilder();
		Iterator<TestSuiteReport.ReportEntry> reportsIterator = suiteReport.getReports().iterator();
		while (reportsIterator.hasNext()) {
			TestSuiteReport.ReportEntry reportEntry = reportsIterator.next();
			if (reportEntry.getView().getStatus() == TestSuiteReport.Status.FAILURE) {
				String testName = getTestName(suiteReport, reportEntry.getKey().getTestId());
				String deviceId = reportEntry.getKey().getDeviceId();
				String url = String.format("%s/executions/%d",
						baseReportUrl, reportEntry.getView().getReportId());
				list.append(String.format("%s - %s ....  %s", testName, deviceId, url));
				list.append("\n");
			}
		}
		return list.toString();
	}

	private static String getTestName(TestSuiteReport suiteReport, long testId) {
		Iterator<TestSuiteReport.TestView> testViewIterator = suiteReport.getTests().iterator();
		while (testViewIterator.hasNext()) {
			TestSuiteReport.TestView testView = testViewIterator.next();
			if (testView.getTestId() == testId) {
				return testView.getName();
			}
		}
		return "";
	}

	public boolean checkTheConfiguration(TestObjectExtension extension){
		return true;
	}

	@Override
	public boolean isConfigured() {
		if (extension.getUsername() == null) {
			logger.error("username has not been set");
			return false;
		}
		if (extension.getPassword() == null) {
			logger.error("password has not been set");
			return false;
		}
		if (extension.getApp() == null) {
			logger.error("app name has not been set");
			return false;
		}
		if (extension.getTestSuite() == null) {
			logger.error("testSuite has not been set");
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

	private List<String> splitByColon(String s){
		if (s == null)
			return null;
		return Arrays.asList(s.split(":"));
	}

	//    public Map<String, String> mapConfiguration(TestObjectExtension extension){
	//        String[] args = extension.getArgs();
	//        Map<String, String> configuration = new HashMap<String, String>();
	//        for(String arg : args){
	//            List<String> parsedArgs = splitByColon(arg);
	//            if(parsedArgs.size() == 2){
	//                configuration.put(parsedArgs.get(0),parsedArgs.get(1));
	//            }
	//        }
	//        return configuration;
	//    }

	/**
	 * get Formatted Execution Time for printing
	 *
	 * @param start
	 * @param end
	 * @return
	 */
	private static String getExecutionTime(final long start, final long end) {
		NumberFormat formatter = new DecimalFormat("#.000");
		long millis = end - start;
		long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
		String seconds = formatter.format(millis / 1000d);
		String executionTime;
		if (minutes > 0) {
			executionTime = String.format("%dm (%ss)", minutes, seconds);
		} else {
			executionTime = String.format("%ss", seconds);
		}
		return executionTime;
	}

}
