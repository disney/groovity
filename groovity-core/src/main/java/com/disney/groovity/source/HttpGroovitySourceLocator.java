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
package com.disney.groovity.source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import org.apache.http.Header;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a groovity script code base hosted on a web server; the web server must be configured to generate directory listings
 * to enable this locator to discover all the source files hosted at a certain address.
 *
 * @author Alex Vigdor
 */
public class HttpGroovitySourceLocator extends AbstractGroovitySourceLocator{	
	private static Log log = LogFactory.getLog(HttpGroovitySourceLocator.class);
	CloseableHttpClient client;
	private static Pattern hrefPattern = Pattern.compile("\"([^/].*?)\"");	
	private URI base_uri;
	private int threadCount = 4;
	private ExecutorService threadPool;
	
	public HttpGroovitySourceLocator(){}
	public HttpGroovitySourceLocator(URI sourceURI) throws URISyntaxException{
		this.setBaseURI(sourceURI);
		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(10,TimeUnit.SECONDS);
		connManager.setDefaultMaxPerRoute(threadCount);
		this.client = HttpClients.createMinimal(connManager);
		this.threadPool = Executors.newFixedThreadPool(threadCount,new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setName("Groovity HTTP Source Locator "+t.getName());
				return t;
			}
		});
	}
	
	public void destroy(){
		super.destroy();
		threadPool.shutdown();
		try {
			if(!threadPool.awaitTermination(20, TimeUnit.SECONDS)) {
				threadPool.shutdownNow();
				threadPool.awaitTermination(10, TimeUnit.SECONDS);
			}
		} catch (InterruptedException e1) {
			threadPool.shutdownNow();
		}
		try {
			this.client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
		
	public URI getBaseURI() {
		return base_uri;
	}

	public void setBaseURI(URI uri) throws URISyntaxException {
		if(!uri.toString().endsWith("/")){
			uri = new URI(uri.toString()+"/");
		}
		this.base_uri = uri;
	}
	
	public GroovitySource getGroovityScriptSource(String path) throws IOException
	{
		return getHttpGroovityScriptSource(this.base_uri.resolve(path.substring(1))); 
	}
	
	protected HttpGroovitySource getHttpGroovityScriptSource(URI uri) throws IOException{
		HttpHead headReq = new HttpHead(uri);
		CloseableHttpResponse  response = this.client.execute(headReq);
		try {			
			if(log.isDebugEnabled()){
				log.debug("Issued head request for "+uri+", got "+response.getStatusLine());
			}
			long lastModified = System.currentTimeMillis();
			Header header = response.getFirstHeader("Last-Modified");
			if(header!=null){				
				lastModified = DateUtils.parseDate(header.getValue()).getTime();
			}
			String path = uri.getPath().substring(this.base_uri.getPath().length()-1);
			if(log.isDebugEnabled()){
				log.debug("getHttpGroovityScriptSource: path="+path);
			}
			return new HttpGroovitySource(uri, path, lastModified, this);	
			
		} finally {
		    response.close();
		}		
	}	


	
	@Override
	public Iterator<GroovitySource> iterator(){
		AtomicReference<Throwable> errorRef = new AtomicReference<Throwable>();
		List<GroovitySource> list = Collections.synchronizedList(new ArrayList<GroovitySource>());
		try {
			long time1=System.currentTimeMillis();
			CountDownLatch pendingLatch = new CountDownLatch(1);
			
			traverse(this.base_uri, list, new AtomicInteger(),pendingLatch,errorRef);
			pendingLatch.await();
			
			if(log.isDebugEnabled()){
				long time2=System.currentTimeMillis();
				log.debug("HTTPSourceLocator traversed "+list.size()+" sources in "+(time2-time1));
			}
			
		} catch (Exception e) {
			throw new RuntimeException("Error iterating HTTP groovy sources",e);
		}
		if(errorRef.get()!=null){
			throw new RuntimeException("Error iterating HTTP groovy sources",errorRef.get());
		}
		return list.iterator();
	}
	
	private void traverse(final URI uri, final List<GroovitySource> fileList, final AtomicInteger pendingCounter, final CountDownLatch pendingLatch, final AtomicReference<Throwable> errorRef) throws IOException{
		String dirListing = getBody(uri);		
        Matcher hrefMatcher = hrefPattern.matcher(dirListing);
        ArrayList<Runnable> runnables = new ArrayList<Runnable>();
        while(hrefMatcher.find())
        {
        	final String name = hrefMatcher.group(1);
            if(name.endsWith(GROOVITY_SOURCE_EXTENSION)){
            	pendingCounter.incrementAndGet();
            	runnables.add(new Runnable(){

					public void run() {
						try {
							fileList.add( getHttpGroovityScriptSource(uri.resolve(name)));
						} catch (Throwable e) {
							log.error("Error in getHttpGroovityScriptSource: "+e.getMessage(), e );
							errorRef.set(e);
							pendingLatch.countDown();
						}
						finally{
							if(pendingCounter.decrementAndGet()==0){
								pendingLatch.countDown();
							}
						}
					}
            		
            	});
                
                
            } 
            else if(name.endsWith("/")){
            	pendingCounter.incrementAndGet();
            	runnables.add(new Runnable(){

					public void run() {
						try {
							traverse( uri.resolve( name), fileList, pendingCounter, pendingLatch, errorRef);
						} catch (Exception e) {
							log.error("Error in HttpSourceLocator traverse call: "+e.getMessage(), e );
							errorRef.set(e);
							pendingLatch.countDown();
						}
						finally{
							if(pendingCounter.decrementAndGet()==0){
								pendingLatch.countDown();
							}
						}
					}
            		
            	});                 
            }     

        }
		for(Runnable r: runnables){
			threadPool.submit(r);
		}
		if(pendingCounter.get()==0 && fileList.size()==0){
			pendingLatch.countDown();
		}
	}
	
	protected String getBody(URI uri) throws IOException
	{
		HttpGet httpget = new HttpGet(uri);
		int status = 0;
		CloseableHttpResponse response = client.execute(httpget);
		try{
			status = response.getStatusLine().getStatusCode();
			if(status == 200){		
				return EntityUtils.toString(response.getEntity());		
			}
			else{
				EntityUtils.consumeQuietly(response.getEntity());
			}
		}
		finally{
			response.close();
		}
		throw new IOException("IO error in HttpSourceLocator getBody: unable to download "+uri+", status code "+status);
	}
	
    protected CloseableHttpClient getClient()
    {
	    return this.client;
    }
	public int getThreadCount() {
		return threadCount;
	}
	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
	}
}
