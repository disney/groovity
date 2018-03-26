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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
/**
 * Parse SQL record locator URLs, which will come in the form count/{datasource}/{table}?{field}={value}, with datasource, table and field all 
 * defaulting to sql.datasource, sql.tableName and sql.primaryKey in the configuration map if not present in the URL.  Count if present indicates only a 
 * count is desired; offset, limit and orderBy attributes in the query string can be used to control pagination.
 * 
 * @author Alex Vigdor
 *
 */
public class SqlRecordLocator {
	static final Pattern numberPattern = Pattern.compile("\\d+");
	private String confDataSource = null;
	private String dataSource = null;
	private String confTableName = null;
	private String tableName = null;
	private String primaryKeyField = "id";
	private Object primaryKeyValue = null;
	private Map<String, Object> fieldValues = null;
	private String orderBy = null;
	private Integer offset = null;
	private Integer limit = null;
	private boolean counting = false;
	
	public SqlRecordLocator() {
		
	}
	
	public SqlRecordLocator(String key, Map conf) {
		Object cds = conf.get("sql.dataSource");
		this.confDataSource = cds != null ? cds.toString() : "DefaultDataSource";
		Object ctn = conf.get("sql.tableName");
		this.confTableName = ctn != null ? ctn.toString() : null;
		Object pk = conf.get("sql.primaryKey");
		if(pk!=null) {
			primaryKeyField = pk.toString();
		}
		if(key==null) {
			key = "";
		}
		if(key.startsWith("count/") || key.startsWith("count?")) {
			counting = true;
			key = key.substring(6);
		}
		int qm = key.indexOf("?");
		if(qm==-1) {
			//no datasource or table name
			dataSource = confDataSource;
			if(confTableName==null) {
				throw new RuntimeException("No table found in URL or configuraton for SQL Record Locator "+key+" "+conf);
			}
			tableName = confTableName;
		}
		else {
			String tableLocation = key.substring(0, qm);
			int sm = tableLocation.indexOf("/");
			if(sm==-1) {
				//no datasource
				dataSource = confDataSource;
				tableName = tableLocation;
			}
			else {
				dataSource = tableLocation.substring(0,sm);
				tableName = tableLocation.substring(sm+1);
			}
		}
		String recordLocation = qm > 0 ? key.substring(qm+1) : key;
		String[] fields = recordLocation.split("&");
		for(String field: fields) {
			String name = null;
			String value = field;
			int em = field.indexOf("=");
			if(em>0) {
				name = field.substring(0,em);
				value = field.substring(em+1);
			}
			Object tval = value;
			if(numberPattern.matcher(value).matches()) {
				tval = Long.valueOf(value);
			}
			else {
				try {
					tval = URLDecoder.decode(value.toString(), "UTF-8");
				} catch (UnsupportedEncodingException e) {}
			}
			if(name == null || name.equals(primaryKeyField)) {
				primaryKeyValue = tval;
			}
			else if(name.equals("orderBy")) {
				orderBy = tval.toString();
			}
			else if(name.equals("offset")) {
				offset = ((Number)tval).intValue();
			}
			else if(name.equals("limit")) {
				limit = ((Number)tval).intValue();
			}
			else {
				if(fieldValues==null) {
					fieldValues = new LinkedHashMap<>();
				}
				fieldValues.put(name, tval);
			}
		}
	}

	public String getDataSource() {
		return dataSource;
	}

	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getPrimaryKeyField() {
		return primaryKeyField;
	}

	public void setPrimaryKeyField(String primaryKeyField) {
		this.primaryKeyField = primaryKeyField;
	}

	public Object getPrimaryKeyValue() {
		return primaryKeyValue;
	}

	public void setPrimaryKeyValue(Object primaryKeyValue) {
		this.primaryKeyValue = primaryKeyValue;
	}

	public Map<String, Object> getFieldValues() {
		return fieldValues;
	}

	public void setFieldValues(Map<String, Object> fieldValues) {
		this.fieldValues = fieldValues;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		if(!dataSource.equals(confDataSource)) {
			if(isCounting()) {
				builder.append("count/");
			}
			builder.append(dataSource).append("/").append(tableName).append("?");
		}
		else if(!tableName.equals(confTableName)) {
			if(isCounting()) {
				builder.append("count/");
			}
			builder.append(tableName).append("?");
		}
		else if(isCounting()) {
			builder.append("count?");
		}
		if(primaryKeyValue!=null) {
			builder.append(primaryKeyValue.toString());
		}
		else {
			boolean first = true;
			for(Entry<String, Object> entry: fieldValues.entrySet()) {
				if(!first) {
					builder.append("&");
				}
				builder.append(entry.getKey()).append("=");
				if(entry.getValue()!=null) {
					builder.append(entry.getValue());
				}
				first=false;
			}
			if(orderBy!=null) {
				builder.append("&orderBy=").append(orderBy);
			}
			if(offset!=null) {
				builder.append("&offset=").append(offset.toString());
			}
			if(limit!=null) {
				builder.append("&limit=").append(limit.toString());
			}
		}
		return builder.toString();
	}

	public boolean isCounting() {
		return counting;
	}

	public void setCounting(boolean counting) {
		this.counting = counting;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}

	public Integer getOffset() {
		return offset;
	}

	public void setOffset(Integer offset) {
		this.offset = offset;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}
	
}
