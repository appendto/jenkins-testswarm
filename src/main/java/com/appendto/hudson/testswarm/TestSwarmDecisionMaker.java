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
import java.util.Iterator;

import net.sf.json.JSONObject;
import net.sf.json.JSONArray;
import net.sf.json.JSONSerializer;

public class TestSwarmDecisionMaker {

	private boolean buildSuccessful = false;

    public Map<String, Integer> parseResults(String response, BuildListener listener) {

	JSONObject json = (JSONObject) JSONSerializer.toJSON( response );
	JSONObject job = (JSONObject) json.getJSONObject("job");
	JSONArray runs = (JSONArray) job.getJSONArray("runs");

    	Map<String, Integer> results = new HashMap<String, Integer>();
    	String result = null;
    	Integer count = null;

	for(int i = 0; i < runs.size(); ++i){
		JSONObject run = (JSONObject) runs.getJSONObject(i);
		JSONObject uaRuns = (JSONObject) run.getJSONObject("uaRuns");

		Iterator uaIter = uaRuns.keys();

		while(uaIter.hasNext()){
			String ua = (String)uaIter.next();
			JSONObject uaResult = (JSONObject) uaRuns.getJSONObject(ua);
			result = uaResult.getString("runStatus");
			count = results.get(result);

			if (count == null) {
				count = new Integer(0);
			}
			count++;
			results.put(result, count);
		}
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
				result.append(s);
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

		Integer notstarted = results.get("new");
		Integer pass = results.get("passed");
		Integer progress = results.get("progress");
		Integer error = results.get("error");
		Integer fail = results.get("failed");
		Integer timeout = results.get("timedout");

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
