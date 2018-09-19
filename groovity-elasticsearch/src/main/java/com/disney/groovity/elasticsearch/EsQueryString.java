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
package com.disney.groovity.elasticsearch;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.regex.Pattern;
/**
 * Parse Elastic search record locator URLs, which will come in the form {/index/type}/id|{/index{/type}}(_search|_count)?{qs}, with index and type 
 * defaulting to es.index and es.type in the configuration map if not present in the URL. The query string may include parameters q, version, from, size or sort.
 * 
 * @author Alex Vigdor
 *
 */
public class EsQueryString {
	static final Pattern numberPattern = Pattern.compile("\\d+");
	private String confIndex = null;
	private String index = null;
	private String confType = null;
	private String type = null;
	private Object idValue = null;
	private String query = null;
	private Long version = null;
	private Integer from = null;
	private Integer size = null;
	private String sort = null;
	private boolean counting = false;
	private boolean searching = false;
	private String source = null;
	
	public EsQueryString() {
		
	}
	
	@SuppressWarnings("rawtypes")
	public EsQueryString(String key, Map conf) {
		if(conf != null) {
			Object cds = conf.get("es.index");
			if(cds!=null) {
				this.confIndex = cds.toString();
				index = confIndex;
			}
			Object ctn = conf.get("es.type");
			if(ctn!=null) {
				this.confType = ctn.toString();
				type = confType;
			}
		}
		if(key==null) {
			key = "";
		}
		if(key.startsWith("{")) {
			//json string maps to source parameter
			source = key;
			return;
		}
		String path = key;
		String queryString = null;
		int qm = key.indexOf("?");
		if(qm>=0) {
			path = key.substring(0,qm);
			queryString = key.substring(qm+1);
		}
		if(path.startsWith("/")) {
			path = path.substring(1);
		}
		String[] pathParts = path.split("/");
		String lastPart = pathParts[pathParts.length-1];
		if(lastPart.equals("_search")) {
			searching=true;
		}
		else if(lastPart.equals("_count")) {
			counting=true;
		}
		switch(pathParts.length) {
			case 2:
				index = pathParts[0];
				if(!searching && !counting) {
					type = pathParts[1];
				}
				break;
			case 3:
				index = pathParts[0];
				type = pathParts[1];
				//flow through
			case 1:
				if(!searching && !counting && !lastPart.trim().isEmpty()) {
					try {
						idValue = URLDecoder.decode(lastPart, "UTF-8");
					} catch (UnsupportedEncodingException e) {}
				}
				break;
		}
		if(queryString!=null) {
			String[] fields = queryString.split("&");
			for(String field: fields) {
				String name = field;
				String value = null;
				int em = field.indexOf("=");
				if(em>0) {
					name = field.substring(0,em);
					value = field.substring(em+1);
				}
				if(value!=null) {
					Object tval = value;
					if(numberPattern.matcher(value).matches()) {
						tval = Long.valueOf(value);
					}
					else {
						try {
							tval = URLDecoder.decode(value.toString(), "UTF-8");
						} catch (UnsupportedEncodingException e) {}
					}
					if(name == null || name.equals("_id")) {
						idValue = tval;
					}
					else if(name.equals("sort")) {
						sort = tval.toString();
					}
					else if(name.equals("from")) {
						from = ((Number)tval).intValue();
					}
					else if(name.equals("size")) {
						size = ((Number)tval).intValue();
					}
					else if(name.equals("version")) {
						version = ((Number)tval).longValue();
					}
					else if(name.equals("q")) {
						query = tval.toString();
					}
					else if(name.equals("source")) {
						source = tval.toString();
					}
				}
			}
		}
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Object getIdValue() {
		return idValue;
	}

	public void setIdValue(Object primaryKeyValue) {
		this.idValue = primaryKeyValue;
	}

	
	public String toRestUpdate() {
		StringBuilder builder = new StringBuilder();
		builder.append(index);
		builder.append("/");
		builder.append(type);
		builder.append("/");
		if(idValue!=null) {
			builder.append(encode(idValue.toString()));
			if(version!=null) {
				builder.append("?version=").append(version);
			}
		}
		return builder.toString();
	}
	public String toRestQuery() {
		StringBuilder builder = new StringBuilder();
		if(index!=null) {
			builder.append(index);
			builder.append("/");
		}
		if(type!=null) {
			builder.append(type);
			builder.append("/");
		}
		if(idValue!=null) {
			builder.append(encode(idValue.toString()));
		}
		else {
			if(isCounting()) {
				builder.append("_count");
			}
			else {
				builder.append("_search");
			}
			String d = "?";
			if(query!=null){
				builder.append(d).append("q=").append(encode(query));
				d="&";
			}
			if(sort!=null) {
				builder.append(d).append("sort=").append(encode(sort));
				d="&";
			}
			if(from!=null) {
				builder.append(d).append("from=").append(from);
				d="&";
			}
			if(size!=null) {
				builder.append(d).append("size=").append(size);
				d="&";
			}
			if(source!=null) {
				builder.append(d).append("source=").append(encode(source));
				d="&";
			}
		}
		return builder.toString();
	}
	
	private String encode(Object o) {
		if(o!=null) {
			try {
				return URLEncoder.encode(o.toString(),"UTF-8");
			} catch (UnsupportedEncodingException e) {
			}
		}
		return "";
	}

	public String toString() {
		if(counting || searching) {
			return toRestQuery();
		}
		StringBuilder builder = new StringBuilder();
		if(index!=null && !index.equals(confIndex)) {
			builder.append(index).append("/");
		}
		if(type!=null && !type.equals(confType)) {
			builder.append(type).append("/");
		}
		if(idValue!=null) {
			builder.append(encode(idValue.toString()));
		}
		return builder.toString();
	}

	public boolean isCounting() {
		return counting;
	}

	public void setCounting(boolean counting) {
		this.counting = counting;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getSort() {
		return sort;
	}

	public void setSort(String sort) {
		this.sort = sort;
	}

	public boolean isSearching() {
		return searching;
	}

	public void setSearching(boolean searching) {
		this.searching = searching;
	}

	public Integer getFrom() {
		return from;
	}

	public void setFrom(Integer from) {
		this.from = from;
	}

	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
	
}
