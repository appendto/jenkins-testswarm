package com.appendto.hudson.testswarm;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestSwarmDecisionMaker {

	private boolean buildSuccessful = false;
	
    public Map<String, Integer> parseResults(String html) {
    	
    	String regex = "td class='.*? ";
    	Pattern p = Pattern.compile(regex);
    	Matcher m = p.matcher(html);
    	Map<String, Integer> results = new HashMap<String, Integer>();
    	String result = null;
    	Integer count = null;
    	
    	while (m.find()) {
    		
	    	result = m.group();
	    	result = result.substring(result.indexOf("'") + 1).trim();
	    	count = results.get(result);

	    	if (count == null) {
	    		count = new Integer(0);
	    	}
	    	count++;
    		results.put(result, count);
    	}

    	return results;
    }

	public String grabPage(String url) throws IOException {
		
		URL u;
		InputStream is = null;
		DataInputStream dis = null;
		String s;
		StringBuffer result = new StringBuffer();
		
		try {
	
			u = new URL(url);
			is = u.openStream();
			dis = new DataInputStream(new BufferedInputStream(is));
		
			while ((s = dis.readLine()) != null) {
				result.append(s).append("\n");
			}
		} 
		finally {
			if (dis != null)
				dis.close();
			if (is != null)
				is.close();
		}
		return result.toString();
	}
	
	public boolean finished(Map<String, Integer> results, BuildListener listener) {
		
		boolean isFinished = false;
		
		if (results.size() == 0) {
			listener.getLogger().println("PROBLEM - NO RESULTS FOUND");
			return true;// fail
		}

		Integer notstarted = results.get("notstarted");
		Integer pass = results.get("pass");
		Integer progress = results.get("progress");
		Integer error = results.get("error");		
		Integer fail = results.get("fail");
		Integer timeout = results.get("timeout");
		
		if (error != null && error.intValue() > 0) {
			listener.getLogger().println(error.intValue()+" test suites ends with ERROR");
			buildSuccessful = false;
			isFinished = true;
		}

		if (fail != null && fail.intValue() > 0) {
			listener.getLogger().println(fail.intValue()+" test suites ends with FAILURE");
			buildSuccessful = false;
			isFinished = true;
		}

		if (timeout != null && timeout.intValue() > 0) {
			listener.getLogger().println(timeout.intValue()+" test suites ends with TIMED OUT");
			buildSuccessful = false;
			isFinished = true;
		}

		if ((notstarted == null || notstarted.intValue() == 0)
				&& (progress == null || progress.intValue() == 0)
				&& (timeout == null || timeout.intValue() == 0)
					&& (fail == null || fail.intValue() == 0)
						&& (error == null || error.intValue() == 0) && pass != null
						&& pass.intValue() > 0) {
			listener.getLogger().println(pass.intValue()+" test suites ends with SUCCESS");
			buildSuccessful = true;
			isFinished = true;
		}
		
		if(isFinished) {
			return true;
		}
		return false;
	}
	
	public boolean isBuildSuccessful() {
		return buildSuccessful;
	}
}
