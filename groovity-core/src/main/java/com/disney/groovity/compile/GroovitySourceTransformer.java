/*******************************************************************************
 * © 2018 Disney | ABC Television Group
 *
 * Licensed under the Apache License, Version 2.0 (the "Apache License")
 * with the following modification; you may not use this file except in
 * compliance with the Apache License and the following modification to it:
 * Section 6. Trademarks. is deleted and replaced with:
 *
 * 6. Trademarks. This License does not grant permission to use the trade
 *     names, trademarks, service marks, or product names of the Licensor
 *     and its affiliates, except as required to comply with Section 4(c) of
 *     the License and to reproduce the content of the NOTICE file.
 *
 * You may obtain a copy of the Apache License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License with the above modification is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the Apache License for the specific
 * language governing permissions and limitations under the Apache License.
 *******************************************************************************/
package com.disney.groovity.compile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translate Groovity source code into Groovy source code for compilation
 * 
 * @author Alex Vigdor
 */
public class GroovitySourceTransformer {
	static Pattern tagPattern = Pattern.compile("</?g:(\\w+)(?:\\s+\\w+\\s*=\\s*\"[^\"]*\")*\\s*/?>");
	static Pattern tagAttsPattern = Pattern.compile("(\\w+)\\s*=\\s*(?:\"\\$\\{([^\"]+?)}\"|\"(\\[[^\"]*\\])\"|(\"[^\"]*?\"))");
	static Pattern expressionStart = Pattern.compile("(?<!\\\\)\\$\\{");
	public static TransformedSource transformSource(String str){
		StringBuilder output = new StringBuilder();
		Map<Integer,Integer> sourceLineNumbers = new HashMap<Integer, Integer>();
		int transformedSourceLineNumber =1;
		int sourceLineNumber = 1;
		Scanner scanner = new Scanner(str);
		try{
		scanner.useDelimiter("((?=<~)|(?<=[^\\\\]~>))");
		while(scanner.hasNext()){
			String segment = scanner.next();
			//System.out.println("<-----------");
			//System.out.println(segment);
			//System.out.println(">-----------");
			if(segment.startsWith("<~")){
				segment = segment.substring(2, segment.length()-2);
				//we'll be making a writable closure
				output.append("new ClosureWritable({ ");
				Matcher tagMatcher = tagPattern.matcher(segment);
				int lastPos = 0;
				ArrayList<String> subsegments = new ArrayList<String>();
				if(tagMatcher.find()){
					tagMatcher.reset();
					while(tagMatcher.find()){
						if(tagMatcher.start()>lastPos){
							subsegments.add(segment.substring(lastPos,tagMatcher.start()));
						}
						subsegments.add(tagMatcher.group(0));
						lastPos = tagMatcher.end();
					}
					if(lastPos < segment.length()){
						subsegments.add(segment.substring(lastPos));
					}
					/*for(String sub:subsegments){
					System.out.println("<=-=-=-=-=-=-=-=-=-");
						System.out.println(sub);
						System.out.println(">=-=-=-=-=-=-=-=-=-");
						
					}*/
				}
				else{
					subsegments.add(segment);
				}
				
				for(String sub: subsegments){
					int prelines = 0;
					int postlines = 0;
					Matcher matcher;
					if(sub.startsWith("<g:")){
						matcher = tagPattern.matcher(sub);
						matcher.find();
						String tagName = matcher.group(1);
						//parse attributes
						output.append("\ntag('").append(tagName).append("'");
						prelines++;
						Matcher attMatcher = tagAttsPattern.matcher(matcher.group(0));
						boolean first = true;
						while(attMatcher.find()){
							if(first){
								output.append(",[");
							}
							else{
								output.append(", ");
							}
							first=false;
							String value = attMatcher.group(2);
							if(value==null){
								value = attMatcher.group(3);
								if(value==null){
									value = attMatcher.group(4);
								}
							}
							else{
								//wrap code in a closure so it can be re-evaluated in a loop
								value = "{ return (".concat(value).concat("); } ");
							}
							//escape $ that are not already escaped or part of an expression
							value = value.replaceAll("(?<!\\\\)(\\$)(?!\\{)", "\\\\$1");
							output.append(attMatcher.group(1)).append(":").append(value);
						}
						
						if(!first){
							output.append("]");
						}
						if(sub.endsWith("/>")){
							output.append(",{});\n");
							postlines++;
						}
						else{
							output.append(",{");
						}
					}
					else if(sub.startsWith("</g:")){
						output.append("});\n");
						postlines++;
					}
					else if((matcher = expressionStart.matcher(sub)).find()){
						int startFrom = 0;
						int exprPos = matcher.start();
						while(exprPos >=0){
							if(exprPos>startFrom){
								String subSegment = escape(sub.substring(startFrom,exprPos));
								output.append("\nstream('''");
								if(subSegment.endsWith("'")){
									output.append(subSegment.substring(0,subSegment.length()-1));
									output.append("\\'");
								}	
								else{
									output.append(subSegment);
								}
								output.append("''');\n");
								postlines++;
								prelines++;
							}
							startFrom=exprPos+2;
							Stack<Character> stack = new Stack<Character>();
							//now the tricky part, find the right ending bracket .... 
							for (startFrom = exprPos+2;startFrom<sub.length();startFrom++){
								char c = sub.charAt(startFrom);
								if(stack.isEmpty() && c=='}'){
									output.append("\nstream(");
									output.append(sub.substring(exprPos+2, startFrom));
									output.append(");\n");
									startFrom++;
									postlines++;
									prelines++;
									break;
								}
								if(c=='"'){
									if(!stack.isEmpty() && '"' == stack.peek()){
										stack.pop();
									}
									else{
										stack.push(c);
									}
								}
								if(c=='\''){
									if(!stack.isEmpty() && '\'' == stack.peek()){
										stack.pop();
									}
									else{
										stack.push(c);
									}
								}
								if(c=='{'){
									stack.push(c);
								}
								if(c=='}'){
									if(!stack.isEmpty() && '{' ==stack.peek()){
										stack.pop();
									}
								}
							}
							if(!stack.isEmpty()){
								throw new RuntimeException("Unbalanced input "+sub);
							}
							while(!(exprPos >= startFrom)){
								if(!matcher.find()){
									exprPos = -1;
									break;
								}
								exprPos = matcher.start();
							}
						}
						if(startFrom < sub.length()){
							String subSegment = escape(sub.substring(startFrom));
							output.append("\nstream('''");
							if(subSegment.endsWith("'")){
								output.append(subSegment.substring(0,subSegment.length()-1));
								output.append("\\'");
							}	
							else{
								output.append(subSegment);
							}
							output.append("''');\n");
							postlines++;
							prelines++;
						}
					}
					else if(sub.trim().length() > 0){
						String subSegment = escape(sub);
						output.append("\nstream('''");
						if(subSegment.endsWith("'")){
							output.append(subSegment.substring(0,subSegment.length()-1));
							output.append("\\'");
						}	
						else{
							output.append(subSegment);
						}
						output.append("''');\n");
						postlines++;
						prelines++;
					}
					else{
						//just whitespace
						output.append(sub);
					}
					
					int subLines=countNewLines(sub);
					//System.out.println(subLines+" newlines in "+sub);
					for(int i=0;i<prelines;i++){
						//System.out.println("Added new prelines "+transformedSourceLineNumber+" "+sourceLineNumber);
						sourceLineNumbers.put(transformedSourceLineNumber++, sourceLineNumber);
					}
					for(int i=0;i<subLines;i++){
						//System.out.println("Filling in lines "+(transformedSourceLineNumber+i)+" "+sourceLineNumber);
						sourceLineNumbers.put(transformedSourceLineNumber++, sourceLineNumber++);
					}
					for(int i=0;i<postlines;i++){
						//System.out.println("Added new postlines "+transformedSourceLineNumber+" "+sourceLineNumber);
						sourceLineNumbers.put(transformedSourceLineNumber++, sourceLineNumber);
					}	
				}
				output.append("})\n");
				sourceLineNumbers.put(transformedSourceLineNumber++, sourceLineNumber);
			}
			else{
				//regular code
				output.append(segment);
				int sourceLines=countNewLines(segment);
				for(int i=0;i<sourceLines;i++){
					//System.out.println("Filling in code lines "+(transformedSourceLineNumber+i)+" "+sourceLineNumber);
					sourceLineNumbers.put(transformedSourceLineNumber++, sourceLineNumber++);
				}
			}
		}
		sourceLineNumbers.put(transformedSourceLineNumber, ++sourceLineNumber);
		/*
		System.out.println("Transformed Source is\n\n");
		int ln = 1;
		for(String line: output.toString().split("\n")){
			System.out.println(line+" //"+sourceLineNumbers.get(ln));
			ln++;
		}*/
		//System.out.println(output.toString());
		}
		finally{
			scanner.close();
		}
		return new TransformedSource(str,output.toString(),sourceLineNumbers);
	}
	private static String escape(String str){
		return str.replaceAll("\\\\\\$\\{",Matcher.quoteReplacement("${")).replaceAll("\\\\~>",Matcher.quoteReplacement("~>")).replaceAll("\\\\",Matcher.quoteReplacement("\\\\"));
	}
	public static int countNewLines(String str) {
	    int lines = 0;
	    int pos = 0;
	    while ((pos = str.indexOf("\n", pos) + 1) != 0) {
	        lines++;
	    }
	    return lines;
	}
	
	public static class TransformedSource{
		public String originalSource;
		public String source;
		public Map<Integer,Integer> sourceLineNumbers;
		public TransformedSource(String originalSource, String source, Map<Integer,Integer> sourceLineNumbers){
			this.originalSource=originalSource;
			this.source=source;
			this.sourceLineNumbers=sourceLineNumbers;
		}
	}
}
