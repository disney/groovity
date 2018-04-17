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
package com.disney.groovity.websocket;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.security.Principal;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.websocket.CloseReason;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

import com.disney.groovity.model.ModelJsonWriter;

import groovy.json.JsonSlurper;
import groovy.lang.Writable;
/**
 * A wrapper around a standard java websocket to enable idiomatic interaction in Groovity.  
 *
 * @author Alex Vigdor
 */
public class WebSocket implements AutoCloseable {
	static final Logger log = Logger.getLogger(WebSocket.class.getSimpleName());

	Session session;
	private String name;
	@SuppressWarnings("rawtypes")
	private Class messageFormat;
	private Consumer<Object> messageHandler;
	private Consumer<CloseReason> closeHandler;
	private Consumer<Throwable> errorHandler;
	
	public WebSocket(final Session session){
		this.session=session;
	}
	/**
	 * Define the message handler for this socket along with a Class indicating the desired message format
	 * @param handler
	 * @param messageFormat
	 */
	public void setMessageHandler(Consumer<Object> handler, @SuppressWarnings("rawtypes") final Class messageFormat){
		if(handler!=null){
			this.messageHandler = handler;
			this.messageFormat = messageFormat;
			if(log.isLoggable(Level.FINE)){
				log.fine("REGISTERING "+handler+" on "+session);
			}
			session.addMessageHandler(new InputStreamHandler());
			session.addMessageHandler(new ReaderHandler());
		}
	}
	/**
	 * Attach a close handler to this socket
	 * @param closer
	 */
	public void setCloseHandler(Consumer<CloseReason> closer){
		this.closeHandler = closer;
	}
	/**
	 * Attach an error handler to this socket
	 * @param errorHandler
	 */
	public void setErrorHandler(Consumer<Throwable> errorHandler) {
		this.errorHandler=errorHandler;
	}
	public Session getSession(){
		return session;
	}
	public void onClose(CloseReason reason){
		if(closeHandler!=null){
			closeHandler.accept(reason);
		}
	}
	public void onError(Throwable error){
		if(errorHandler!=null){
			errorHandler.accept(error);
		}
		else {
			Level logLevel = Level.WARNING;
			if(error instanceof IOException) {
				logLevel = Level.FINE;
			}
			log.log(logLevel,"WebSocket encountered error ",error);
		}
	}
	public void close() throws IOException{
		session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY,"Socket closed"));
	}
	public boolean isOpen(){
		return session.isOpen();
	}
	/**
	 * Send a message on this socket; native recognition of Writable, CharSequence, byte[], char[], InputStream, Reader,
	 * ByteBuffer, File and DataSource, with a fallthrough to JSON serialization
	 * 
	 * @param message
	 * @throws IOException
	 */
	public void call(Object message) throws IOException{
		if(!session.isOpen()){
			throw new ClosedChannelException();
		}
		if(message instanceof Writable){
			try(Writer writer = session.getBasicRemote().getSendWriter()){
				((Writable) message).writeTo(writer);
			}
		}
		else if(message instanceof CharSequence){
			try(Writer writer = session.getBasicRemote().getSendWriter()){
				writer.append((CharSequence) message);
			}
		}
		else if(message instanceof byte[]){
			session.getBasicRemote().sendBinary(ByteBuffer.wrap((byte[])message));
		}
		else if(message instanceof char[]){
			try(Writer writer = session.getBasicRemote().getSendWriter()){
				writer.write((char[])message);
			}
		}
		else if(message instanceof InputStream){
			try(InputStream in = (InputStream)message; OutputStream out = session.getBasicRemote().getSendStream()){
				byte[] buffer = new byte[8192];
				int c=0;
				while((c=in.read(buffer))!=-1){
					out.write(buffer,0,c);
				}
			}
		}
		else if(message instanceof Reader){
			try(Reader in = (Reader)message; Writer out = session.getBasicRemote().getSendWriter()){
				char[] buffer = new char[8192];
				int c=0;
				while((c=in.read(buffer))!=-1){
					out.write(buffer, 0, c);
				}
			}
		}
		else if(message instanceof ByteBuffer){
			session.getBasicRemote().sendBinary((ByteBuffer) message);
		}
		else if(message instanceof File){
			try(InputStream in = new FileInputStream((File)message); OutputStream out = session.getBasicRemote().getSendStream()){
				byte[] buffer = new byte[8192];
				int c=0;
				while((c=in.read(buffer))!=-1){
					out.write(buffer,0,c);
				}
			}
		}
		else if(message instanceof DataSource){
			try(InputStream in = ((DataSource)message).getInputStream(); OutputStream out = session.getBasicRemote().getSendStream()){
				byte[] buffer = new byte[8192];
				int c=0;
				while((c=in.read(buffer))!=-1){
					out.write(buffer,0,c);
				}
			}
		}
		else{
			//serialize as json if it's not one of the known text/binary types
			try(Writer writer = session.getBasicRemote().getSendWriter()){
				new ModelJsonWriter(writer).visit(message);
			}
			catch(IOException e){
				throw e;
			}
			catch(RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				throw new RuntimeException("Error serializing message to websocket ",e);
			}
		}
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Principal getUserPrincipal(){
		return session.getUserPrincipal();
	}
	
	class InputStreamHandler implements MessageHandler.Whole<InputStream>{
		
		@Override
		public void onMessage(final InputStream message) {
			Object argument = message;
			try{
				try{
					if(messageFormat==null || !messageFormat.equals(InputStream.class)){
						if(messageFormat==null || 
								messageFormat.equals(Object.class) || 
								messageFormat.equals(byte[].class) || 
								messageFormat.equals(ByteBuffer.class)){
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							byte[] buffer = new byte[8192];
							int c=0;
							while((c=message.read(buffer))!=-1){
								baos.write(buffer,0,c);
							}
							if(messageFormat.equals(ByteBuffer.class)){
								argument = ByteBuffer.wrap(baos.toByteArray());
							}
							else{
								argument = baos.toByteArray();
							}
						}
						else{
							argument = DefaultTypeTransformation.castToType(new JsonSlurper().parse(message), messageFormat);
						}
					}
					messageHandler.accept(argument);
				}
				finally{
					message.close();
				}
			}
			catch(Exception e){
				log.log(Level.SEVERE,"Error processing socket message",e);
			}
		}
	}
	
	class ReaderHandler implements MessageHandler.Whole<Reader> {
		
		@Override
		public void onMessage(Reader message) {
			Object argument = message;
			try{
				try{
					if(messageFormat==null || !messageFormat.equals(Reader.class)){
						if(messageFormat==null || messageFormat.equals(Object.class) || messageFormat.equals(String.class)){
							CharArrayWriter caw = new CharArrayWriter();
							char[] buffer = new char[8192];
							int c=0;
							while((c=message.read(buffer))!=-1){
								caw.write(buffer,0,c);
							}
							argument = caw.toString();
						}
						else{
							argument = DefaultTypeTransformation.castToType(new JsonSlurper().parse(message), messageFormat);
						}
					}
					messageHandler.accept(argument);
				}
				finally{
					message.close();
				}
			}
			catch(Exception e){
				log.log(Level.SEVERE,"Error processing socket message",e);
			}	
		}
	}
}
