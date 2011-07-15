package com.appendto.hudson.testswarm;

import java.io.FileReader;

import java.io.IOException;
import java.io.Reader;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

public class Html2text extends HTMLEditorKit.ParserCallback {
	 StringBuffer s;

	 public Html2text() {}

	 public void parse(Reader in) throws IOException {
	   s = new StringBuffer();
	   ParserDelegator delegator = new ParserDelegator();
	   // the third parameter is TRUE to ignore charset directive
	   delegator.parse(in, this, Boolean.FALSE);
	 }

	 public void handleText(char[] text, int pos) {
	   s.append(text);
	 }

	 public String getText() {
	   return s.toString();
	 }

//	 public static void main (String[] args) {
//	   try {
//	     // the HTML to convert
//	     FileReader in = new FileReader("java-new.html");
//	     Html2text parser = new Html2text();
//	     parser.parse(in);
//	     in.close();
//	     System.out.println(parser.getText());
//	   }
//	   catch (Exception e) {
//	     e.printStackTrace();
//	   }
//	 }
	}
