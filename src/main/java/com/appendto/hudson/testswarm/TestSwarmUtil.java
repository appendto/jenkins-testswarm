package com.appendto.hudson.testswarm;

import hudson.model.BuildListener;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class TestSwarmUtil {

	private static TestSwarmUtil swarmUtil = null;
	public static void main(String[] args) {
		try {

			String url = "http://swarm.example.com/job/001/";
			String html = new TestSwarmDecisionMaker().grabPage(url);
			//System.out.println(new TestSwarmUtil().getGridText(html));
			TestSwarmUtil testSwarmUtil = new TestSwarmUtil().getInstance();
			System.out.println("Result: \n" + testSwarmUtil.processResult(html));
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	public StringBuffer processResult(String html) {
		StringBuffer stringBuffer = null;

		try {
			stringBuffer = new StringBuffer("");

			List<String> moduleList = getModulesList(html);
			String moduleName = null;

			stringBuffer.append("\nFailed Test Case(s) Summary:\n ");

			for (int index = 0 ; index < moduleList.size(); index ++) {
				StringTokenizer tokenizedString = new StringTokenizer(moduleList.get(index), "|");

				while (tokenizedString.hasMoreElements()) {
					moduleName = tokenizedString.nextElement().toString();
					break;
				}

				stringBuffer.append("\n" + moduleName + ":");
						
				while (tokenizedString.hasMoreElements()) {
					String result = getFailedResultString(moduleName, tokenizedString.nextElement().toString());
					if (result != null)
						stringBuffer.append("\n" + result);
					else 						
						stringBuffer.append("\tNone");					
				}			
				
			}
			
			String finalResult = new String(stringBuffer);
			
			stringBuffer.append("\n\nDetails: \n");

			for (int index = 0 ; index < moduleList.size(); index ++) {
				StringTokenizer tokenizedString = new StringTokenizer(moduleList.get(index), "|");

				while (tokenizedString.hasMoreElements()) {
					stringBuffer.append("\nModule: " + tokenizedString.nextElement());
					break;
				}

				while (tokenizedString.hasMoreElements()) {
					stringBuffer.append("\n" + manipulateString(tokenizedString.nextElement().toString()));
				}
				stringBuffer.append("\n");
			}
		}
		catch (Throwable th) {
			th.printStackTrace();
		}
		return stringBuffer;		
	}

	private String getFailedResultString(String moduleName, String string) {
		// TODO Auto-generated method stub
		if (string != null && string.contains("fail")) {
			//return "Module: " + moduleName + " - There are " + string.split("-")[1] + " failed TCs in " + string.split("-")[0].split(" ")[1];
			return "\tThere are " + string.split("-")[1] + " failed TCs in " + string.split("-")[0].split(" ")[1];
		}
		return null;
	}

	private String manipulateString(String details) {
		String manipulatedString = null;

		try {
			String parse_1[] = details.split(" ");
			String passOrFail = parse_1[0];

			String parse_2[] = null;
			
			if (parse_1[1].contains("-")) 				
				parse_2 = parse_1[1].split("-");				
			else 
				parse_2 = parse_1[2].split("-");
						
			String browser = "na";
			int count = -1;

			if (parse_2 != null) {
				browser = parse_2[0];

				if (browser.equalsIgnoreCase("notdone"))
					browser = "na";
				
				try {
					if(parse_2[1] != null) 
						count = Integer.parseInt(parse_2[1]);
				}
				catch(ArrayIndexOutOfBoundsException exception) {
					//NOP
				}
				catch(NumberFormatException exception) {
					//NOP
				}

				if (count == -1) 
					manipulatedString = "Browser - " + browser + "; Status - " + passOrFail;			
				else 
					manipulatedString = "Browser - " + browser + "; No. of TCs with status " + passOrFail + " - " + count;		

			}
		}
		catch (Throwable throwable) {
			throwable.printStackTrace();			
		}

		return manipulatedString;
	}

	private TestSwarmUtil() {

	}

	public static TestSwarmUtil getInstance() {
		if (swarmUtil == null)
			swarmUtil = new TestSwarmUtil();

		return swarmUtil;
	}

	private String getGridHTML(String html){


		String result = "";

		String browseRegrex = "(?s)<table class=\"results\">(.*?)</table>";
		//String browseName = ""
		Pattern browser = Pattern.compile(browseRegrex);
		Matcher brMatch = browser.matcher(html);

		if(brMatch.find()) {
			result = brMatch.group();
		}

		return result;
	}

	private List<String> getBrowserList(String html){

		List<String> list = new ArrayList<String>();
		String result = null;

		String browseRegrex = "(?s)<th><div class=\"browser\">(.*?)</div></th>";
		String titleRegex="(?s)title=\"(.*?)\"";
		//String browseName = ""
		Pattern browser = Pattern.compile(browseRegrex);
		Pattern titlePattern = Pattern.compile(titleRegex);
		Matcher brMatch = browser.matcher(html);

		while (brMatch.find()) {

			result = brMatch.group();

			Matcher titleMatch = titlePattern.matcher(result);
			if(titleMatch.find()){
				String title = titleMatch.group();
				title = title.substring(title.indexOf("\"") + 1, title.lastIndexOf("\""));
				list.add(title);
			}
		}
		return list;
	}

	private List<String> getModulesList(String html){

		String result = null;
		List<String> results = new ArrayList<String>();
		String browseRegrex = "(?s)<tr><th><a href(.*?)</tr>";
		//String browseName = ""
		Pattern browser = Pattern.compile(browseRegrex);
		Matcher brMatch = browser.matcher(html);
		Html2text parser = new Html2text();
		Reader in  = null;

		while (brMatch.find()) {

			result = brMatch.group();
			//result = result.replaceAll("</td>","|</td>");
			result = result.replaceAll("<td class='","<td class=''>|");
			result = result.replaceAll("\\&nbsp;</td>", "</td>");
			//result = result.replaceAll(" ","-");
			in  = new StringReader(result);

			try {
				parser.parse(in);
				in.close();
				String resultRow = parser.getText();
				resultRow = resultRow.replaceAll("'>", "-");
				resultRow = resultRow.replaceAll("notstarted-", "notstarted-0");
				//resultRow = resultRow.replaceAll("<#50>", "0");
				results.add(resultRow);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return results;

	}

	public String getGridText(String html){

		try {

			String gridHtml = getGridHTML(html);

			//Now use gridHtml to get the grid contents-//Construct the header first
			List<String>  browsersList = getBrowserList(gridHtml);
			List<String>  modulesList = getModulesList(gridHtml);
			StringBuilder gridText = new StringBuilder();				
			gridText.append("\n");
			//					gridText.append(StringUtils.rightPad("", 30, ' '));
			browsersList.add(0,StringUtils.rightPad("", 30, ' '));
			//					gridText.append("|");

			for(String browser: browsersList){
				gridText.append(StringUtils.rightPad(browser, 30, ' '));
				gridText.append("|");
			}

			gridText.append("\n\n");
			//Construct the table
			for(String modules: modulesList){

				StringTokenizer tokenizer = new StringTokenizer(modules, "|");
				int count = 0;

				String item;
				while(tokenizer.hasMoreTokens()){
					item = tokenizer.nextToken();
					//gridText.append(StringUtils.rightPad(item, browsersList.get(count).length(), ' '));
					gridText.append(StringUtils.rightPad(item, 30, ' '));
					gridText.append("|");
					count++;
				}	

				gridText.append("\n");
			}

			return gridText.toString();

		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		return "";
	}


}
