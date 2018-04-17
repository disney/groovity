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
package com.disney.groovity.servlet;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.HandshakeResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import com.disney.groovity.websocket.WebSocket;

/**
 * A java websocket endpoint shim that captures all single-path-element web socket requests and
 * forwards them to dynamically registered groovity endpoints
 *
 * @author Alex Vigdor
 */
public class GroovityServerEndpoint extends Endpoint{
	private static Logger log = Logger.getLogger(GroovityServerEndpoint.class.getSimpleName());
	private GroovityScriptViewFactory factory;
	private WebSocket socket = null;
	
	public GroovityServerEndpoint(GroovityScriptViewFactory factory){
		this.factory=factory;
	}

	@Override
	public void onOpen(Session session, EndpointConfig config) {
		factory.socketOpen();
		Map<String,String> pathparms = session.getPathParameters();
		String socketName = null;
		Exception err = null;
		if(pathparms.containsKey("socketName")){
			try {
				socketName = (String)pathparms.get("socketName");
				socket = factory.createSocket(socketName, session);
			} catch (Exception e) {
				err = e;
				log.log(Level.SEVERE,"Error opening socket "+socketName,e);
			}
		}
		if(socket==null){
			if(err==null){
				log.warning("NO SOCKET FOUND "+socketName);
			}
			try {
				session.close();
			} catch (IOException e) {
				log.log(Level.SEVERE, "Unable to close session for missing socket", e);
			}
		}
	}

	@Override
	public void onClose(Session session, CloseReason reason) {
		factory.socketClose();
		if(socket!=null){
			try{
				socket.onClose(reason);
			}
			catch(Exception e){
				log.log(Level.SEVERE,"Error closing web socket session ",e);
			}
		}
	}
	@Override
	public void onError(Session session, Throwable thr){
		factory.socketError();
		if(socket!=null) {
			try{
				socket.onError(thr);
			}
			catch(Exception e){
				log.log(Level.SEVERE,"Error handling error for web socket server session ",e);
				log.log(Level.SEVERE,"Original error was ",thr);
			}
		}
	}
	
	public static class Configurator extends ServerEndpointConfig.Configurator{
		final private GroovityScriptViewFactory factory;
		
		public Configurator(GroovityScriptViewFactory factory){
			this.factory=factory;
		}
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public GroovityServerEndpoint getEndpointInstance(Class endpointClass){
			return new GroovityServerEndpoint(factory);
		}

		@Override
		public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
			//add HttpSession to WebSocketSession so web socket handlers can share session state with regular HTTP requests
			HttpSession httpSession = (HttpSession) request.getHttpSession();
			if(httpSession!=null) {
				config.getUserProperties().put(HttpSession.class.getName(), httpSession);
			}
		}
	}
}
