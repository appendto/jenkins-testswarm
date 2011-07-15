package com.appendto.hudson.testswarm;

import org.kohsuke.stapler.DataBoundConstructor;

public class TestTypeConfig {

	//target system url
	private String targetSystemUrl;
	
	//query parameters
	private String queryParameters;

	//config file location
	private String configFileLoc;
	
	//test name
	private String testName;
	
	//testFile
	private String testFile;
	//value
//	private String testType;
	
	
    @DataBoundConstructor
    public TestTypeConfig(String configFileLoc, String targetSystemUrl, 
    					String queryParameters, String testName, String testFile) {
    	this.configFileLoc = configFileLoc;
    	this.targetSystemUrl = targetSystemUrl;
    	this.queryParameters = queryParameters;
    	this.testName = testName;
    	this.testFile = testFile;
    	
    }
    
  public String getTargetSystemUrl() {
	return targetSystemUrl;
//	return getDescriptor().getTargetSystemUrl();
  }

	public String getQueryParameters() {
		return queryParameters;
	//	return getDescriptor().getQueryParameters();
	}
	
    public String getConfigFileLoc() {
        return configFileLoc;
    }
    
    public String getTestName() {
    	return testName;
    }
    
    public String getTestFile() {
    	return testFile;
    }
    
	
}
