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
package com.disney.groovity.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.disney.groovity.compile.GroovityClassLoader;

import groovy.lang.Closure;
import groovy.sql.Sql;

/**
 * Basic facility for acquiring Groovy SQL facades for datasources;
 * Names are  JNDI names presumed to be prefixed with "java:comp/env/jdbc/"
 * Bind methods allow installing datasources if the container is not being used to manage them
 * sql() methods retrieves the Sql object at runtime
 *
 * @author Alex Vigdor
 */
public class SqlLoader {
	private final static Logger logger = Logger.getLogger(SqlLoader.class.getName());
	final ConcurrentHashMap<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();
	final ConcurrentHashMap<String, List<String>> pendingInits = new ConcurrentHashMap<>();
	final ConcurrentHashMap<String,List<Closure>> pendingCalls = new ConcurrentHashMap<>();
	final Set<SqlConfiguratorSource> sqlConfiguratorSources = ConcurrentHashMap.newKeySet();
	final GroovityClassLoader loader;
	
	public SqlLoader(GroovityClassLoader loader){
		this.loader = loader;
	}
	
	public Sql sql(String name) throws NamingException{
		DataSource ds = dataSourceCache.get(name);
		if(ds==null) {
			name = qualifyName(name);
			InitialContext ic = new InitialContext();
			ds = (DataSource)ic.lookup(name);
			dataSourceCache.put(name, ds);
		}
		return new Sql(ds);
	}
	
	public boolean isBound(String name){
		if(dataSourceCache.containsKey(name)){
			//we have a local copy so we know it exists
			return true;
		}
		//check in JNDI if an object exists there we haven't encountered yet
		try{
			InitialContext ic = new InitialContext();
			name = qualifyName(name);
			Object o = ic.lookup(name);
			if(o instanceof DataSource){
				return true;
			}
		}
		catch(NamingException ne){
		}
		return false;
	}
	
	public void bind(String name, DataSource ds) throws NamingException{
		if(dataSourceCache.containsKey(name)){
			logger.warning("DataSource Binding for "+name+" is replacing "+dataSourceCache.get(name)+" with "+ds);
		}
		
		dataSourceCache.put(name, ds);
		Sql sql = new Sql(ds);
		try {
			List<String> cmds = pendingInits.get(name);
			if(cmds!=null){
				synchronized(cmds){
					invoke(name,sql,cmds);
					cmds.clear();
				}
			}
			for(SqlConfiguratorSource configuratorSource: sqlConfiguratorSources){
				if(!configuratorSource.installed() && configuratorSource.getDataSource().equals(name)){
					configuratorSource.install();
				}
			}
			List<Closure> calls = pendingCalls.get(name);
			if(calls!=null){
				synchronized (calls) {
					for(Closure c: calls){
						c.call(sql);
					}
					calls.clear();
				}
			}
		}
		finally {
			sql.close();
		}
	}
	
	private String qualifyName(String name){
		if(!name.startsWith("java:comp/env")){
			if(name.startsWith("jdbc/")){
				name = "java:comp/env/".concat(name);
			}
			else{
				name = "java:comp/env/jdbc/".concat(name);
			}
		}
		return name;
	}
	
	public void unbind(String name, DataSource ds) throws NamingException{
		DataSource source = dataSourceCache.get(name);
		if(source!=null && source.equals(ds)){
			dataSourceCache.remove(name);
		}
	}
	
	public void init(String name, String command) throws NamingException{
		init(name, Arrays.asList(command));
	}
	
	public void init(String name, List<String> commands) throws NamingException{
		if(isBound(name)){
			invoke(name, sql(name),commands);
		}
		else{
			List<String> pc = pendingInits.get(name);
			if(pc==null){
				pc = new ArrayList<>();
				List<String> oldPc = pendingInits.putIfAbsent(name,pc);
				if(oldPc!=null){
					pc=oldPc;
				}
			}
			synchronized(pc){
				pc.addAll(commands);
			}
		}
	}
	public void call(String name, Closure command) throws NamingException{
		call(name,Arrays.asList(command));
	}
	public void call(String name, List<Closure> command) throws NamingException{
		if(isBound(name)){
			call(name,sql(name),command);
		}
		else{
			List<Closure> pc = pendingCalls.get(name);
			if(pc==null){
				pc = new ArrayList<>();
				List<Closure> oldPc = pendingCalls.putIfAbsent(name,pc);
				if(oldPc!=null){
					pc=oldPc;
				}
			}
			synchronized(pc){
				pc.addAll(command);
			}
		}
	}
	
	private void invoke(String name, Sql sql, List<String> commands){
		int errors = 0;
		for(String command:commands){
			try{
				sql.execute(command);
			}
			catch(Exception e){
				logger.log(Level.SEVERE, "Problem initializing datasource "+name, e);
				errors++;
			}
		}
		logger.info("Initialized data source "+name+" with "+commands.size()+" commands and "+errors+" errors");
	}
	
	private void call(String name, Sql sql, List<Closure> commands){
		int errors = 0;
		for(Closure c: commands){
			try{
				c.call(sql);
			}
			catch(Exception e){
				logger.log(Level.SEVERE, "Problem calling datasource "+name, e);
				errors++;
			}
		}
		logger.info("Called data source "+name+" with "+commands.size()+" commands and "+errors+" errors");
	}
	
	public void destroy(){
		dataSourceCache.clear();
	}
	
	public void configurator(String dataSourceName, String tableName) throws NamingException{
		SqlConfiguratorSource scs = new SqlConfiguratorSource(dataSourceName, tableName);
		if(sqlConfiguratorSources.add(scs)){
			if(isBound(dataSourceName)){
				scs.install();
			}
		}
	}
	
	private class SqlConfiguratorSource{
		private final String dataSource; 
		private final String table;
		private boolean installed = false;
		private SqlConfiguratorSource(String dataSource, String table){
			this.dataSource=dataSource;
			this.table=table;
		}
		
		public String getDataSource() {
			return dataSource;
		}
		public String getTable() {
			return table;
		}
		public boolean installed(){
			return installed;
		}
		public void install() throws NamingException{
			if(!installed){
				SqlConfigurator sc = new SqlConfigurator(getOuterType(),dataSource, table);
				loader.addConfigurator(sc);
				installed = true;
				logger.info("Installed SQL configurator for datasource "+dataSource+" table "+table);
			}
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((dataSource == null) ? 0 : dataSource.hashCode());
			result = prime * result + ((table == null) ? 0 : table.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SqlConfiguratorSource other = (SqlConfiguratorSource) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (dataSource == null) {
				if (other.dataSource != null)
					return false;
			} else if (!dataSource.equals(other.dataSource))
				return false;
			if (table == null) {
				if (other.table != null)
					return false;
			} else if (!table.equals(other.table))
				return false;
			return true;
		}
		private SqlLoader getOuterType() {
			return SqlLoader.this;
		}
		@Override
		public String toString() {
			return "SqlConfiguratorSource [dataSource=" + dataSource + ", table=" + table + "]";
		}
	}
}
