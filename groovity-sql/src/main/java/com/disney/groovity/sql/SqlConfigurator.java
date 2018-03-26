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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.disney.groovity.conf.AbstractConfigurator;
import com.disney.groovity.conf.ConfigurationKey;
/**
 * An implementation of the Groovity Configurator API that pulls configuration data from a SQL
 * table and expects strings in 3 columns: path, property and value.
 * 
 * @author Alex Vigdor
 *
 */
public class SqlConfigurator extends AbstractConfigurator {
	String dataSource;
	String tableName;
	SqlLoader loader;
	
	public SqlConfigurator(SqlLoader loader, String dataSource, String tableName) {
		this.dataSource=dataSource;
		this.tableName=tableName;
		this.loader=loader;
	}

	@Override
	protected Map<ConfigurationKey, String> loadConfiguration() throws Exception {
		Map<ConfigurationKey,String> rmap = new HashMap<>();
		Connection conn = loader.sql(dataSource).getDataSource().getConnection();
		try{
			CallableStatement cs = conn.prepareCall("SELECT path, property, value FROM ".concat(tableName));
			try{
				ResultSet rs = cs.executeQuery();
				try{
					while(rs.next()){
						ConfigurationKey key = new ConfigurationKey();
						String path = rs.getString(1);
						if(path.startsWith("/")){
							path = path.substring(1);
						}
						key.setPath(path.length()==0 ? new String[0] : path.split("/"));
						key.setProperty(rs.getString(2));
						rmap.put(key, rs.getString(3));
					}
				}
				finally{
					rs.close();
				}
			}
			finally{
				cs.close();
			}
		}
		finally{
			conn.close();
		}
		return rmap;
	}

}
