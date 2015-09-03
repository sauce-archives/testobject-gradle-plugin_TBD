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
        logger.info(String.format("using baseUrl '%s'", baseUrl));

        TestObjectClient client = TestObjectClient.Factory.create(baseUrl, getProxySettings());

        String username = extension.getUsername();
        String password = extension.getPassword();
        String app = extension.getApp();
        Long testSuite = extension.getTestSuite();
		String team = extension.getTeam() != null && extension.getTeam().isEmpty() == false ? extension.getTeam() : username;

		login(client, username, password);
		
		updateInstrumentationSuite(testApk, appAk, client, team, app, testSuite);

		long start = System.currentTimeMillis();

		long suiteReportId = client.startInstrumentationTestSuite(username, app, testSuite);

		TestSuiteReport suiteReport = client.waitForSuiteReport(username, app, suiteReportId);

		long end = System.currentTimeMillis();


		String executionTime = getExecutionTime(start , end);

		int errors = countErrors(suiteReport);
		String downloadURL = String.format("%s/users/%s/projects/%s/automationReports/%d/download/zip", baseUrl, username, app, suiteReportId);
		String reportURL = String.format("%s/#/%s/%s/espresso/%d/reports/%d" , baseUrl.replace("/api/rest" , ""), username , app, testSuite , suiteReportId);

		StringBuilder msg = new StringBuilder();
		msg.append("\n");
		msg.append(getTestsList(suiteReport , reportURL));
		msg.append("----------------------------------------------------------------------------------");
		msg.append("\n");
		msg.append(String.format("Ran %d tests in %s" , suiteReport
				.getReports().size() , executionTime ));
		msg.append("\n");
		msg.append(suiteReport.getStatus());
		msg.append("\n");

		if(errors > 0){
			msg.append(String.format("List of failed Test (Total errors : %d)", errors));
			msg.append("\n");
			msg.append(failedTestsList(suiteReport , reportURL));
			msg.append("\n");
		}

		msg.append(String.format("DownloadZIP URL: '%s'" , downloadURL));
		msg.append("\n");
		msg.append(String.format("Report URL : '%s'" , reportURL));
		if (errors == 0) {
			logger.info(msg.toString());
		} else {
			if (extension.getFailOnError()) {
				throw new GradleScriptException("failure during test suite execution of test suite " + testSuite, new Exception(msg.toString()));
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
            logger.info(String.format("Uploaded appAPK : %s and testAPK : %s" , appAk.getAbsolutePath() , testApk.getAbsolutePath()));
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

    private static String getTestsList(TestSuiteReport suiteReport , String  baseReportUrl) {
        StringBuilder list = new StringBuilder();
        Iterator<TestSuiteReport.ReportEntry> reportsIterator = suiteReport.getReports().iterator();
        while (reportsIterator.hasNext()) {
            TestSuiteReport.ReportEntry reportEntry = (TestSuiteReport.ReportEntry) reportsIterator.next();
            String testName = getTestName(suiteReport , reportEntry.getKey().getTestId());
            String deviceId = reportEntry.getKey().getDeviceId();
            list.append(String.format("%s - %s .............  %s" , testName , deviceId , reportEntry.getView().getStatus().toString()));
            list.append("\n");
        }
        return list.toString();
    }

    private static String failedTestsList(TestSuiteReport suiteReport , String  baseReportUrl) {
        StringBuilder list = new StringBuilder();
        Iterator<TestSuiteReport.ReportEntry> reportsIterator = suiteReport.getReports().iterator();
        while (reportsIterator.hasNext()) {
            TestSuiteReport.ReportEntry reportEntry = (TestSuiteReport.ReportEntry) reportsIterator.next();
            if (reportEntry.getView().getStatus() == TestSuiteReport.Status.FAILURE) {
                String testName = getTestName(suiteReport , reportEntry.getKey().getTestId());
                String deviceId = reportEntry.getKey().getDeviceId();
                String url = String.format("%s/executions/%d" ,
                        baseReportUrl , reportEntry.getView().getReportId()); 
                list.append(String.format("%s - %s ....  %s" , testName , deviceId , url));
                list.append("\n");
            }
        }
        return list.toString();
    }

    private static String getTestName(TestSuiteReport suiteReport , long testId) {
        Iterator<TestSuiteReport.TestView> testViewIterator = suiteReport.getTests().iterator();
        while (testViewIterator.hasNext()) {
            TestSuiteReport.TestView testView = (TestSuiteReport.TestView) testViewIterator.next();
            if (testView.getTestId() == testId) {
                return testView.getName();
            }
        }
        return "";
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

    /**
     * get Formatted Execution Time for printing
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
