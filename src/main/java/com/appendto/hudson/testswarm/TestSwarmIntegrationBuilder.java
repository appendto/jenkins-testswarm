package com.appendto.hudson.testswarm;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.VariableResolver;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * This is plugin is responsible for integrating TestSwarm into hudson for javascript
 * integration testing. It will take all test case urls and post it to TestSwarm
 * server
 * @author kkesavan
 *
 */
public class TestSwarmIntegrationBuilder extends Builder {

	protected final String CHAR_ENCODING = "iso-8859-1";

	//client id
	private final String CLIENT_ID = "fromHudson";

	//state
	private final String STATE ="addjob";

	//browsers type
	private String chooseBrowsers;

	//job name
	private String jobName;

	//user name
	private String userName;

	//password
	private String authToken;

	//max run
	private String maxRuns;

	//test swarm server url
	private String testswarmServerUrl;

	/*
	 * How frequent this plugin will hit the testswarm job url
	 * to know about test suite results
	 */
	private String pollingIntervalInSecs;

	/*
	 * How long this plugin will wait to know about
	 * test suite results
	 */
	private String timeOutPeriodInMins;

	private TestSuiteData[] testSuiteList = new TestSuiteData[0];

	private TestSuiteData[] testSuiteListCopy;

	private String testswarmServerUrlCopy;

	//private TestTypeConfig testTypeConfig;

	private TestSwarmDecisionMaker resultsAnalyzer;

	// Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
	@DataBoundConstructor
	public TestSwarmIntegrationBuilder(String testswarmServerUrl, String jobName, String userName,
			String authToken, String maxRuns, String chooseBrowsers,
			String pollingIntervalInSecs, String timeOutPeriodInMins,
			List<TestSuiteData> testSuiteList
			) {

		this.testswarmServerUrl = testswarmServerUrl;
		this.jobName = jobName;
		this.userName = userName;
		this.authToken = authToken;
		this.maxRuns = maxRuns;
		this.chooseBrowsers = chooseBrowsers;
		this.pollingIntervalInSecs = pollingIntervalInSecs;
		this.timeOutPeriodInMins = timeOutPeriodInMins;
		this.testSuiteList = testSuiteList.toArray(new TestSuiteData[testSuiteList.size()]);
		//this.testTypeConfig = testTypeConfig;
		this.resultsAnalyzer = new TestSwarmDecisionMaker();
	}

	@Exported
	public TestSuiteData[] getTestSuiteList() {
		return testSuiteList;
	}

	/**
	 * We'll use this from the <tt>config.jelly</tt>.
	 */
	public String getTestswarmServerUrl() {
		return testswarmServerUrl;
	}

	public String getChooseBrowsers() {
		return chooseBrowsers;
	}

	public String getJobName() {
		return jobName;
	}

	public String getUserName() {
		return userName;
	}

	public String getAuthToken() {
		return authToken;
	}

	public String getMaxRuns() {
		return maxRuns;
	}

	public String getPollingIntervalInSecs() {
		return pollingIntervalInSecs;
	}

	public String getTimeOutPeriodInMins() {
		return timeOutPeriodInMins;
	}

	/**
	 * Check if config file loc is a url
	 * @return true if the configFileLoc is a valid url else return false
	 */
	public boolean isValidUrl(String urlStr) {

		try {
			URL url = new URL(urlStr);
			return url != null;
		} catch(Exception ex) {
			return false;
		}
	}

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException{

		listener.getLogger().println("");
		listener.getLogger().println("Launching TestSwarm Integration Suite...");

		testswarmServerUrlCopy = new String(testswarmServerUrl);

		testSuiteListCopy = new TestSuiteData[testSuiteList.length];
		TestSuiteData copyData;
		TestSuiteData origData;
		for(int i=0; i < testSuiteList.length; i++) {
			origData = (TestSuiteData)testSuiteList[i];
			copyData = new TestSuiteData(origData.testName, origData.testUrl, origData.testCacheCracker, origData.disableTest);
			//listener.getLogger().println(copyData.testName+"  --->  "+copyData.testUrl);

				if (origData.disableTest)
			  listener.getLogger().println("Test is disabled for : " + origData.testName);

			testSuiteListCopy[i] = copyData;

		}

		//resolve environmental variables
		expandRuntimeVariables(build);

		//check all required parameters are entered
		if(this.getTestswarmServerUrl() == null || this.getTestswarmServerUrl().length() == 0) {
			listener.error("TestSwarm Server Url is mandatory");
			build.setResult(Result.FAILURE);
			return false;
		}

		if(this.getJobName() == null || this.getJobName().length() == 0) {
			listener.error("Jobname is mandatory");
			build.setResult(Result.FAILURE);
			return false;
		}

		if(this.getUserName() == null || this.getUserName().length() == 0) {
			listener.error("Username is mandatory");
			build.setResult(Result.FAILURE);
			return false;
		}

		if(this.getAuthToken() == null || this.getAuthToken().length() == 0) {
			listener.error("Auth Token is mandatory");
			build.setResult(Result.FAILURE);
			return false;
		}

		if(this.getMaxRuns() == null || this.getMaxRuns().length() == 0) {
			listener.error("Maximum number of runs is mandatory");
			build.setResult(Result.FAILURE);
			return false;
		} else {
			//Check for integer value
			try {
				Integer.parseInt(getMaxRuns());
			} catch (Exception parseEx) {
				listener.error("Maximum number of runs is not an integer");
				build.setResult(Result.FAILURE);
				return false;
			}
		}

		if(this.getPollingIntervalInSecs() == null || this.getPollingIntervalInSecs().length() == 0) {
			listener.error("Polling interval is mandatory");
			build.setResult(Result.FAILURE);
			return false;
		} else {
			//Check for integer value
			try {
				Integer.parseInt(getPollingIntervalInSecs());
			} catch (Exception parseEx) {
				listener.error("Polling interval is not an integer");
				build.setResult(Result.FAILURE);
				return false;
			}
		}

		if(this.getTimeOutPeriodInMins() == null || this.getTimeOutPeriodInMins().length() == 0) {
			listener.error("Timeout Period is mandatory");
			build.setResult(Result.FAILURE);
			return false;
		} else {
			//Check for integer value
			try {
				Integer.parseInt(getTimeOutPeriodInMins());
			} catch (Exception parseEx) {
				listener.error("Timeout period is not an integer");
				build.setResult(Result.FAILURE);
				return false;
			}
		}

		if(!isValidUrl(getTestswarmServerUrl())) {
			listener.error("Testswarm Server Url is not a valid url ! check your TestSwarm Integration Plugin configuration");
			build.setResult(Result.FAILURE);
			return false;
		}

		URL url;
		HttpURLConnection urlConnection;
		String redirectURL = null;
		StringBuffer requestStr = new StringBuffer();

		try {

			//download the given json config
			populateStaticDataInRequestString(requestStr);
			populateTestSuites(requestStr);
			//requestStr = frameRequestStr();
			listener.getLogger().println("Printing encoded request ....");
			listener.getLogger().println(requestStr.toString());

			// Create connection
			url = new URL(this.testswarmServerUrlCopy + "?" + requestStr.toString());
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("HEAD");
			urlConnection.setInstanceFollowRedirects(false);
			//physically connect
			urlConnection.connect();

			Map<String, List<String>> headerFields = urlConnection.getHeaderFields();
			//fetch redirected url
			redirectURL =  urlConnection.getHeaderField("Location");
			//if location is null - somewhere some thing wrong
			if (redirectURL == null || redirectURL.trim().length() ==0) {
				listener.getLogger().println("Failed to post your request to Testswarm Server");
				listener.getLogger().println("It could be because of various reasons, one such is incorrect username or authtoken. " +
												"So please verify it");
				build.setResult(Result.FAILURE);
				return false;
			}

			String jobUrl = this.testswarmServerUrlCopy + redirectURL;
			listener.getLogger().println("**************************************************************");
			listener.getLogger().println("Your request is successfully posted to TestSwarm Server and " +
					" you can view the result in the following URL");
			listener.getLogger().println(jobUrl);
			listener.getLogger().println("**************************************************************");
			listener.getLogger().println("");
			listener.getLogger().println("Analyzing Test Suite Result....");
			if (!analyzeTestSuiteResults(jobUrl, build, listener)) {
				listener.getLogger().println("Analyzing Test Suite Result COMPLETED...");
				listener.getLogger().println("");

				//listener.getLogger().println(TestSwarmUtil.getInstance().getGridText(new TestSwarmDecisionMaker().grabPage(jobUrl)));
				listener.getLogger().println(TestSwarmUtil.getInstance().processResult(new TestSwarmDecisionMaker().grabPage(jobUrl)));
				build.setResult(Result.FAILURE);
				return false;
			}
			listener.getLogger().println("Analyzing Test Suite Result COMPLETED...");
			listener.getLogger().println("");
			//listener.getLogger().println(TestSwarmUtil.getInstance().getGridText(new TestSwarmDecisionMaker().grabPage(jobUrl)));
			listener.getLogger().println("Result: ");
			listener.getLogger().println(TestSwarmUtil.getInstance().processResult(new TestSwarmDecisionMaker().grabPage(jobUrl)));

		}
		catch(Exception ex) {
			System.out.println("Exception cought:\n"+ ex.toString());
			listener.error(ex.toString());
			build.setResult(Result.FAILURE);
			return false;
			//listener.getLogger().println("Exception cought:\n"+ ex.toString());
		}
//        finally {
//        	env.remove("testPath");
//        }
		return true;
	}

	private void expandRuntimeVariables(AbstractBuild build) {

		VariableResolver<String> varResolver = build.getBuildVariableResolver();
		this.testswarmServerUrlCopy = Util.replaceMacro(this.getTestswarmServerUrl(), varResolver);

		for (int i = testSuiteListCopy.length - 1; i >= 0; i--) {
			//Ignore testcase if disbled
			if(!testSuiteListCopy[i].isDisableTest()){
				//listener.getLogger().println("Test Url : "+Util.replaceMacro(testSuiteListCopy[i].getTestUrl(), varResolver));
				testSuiteListCopy[i].setTestUrl(Util.replaceMacro(testSuiteListCopy[i].getTestUrl(), varResolver));
			}
		}



	}

	private void populateStaticDataInRequestString(StringBuffer requestStr) throws Exception {

		//Populate static data like user credentials and other properties
		requestStr.append("client_id=").append(CLIENT_ID)
		.append("&state=").append(STATE)
		.append("&job_name=").append(URLEncoder.encode(getJobName(), CHAR_ENCODING))
		.append("&user=").append(getUserName())
		.append("&auth=").append(getAuthToken())
		.append("&max=").append(getMaxRuns())
		.append("&browsers=").append(getChooseBrowsers());
	}

	private void populateTestSuites(StringBuffer requestStr) throws Exception {

		/*
		 * Changes done to send the reversed test suite list to TestSwarm.
		 */
		//for (TestSuiteData t : getTestSuiteList()) {
		for (int i = testSuiteListCopy.length - 1; i >= 0; i--) {
			//Ignore testcase if disbled
			if(!testSuiteListCopy[i].isDisableTest()){
				encodeAndAppendTestSuiteUrl(requestStr, testSuiteListCopy[i].getTestName(), testSuiteListCopy[i].getTestUrl(), testSuiteListCopy[i].isTestCacheCracker());
			}
		}
	}

	private void encodeAndAppendTestSuiteUrl(StringBuffer requestStr,String testName, String testSuiteUrl, boolean cacheCrackerEnabled) throws Exception {

		requestStr.append("&").append(URLEncoder.encode("suites[]", CHAR_ENCODING)).append("=")
								.append(URLEncoder.encode(testName, CHAR_ENCODING))
								.append("&").append(URLEncoder.encode("urls[]", CHAR_ENCODING)).append("=");
		requestStr.append(URLEncoder.encode(testSuiteUrl, CHAR_ENCODING));
		if(cacheCrackerEnabled)
		{
			requestStr.append("&").append(URLEncoder.encode("cache_killer="+System.currentTimeMillis(), CHAR_ENCODING));
		}
	}

	private boolean analyzeTestSuiteResults(String jobUrl, AbstractBuild build, BuildListener listener)
					throws Exception
	{

		long secondsBetweenResultPolls = Long.parseLong(getPollingIntervalInSecs());
		long minutesTimeOut = Long.parseLong(getTimeOutPeriodInMins());

		long start = System.currentTimeMillis();
		//give testswarm 15 seconds to finish earlier activities
		Thread.sleep(15 * 1000);
		String html;
		Map<String, Integer> results = new HashMap<String, Integer>();
		boolean isBuildSuccessful = false;
		Integer statusCount;

		while (start + (minutesTimeOut * 60000) > System.currentTimeMillis()) {

			html = this.resultsAnalyzer.grabPage(jobUrl);
			results = this.resultsAnalyzer.parseResults(html);
			for(String status : results.keySet()) {
				statusCount = (Integer)results.get(status);
				if (statusCount != null)
					listener.getLogger().println(status+"   ->  "+statusCount.intValue());
				else
					listener.getLogger().println(status+"   ->  0");
			}

			//System.out.println(results);
			if (this.resultsAnalyzer.finished(results, listener)) {
				return this.resultsAnalyzer.isBuildSuccessful();
			}

			listener.getLogger().println("Sleeping for " + secondsBetweenResultPolls
					+ " seconds...");
			Thread.sleep(secondsBetweenResultPolls * 1000);
		}
		listener.getLogger().println("Timed Out....");
		return false;

	}
	// overrided for better type safety.
	// if your plugin doesn't really define any property on Descriptor,
	// you don't have to do this.
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl)super.getDescriptor();
	}

	/**
	 * Descriptor for {@link HelloWorldBuilder}. Used as a singleton.
	 * The class is marked as public so that it can be accessed from views.
	 *
	 * <p>
	 * See <tt>views/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
	 * for the actual HTML fragment for the configuration screen.
	 */
	@Extension // this marker indicates Hudson that this is an implementation of an extension point.
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		/**
		 * To persist global configuration information,
		 * simply store it in a field and call save().
		 *
		 * <p>
		 * If you don't want fields to be persisted, use <tt>transient</tt>.
		 */
		private boolean useFrench;

		/**
		 * Performs on-the-fly validation of the form field 'name'.
		 *
		 * @param value
		 *      This parameter receives the value that the user has typed.
		 * @return
		 *      Indicates the outcome of the validation. This is sent to the browser.
		 */
		public FormValidation doCheckName(@QueryParameter String value) throws IOException, ServletException {
			if(value.length()==0)
				return FormValidation.error("Please set a name");
			if(value.length()<4)
				return FormValidation.warning("Isn't the name too short?");
			return FormValidation.ok();
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			// indicates that this builder can be used with all kinds of project types
			return true;
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			return "TestSwarm Integration";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			// To persist global configuration information,
			// set that to properties and call save().
			useFrench = formData.getBoolean("useFrench");

			// ^Can also use req.bindJSON(this, formData);
			//  (easier when there are many fields; need set* methods for this, like setUseFrench)
			save();
			return super.configure(req,formData);
		}

		@Override
		public Builder newInstance(StaplerRequest staplerRequest, JSONObject jsonObject) throws FormException {
			return super.newInstance(staplerRequest, jsonObject);
		}

		public DescriptorImpl() {
			super(TestSwarmIntegrationBuilder.class);
			load();
		}
		/**
		 * This method returns true if the global configuration says we should speak French.
		 */
		public boolean useFrench() {
			return useFrench;
		}

	}

	@ExportedBean
	public static final class TestSuiteData implements Serializable {

		@Exported
		public String testName;

		@Exported
		public String testUrl;

		@Exported
		public boolean testCacheCracker;

		@Exported
		public boolean disableTest;

		@DataBoundConstructor
		public TestSuiteData(String testName, String testUrl, boolean testCacheCracker, boolean disableTest) {
			this.testName = testName;
			this.testUrl = testUrl;
			this.testCacheCracker = testCacheCracker;
			this.disableTest = disableTest;
		}

		public void setTestName(String testName) {
			this.testName = testName;
		}

		public void setTestUrl(String testUrl) {
			this.testUrl = testUrl;
		}

		public String getTestName() {
			return testName;
		}

		public String getTestUrl() {
			return testUrl;
		}

		public String toString() {
			return "==> " + testName + ", " + testUrl + ", " + testCacheCracker + "sss<==" ;
		}

		public boolean isTestCacheCracker() {
			return testCacheCracker;
		}

		public boolean isDisableTest() {
			return disableTest;
		}

	}
//    public static final class TestSuiteData {
//
//    	public String testName;
//
//    	public String testUrl;
//
//    	@DataBoundConstructor
//    	public TestSuiteData(String testName, String testUrl) {
//
//    		this.testName = testName;
//    		this.testUrl = testUrl;
//    	}
//    }
}

