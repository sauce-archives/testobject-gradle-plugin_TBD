package org.testobject.gradle;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.testing.api.TestServer;
import org.gradle.api.GradleScriptException;
import org.gradle.api.logging.Logger;
import org.testobject.api.TestObjectClient;
import org.testobject.rest.api.TestSuiteReport;
import org.testobject.rest.api.TestSuiteResource;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

        TestSuiteResource.InstrumentationTestSuiteRequest instrumentationTestSuiteRequest = findUsedConfiguration(extension);
        login(client, username, password);

        long responseTestSuite = createInstrumentationSuite(testApk, appAk, client, team, app, testSuite, instrumentationTestSuiteRequest);

        long start = System.currentTimeMillis();

        long suiteReportId = client.startInstrumentationTestSuite(team, app, responseTestSuite);

        TestSuiteReport suiteReport = client.waitForSuiteReport(team, app, suiteReportId);

        long end = System.currentTimeMillis();

        String executionTime = getExecutionTime(start, end);

        int errors = countErrors(suiteReport);
        String downloadURL = String.format("%s/users/%s/projects/%s/automationReports/%d/download/zip", baseUrl, team, app, suiteReportId);
        String reportURL = String.format("%s/#/%s/%s/espresso/%d/reports/%d", baseUrl.replace("/api/rest", ""), team, app, responseTestSuite, suiteReportId);

        StringBuilder msg = new StringBuilder();
        if(responseTestSuite != testSuite){
            msg.append("\n");
            msg.append("Test suite with the id entered wasn't found and a new Test Suite is created with the id: " + responseTestSuite);
        }

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
        logger.info(msg.toString());
        if (errors > 0) {
            logger.warn("Failure during test suite execution of test suite: " + responseTestSuite);
        }
    }

    private void checkDevices(Set<String> devices) {
        if (devices == null)
            throw new IllegalArgumentException("incorrect configuration. Devices can not be empty");


    }

    private void checkTheAnnotationConfiguration(TestObjectExtension extension, Set<String> runSizes) {
        TestAnnotationConfiguration testAnnotationConfiguration = extension.getTestAnnotationConfiguration();
        Set<String> runWithAnnotations = splitByComma(testAnnotationConfiguration.getRunWithAnnotations());
        Set<String> runWithOutAnnotations = splitByComma(testAnnotationConfiguration.getRunWithOutAnnotations());
        if (runWithAnnotations != null && runWithOutAnnotations != null)
            throw new IllegalArgumentException("incorrect configuration. runWithAnnotations and runWithOutAnnotations can not be used together");
        if(runSizes == null) {
            return;
        }
        for (String size : runSizes) {
            if (!(size.equals("small") || size.equals("medium") || size.equals("large"))) {
                throw new IllegalArgumentException("incorrect configuration. runSizes should be a combination of these elements : small, medium, large ");
            }
        }

    }

    private void checkTheConfiguration(TestObjectExtension extension) {
        TestClassConfiguration testClassConfiguration = extension.getTestClassConfiguration();


        TestCaseConfiguration testCaseConfiguration = extension.getTestCaseConfiguration();

        TestPackageConfiguration testPackageConfiguration = extension.getTestPackageConfiguration();

        if ((!testCaseConfiguration.isEmpty() && !testClassConfiguration.isEmpty())
                || (!testCaseConfiguration.isEmpty() && !testPackageConfiguration.isEmpty())
                || (!testPackageConfiguration.isEmpty() && !testClassConfiguration.isEmpty())) {
            throw new IllegalArgumentException("incorrect configuration. Please only use one test configuration");
        }

        Set<String> runTestClasses = splitByComma(testClassConfiguration.getRunTestClasses());
        Set<String> runWitOutTestClasses = splitByComma(testClassConfiguration.getRunWithOutTestClasses());
        if (runTestClasses != null && runWitOutTestClasses != null) {
            throw new IllegalArgumentException("incorrect configuration. Please only use runTestClasses or runWithOutTestClasses");
        }

        Set<String> runTestPackages = splitByComma(testPackageConfiguration.getRunTestPackages());
        Set<String> runWithOutTestPackages = splitByComma(testPackageConfiguration.getRunWithOutTestPackages());

        if (runTestPackages != null && runWithOutTestPackages != null) {
            throw new IllegalArgumentException("incorrect configuration. Please only use runTestPackages or runWithOutTestPackages");
        }

    }

    private TestSuiteResource.InstrumentationTestSuiteRequest findUsedConfiguration(TestObjectExtension extension) {
        String name = extension.getName();
        TestSuiteResource.Type type = null;
        Boolean typeRun = null;
        Set<String> typeToRun = null;
        Boolean annotationsRun = null;
        Set<String> annotations = null;
        Set<String> runSizes = splitByComma(extension.getRunSizes());
        Set<String> devices = splitByComma(extension.getDevices());


        if (!extension.getTestClassConfiguration().isEmpty()) {
            Set<String> runTestClasses = splitByComma(extension.getTestClassConfiguration().getRunTestClasses());
            Set<String> runWitOutTestClasses = splitByComma(extension.getTestClassConfiguration().getRunWithOutTestClasses());
            if (runTestClasses != null) {
                type = TestSuiteResource.Type.TEST_CLASS;
                typeRun = true;
                typeToRun = runTestClasses;
            } else if (runWitOutTestClasses != null) {
                type = TestSuiteResource.Type.TEST_CLASS;
                typeRun = false;
                typeToRun = runWitOutTestClasses;
            }
        } else if (!extension.getTestCaseConfiguration().isEmpty()) {
            Set<String> runTestCases = splitByComma(extension.getTestCaseConfiguration().getRunTestCases());
            if (runTestCases != null) {
                type = TestSuiteResource.Type.TEST_CASE;
                typeRun = true;
                typeToRun = runTestCases;
            }
        } else if (!extension.getTestPackageConfiguration().isEmpty()) {
            Set<String> runTestPackages = splitByComma(extension.getTestPackageConfiguration().getRunTestPackages());
            Set<String> runWithOutTestPackages = splitByComma(extension.getTestPackageConfiguration().getRunWithOutTestPackages());
            if (runTestPackages != null) {
                type = TestSuiteResource.Type.TEST_PACKAGE;
                typeRun = true;
                typeToRun = runTestPackages;
            } else if (runWithOutTestPackages != null) {
                type = TestSuiteResource.Type.TEST_PACKAGE;
                typeRun = false;
                typeToRun = runWithOutTestPackages;
            }
        }
        if (!extension.getTestAnnotationConfiguration().isEmpty()) {
            Set<String> runWithAnnotations = splitByComma(extension.getTestAnnotationConfiguration().getRunWithAnnotations());
            Set<String> runWithOutAnnotations = splitByComma(extension.getTestAnnotationConfiguration().getRunWithOutAnnotations());
            if (runWithAnnotations != null) {
                annotationsRun = true;
                annotations = runWithAnnotations;
            } else if (runWithOutAnnotations != null) {
                annotationsRun = false;
                annotations = runWithOutAnnotations;
            }
        }


        return new TestSuiteResource.InstrumentationTestSuiteRequest(name, type, typeRun, typeToRun, annotationsRun, annotations, runSizes, devices);


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

        Set<String> runSizes = splitByComma(extension.getRunSizes());

        Set<String> devices = splitByComma(extension.getDevices());

        try {
            checkTheConfiguration(extension);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
            return false;
        }

        try {
            checkTheAnnotationConfiguration(extension, runSizes);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
            return false;
        }

        try {
            checkDevices(devices);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
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

    private Set<String> splitByComma(String s) {
        if (s == null)
            return null;
        return new HashSet<String>(Arrays.asList(s.split(",")));
    }

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
