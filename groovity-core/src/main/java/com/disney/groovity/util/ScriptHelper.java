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
package com.disney.groovity.util;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

import com.disney.groovity.BindingDecorator;
import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityConstants;
import com.disney.groovity.compile.GroovityClassLoader;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import groovy.lang.MetaClassImpl;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;
import groovy.lang.Writable;
/**
 * Utility methods for scripts, generally called from generated AST commands
 *
 * @author Alex Vigdor
 */
public class ScriptHelper implements GroovyObject, GroovityConstants {
	public static final ThreadLocal<Binding> THREAD_BINDING = new ThreadLocal<Binding>();
	private static final String THREAD_BINDING_OWNER = INTERNAL_BINDING_PREFIX.concat("ScriptHelper$removeBinding");
	private static final String THREAD_BINDING_PREVIOUS = INTERNAL_BINDING_PREFIX.concat("ScriptHelper$priorBinding");
	private final Groovity groovity;
	private final GroovityClassLoader groovityClassLoader;
	private MetaClass metaClass;
	
	public ScriptHelper(Groovity groovity, GroovityClassLoader groovityClassLoader) {
		this.groovity=groovity;
		this.groovityClassLoader=groovityClassLoader;
		this.metaClass = new MetaClassImpl(ScriptHelper.class);
		metaClass.initialize();
	}
	
	@SuppressWarnings("rawtypes")
	public static ScriptHelper get(Closure body){
		if(body.getClass().getClassLoader() instanceof GroovityClassLoader){
			return ((GroovityClassLoader)body.getClass().getClassLoader()).getScriptHelper();
		}
		//fallback for empty body tag calls to get a hold of the ScriptHelper from the script Class
		if(body.getThisObject()!=null){
			if(body.getThisObject() instanceof Class){
				return ((GroovityClassLoader)((Class)body.getThisObject()).getClassLoader()).getScriptHelper();
			}
		}
		if(body.getOwner()!=null){
			if(body.getOwner() instanceof Class){
				return ((GroovityClassLoader)((Class)body.getOwner()).getClassLoader()).getScriptHelper();
			}
		}
		throw new RuntimeException("Could not find ScriptHelper for "+body);
	}

	public void startRun(final Script script){
		final Binding oldBinding = THREAD_BINDING.get();
		final Binding scriptBinding = script.getBinding();
		if(oldBinding != scriptBinding){
			if(oldBinding!=null){
				scriptBinding.setVariable(THREAD_BINDING_PREVIOUS, oldBinding);
			}
			scriptBinding.setVariable(THREAD_BINDING_OWNER, script);
			THREAD_BINDING.set(scriptBinding);
		}
	}
	
	public boolean processReturn(Script script, Object returnVal) throws IOException{
		if(returnVal instanceof Writable){
			Object out = script.getProperty(OUT);
			if(out instanceof Writer){
				final Binding oldBinding = THREAD_BINDING.get();
				THREAD_BINDING.set(script.getBinding());
				try{
					((Writable)returnVal).writeTo((Writer)out);
				}
				finally{
					if(oldBinding==null) {
						THREAD_BINDING.remove();
					}
					else {
						THREAD_BINDING.set(oldBinding);
					}
				}
				return true;
			}
		}
		return false;
	}
	
	public void endRun(final Script script){
		@SuppressWarnings("rawtypes")
		final Map vars = script.getBinding().getVariables();
		final Object owner = vars.get(THREAD_BINDING_OWNER);
		if(owner==script){
			vars.remove(THREAD_BINDING_OWNER);
			Binding oldBinding = (Binding) vars.get(THREAD_BINDING_PREVIOUS);
			if(oldBinding!=null){
				THREAD_BINDING.set(oldBinding);
				vars.remove(THREAD_BINDING_PREVIOUS);
			}
			else{
				THREAD_BINDING.remove();
			}
		}
	}
	
	public Object run(final String scriptName) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException{
		return groovity.run(scriptName,getBinding());
	}
	
	public Script load(final String scriptName) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		return groovity.load(scriptName, getBinding());
	}
	
	@SuppressWarnings("rawtypes")
	public Object tag(final String tagName, final Map attributes, Closure closure) throws Exception{
		return groovity.tag(tagName,attributes, closure);
	}
	
	public Object tag(final String tagName, @SuppressWarnings("rawtypes") Closure closure) throws Exception{
		return groovity.tag(tagName,Collections.EMPTY_MAP, closure);
	}
	
	@SuppressWarnings("rawtypes")
	protected final Object tag(final String tagName, final Object[] argsArray) throws Exception{
		Map tagParams = null;
		Closure tagClosure=null;
		if(argsArray!=null && argsArray.length>0){
			Object arg1 = argsArray[0];
			if(arg1 instanceof Map){
				tagParams = (Map) arg1;
				if(argsArray.length>1){
					Object arg2 = argsArray[1];
					if(arg2 instanceof Closure){
						tagClosure = (Closure) arg2;
					}
					else if(arg2 instanceof ClosureWritable){
						tagClosure = ((ClosureWritable)arg2).getClosure();
					}
				}
			}
			else{
				if(arg1 instanceof Closure){
					tagClosure = (Closure) arg1;
				}
				else if(arg1 instanceof ClosureWritable){
					tagClosure = ((ClosureWritable)arg1).getClosure();
				}
				else{
					throw new MissingMethodException(tagName, groovityClassLoader.getScriptClass(), argsArray);
				}
			}
		}
		if(tagParams==null){
			tagParams = Collections.EMPTY_MAP;
		}
		if(tagClosure==null){
			tagClosure = new groovy.lang.Closure(groovityClassLoader.getScriptClass()){
				private static final long serialVersionUID = 2274670949356312472L;

				@SuppressWarnings("unused")
				public void doCall(){}
			};
		}
		return groovity.tag(tagName, tagParams, tagClosure);
	}
	
	@SuppressWarnings("unchecked")
	public Binding getBinding(){
		Binding binding = THREAD_BINDING.get();
		if(binding==null){
			//could be static initializer or async code, decorate a plain binding
			//async tags are responsible for setting the THREAD_BINDING explicitly if
			//the bodies need access to the request binding or a derivative thereof
			binding = new Binding();
			BindingDecorator bindingDecorator = groovity.getBindingDecorator();
			if(bindingDecorator!=null){
				bindingDecorator.decorateRecursive(binding.getVariables());
			}
		}
		return binding;
	}

	public Object staticPropertyMissing(String name){
		//only supported resolution for missing static properties is default binding
		Binding binding = getBinding();
		try{
			return binding.getVariable(name);	
		} catch (MissingPropertyException e) {
			if(BINDING.equals(name)){
				return binding;
			}
			throw e;
        }
	}
	
	private void handleException(Exception e){
		if(e instanceof RuntimeException){
			throw (RuntimeException) e;
		}
		throw new RuntimeException(e);
	}
	
	public Object invokeMethod(String name, Object args){
		Object[] argsArray = null;
		if(args instanceof Object[]){
			argsArray = (Object[]) args;
		}
		if(LOAD.equals(name)){
			if(argsArray!=null && argsArray.length>0){
				args = argsArray[0];
			}
			if(args instanceof String){
				try {
					return load((String)args);
				} catch (Exception e) {
					handleException(e);
				}
			}
		}
		else if(RUN.equals(name)){
			if(argsArray!=null && argsArray.length>0){
				args = argsArray[0];
			}
			if(args instanceof String){
				try {
					return run((String)args);
				} catch (Exception e) {
					handleException(e);
				}
			}
		}
		else if(STREAM.equals(name)){
			if(argsArray!=null && argsArray.length>0){
				args = argsArray[0];
			}
			stream(args);
			return null;
		}
		else if(TAG.equals(name)){
			if(argsArray!=null && argsArray.length>1){
				name = argsArray[0].toString();
				argsArray = Arrays.copyOfRange(argsArray, 1, argsArray.length);
			}
		}
		if(groovity.hasTag(name)){
			try {
				return tag(name,argsArray);
			} catch (Exception e1) {
				handleException(e1);
			}
		}
		throw new MissingMethodException(name, groovityClassLoader.getScriptClass(), argsArray);
	}

	public Object getProperty(String name){
		//at runtime try looking up in thread binding
		Binding binding = THREAD_BINDING.get();
		if(binding!=null){
			try{
				return binding.getVariable(name);	
			} catch (MissingPropertyException e) {
				if(BINDING.equals(name)){
					return binding;
				}
				if(CONF.equals(name)) {
					return groovityClassLoader.getConfiguration();
				}
				throw e;
	        }
		}
		throw new MissingPropertyException(name);
	}

	public void stream(final Object o){
		if(o==null){
			return;
		}
		try{
			if(o instanceof Callable){
				@SuppressWarnings("rawtypes")
				final Object r = ((Callable)o).call();
				if(r!=null && r!=o){
					stream(r);
				}
				return;
			}
			final Binding binding = THREAD_BINDING.get();
			if(binding==null){
				return;
			}
			final Writer writer = (Writer) binding.getVariable(OUT);
			if(writer==null){
				return;
			}
			if(o instanceof Writable){
				((Writable)o).writeTo(writer);
			}
			else if(o instanceof CharSequence){
				writer.append((CharSequence)o);
			}
			else if(o.getClass().isArray()){
				@SuppressWarnings("rawtypes")
				Class ct = o.getClass().getComponentType();
				if(ct.isPrimitive()){
					if(ct.equals(Integer.TYPE)){
						writer.append(Arrays.toString((int[])o));
					}
					if(ct.equals(Long.TYPE)){
						writer.append(Arrays.toString((long[])o));
					}
					if(ct.equals(Float.TYPE)){
						writer.append(Arrays.toString((float[])o));
					}
					if(o.getClass().equals(Double.TYPE)){
						writer.append(Arrays.toString((double[])o));
					}
				}
				else{
					writer.append(Arrays.deepToString((Object[])o));
				}
			}
			else{
				writer.append(o.toString());
			}
		}
		catch(RuntimeException e){
			throw e;
		}
		catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public void setProperty(String name, Object newValue){
		Binding binding = THREAD_BINDING.get();
		if(binding!=null){
			binding.setVariable(name, newValue);
		}
		else{
			throw new MissingPropertyException(name);
		}
	}

	public MetaClass getMetaClass() {
		return metaClass;
	}

	public void setMetaClass(MetaClass arg0) {
		this.metaClass=arg0;
	}
	
	public GroovityClassLoader getClassLoader(){
		return groovityClassLoader;
	}
}
