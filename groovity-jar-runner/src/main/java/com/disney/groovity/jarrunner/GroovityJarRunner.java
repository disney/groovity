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
package com.disney.groovity.jarrunner;

import com.disney.groovity.servlet.container.GroovityServletContainer;
import com.disney.groovity.servlet.container.GroovityServletContainerBuilder;

/**
 * Simple main class for groovity standalone jars
 * 
 * @author Alex Vigdor
 *
 */
public class GroovityJarRunner {
	
	public static void main(String[] args) {
		try{
			int port = -1;
			int securePort = -1;
			String keyStoreLocation = null;
			String keyStorePassword = null;
			if(args.length>0){
				port = Integer.parseInt(args[0]);
				if(args.length>1) {
					securePort = Integer.parseInt(args[1]);
					if(args.length>2) {
						keyStoreLocation = args[2];
						if(args.length > 3) {
							keyStorePassword = args[3];
						}
						else {
							keyStorePassword = "";
						}
					}
				}
			}
			GroovityServletContainerBuilder groovityServletContainerBuilder = new GroovityServletContainerBuilder()
					.setPort(port)
					.setSecurePort(Integer.valueOf(securePort))
					.setSecureKeyStoreLocation(keyStoreLocation)
					.setSecureKeyStorePassword(keyStorePassword);
			GroovityServletContainer container = groovityServletContainerBuilder.build();
			container.start();
			try{
				container.enterConsole();
		    }
		    finally{
		    	container.stop();
		    }
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

}
