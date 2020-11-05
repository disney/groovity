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
package com.disney.groovity.model;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;

import com.disney.groovity.util.MetaPropertyLookup;
import com.disney.groovity.util.XmlEscapingWriter;

import groovy.lang.Closure;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import groovy.lang.MetaProperty;
import groovy.lang.Writable;

import static com.disney.groovity.util.MetaPropertyLookup.getAnnotation;

/**
 * A ModelVisitor that produces a serialized XML representation of a Model, supports JAXB annotations
 * to control how the XML is formed
 * 
 * @author Alex Vigdor
 *
 */
public class ModelXmlWriter extends ModelWalker{
	static TransformerFactory transformerFactory = TransformerFactory.newInstance();
	final static Map<Class<?>,String> NO_NAMES = Collections.unmodifiableMap(new HashMap<>());
	final static Map<Class<?>,String> SKIP_TAG = Collections.unmodifiableMap(new HashMap<>());
	final static Logger log = Logger.getLogger(ModelXmlWriter.class.getName());
	final static ConcurrentHashMap<Class<?>, String> ELEMENT_NAME_CACHE = new ConcurrentHashMap<>();
	final protected Writer writer;
	final protected Writer escape;
	int indent = 0;
	final String indentChars;
	ArrayDeque<Optional<String>> listElementNames = new ArrayDeque<>();
	ArrayDeque<Optional<Map<Class<?>,String>>> listTypedElementNames = new ArrayDeque<>();
	String rootElementName;
	boolean inAttribute=false;
	private Transformer transformer = null;
	boolean doDelimit = false;
	Map<String, String> namespacePrefixes;
	Map<String, String> declareNamespaces;
	Map<String, String> usedNamespacePrefixs;
	boolean root = true;
	
	public ModelXmlWriter(Writer writer) {
		this(writer,null);
	}
	
	public ModelXmlWriter(Writer out, String indentChars) {
		this.writer = out;
		this.escape = new XmlEscapingWriter(out);
		this.indentChars = indentChars;
		if(indentChars==null || indentChars.length()==0) {
			this.indent = -1;
		}
	}
	
	protected String getNamespacePrefix(String uri) {
		if(namespacePrefixes==null) {
			namespacePrefixes = new HashMap<>();
		}
		String prefix = namespacePrefixes.get(uri);
		if(prefix == null) {
			if(usedNamespacePrefixs!=null) {
				prefix = usedNamespacePrefixs.get(uri);
			}
			else {
				usedNamespacePrefixs = new HashMap<>();
			}
			if(prefix==null) {
				prefix = "ns".concat(String.valueOf(usedNamespacePrefixs.size()+1));
				usedNamespacePrefixs.put(uri, prefix);
			}
			namespacePrefixes.put(uri, prefix);
			if(declareNamespaces==null) {
				declareNamespaces = new LinkedHashMap<>();
			}
			declareNamespaces.put(uri, prefix);
		}
		return prefix;
	}
	
	protected String getTagName(String namespace, String localName) {
		if(namespace==null || namespace.isEmpty() || "##default".equals(namespace)) {
			return localName;
		}
		String prefix = getNamespacePrefix(namespace);
		return prefix.concat(":").concat(localName);
	}

	public void visitNull() throws Exception {
	}
	
	protected void writeIndent() throws IOException {
		if(indent>0) {
			for(int i=0;i<indent;i++) {
				writer.write(indentChars);
			}
		}
	}
	
	protected void delimit() throws IOException {
		if(doDelimit) {
			if(indent>=0) {
				doDelimit=false;
				writer.write("\n");
			}
			if(indent>0) {
				writeIndent();
			}
		}
	}
	
	private boolean writeString(Object o) throws Exception {
		if(o == null) {
			writer.write("");
			return true;
		}
		if(o instanceof CharSequence) {
			escape.append((CharSequence) o);
			return true;
		}
		if(o instanceof Number || o instanceof Boolean) {
			writer.write(o.toString());
			return true;
		}
		if(o instanceof Date) {
			writer.write(String.valueOf(((Date)o).getTime()));
			return true;
		}
		if(o instanceof Writable  && ! (o instanceof Model)) {
			((Writable)o).writeTo(escape);
			return true;
		}
		if(o instanceof Class) {
			escape.write(((Class<?>)o).getName());
			return true;
		}
		if(o instanceof URI || o instanceof URL || o instanceof Throwable || o.getClass().isEnum()) {
			escape.write(o.toString());
			return true;
		}
		if(o instanceof File) {
			escape.write(((File)o).getName());
			return true;
		}
		if(o instanceof byte[]) {
			escape.write(Base64.getEncoder().encodeToString((byte[]) o));
			return true;
		}
		return false;
	}
	
	public void visitObject(Object o) throws Exception {
		if(!writeString(o)) {
			if(o instanceof Node) {
				if(transformer==null) {
					transformer = transformerFactory.newTransformer();
					transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				}
				transformer.transform(new DOMSource((Node)o), new StreamResult(writer));
				doDelimit=true;
			}
			else if(o instanceof JAXBElement) {
				JAXBElement<?> je = (JAXBElement<?>)o;
				String tn = getTagName(je.getName().getNamespaceURI(), je.getName().getLocalPart());
				writeTag(tn, je.getValue(), t -> {
					try {
						visit(t);
					} catch (RuntimeException e) {
						throw e;
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
			}
			else if(o instanceof Closure) {
				@SuppressWarnings("rawtypes")
				Closure c = (Closure) o;
				visit(c.call());
			}
			else if(o instanceof Future) {
				@SuppressWarnings("rawtypes")
				Future f = (Future) o;
				visit(f.get());
			}
			else{
				if(inAttribute) {
					escape.write(o.toString());
					return;
				}
				if(root) {
					root=false;
					//create root element
					writeTag(getRootElementName(o), o, r->{
						try {
							if(r!=o) {
								visitObject(r);
							}
							else {
								super.visitObject(r);
							}
						}
						catch(RuntimeException e) {
							throw e;
						}
						catch(Exception e) {
							throw new RuntimeException(e);
						}
						doDelimit=true;
					});
				}
				else {
					super.visitObject(o);
				}
			}
		}
	}

	protected String getRootElementName(Object o) {
		if(rootElementName!=null) {
			return rootElementName;
		}
		return getElementName(o);
	}
	
	protected String getElementName(Object o) {
		Class<?> c = o.getClass();
		String name = ELEMENT_NAME_CACHE.get(c);
		if(name!=null) {
			return name;
		}
		XmlRootElement xre = c.getAnnotation(XmlRootElement.class);
		String namespace = null;
		if(xre!=null) {
			Package p = c.getPackage();
			if(p!=null) {
				XmlSchema schema = p.getAnnotation(XmlSchema.class);
				if(schema!=null && schema.xmlns()!=null) {
					if(usedNamespacePrefixs==null) {
						usedNamespacePrefixs = new HashMap<>();
					}
					for(XmlNs xns : schema.xmlns()) {
						if(!usedNamespacePrefixs.containsKey(xns.namespaceURI())) {
							usedNamespacePrefixs.put(xns.namespaceURI(), xns.prefix());
						}
					}
				}
			}
			namespace = xre.namespace();
			if(!"##default".equals(xre.name())) {
				name = getTagName(namespace, xre.name());
			}
		}
		else {
			XmlElement x = c.getAnnotation(XmlElement.class);
			if(x!=null) {
				namespace = x.namespace();
				if(!"##default".equals(x.name())) {
					name = getTagName(namespace, x.name());
				}
			}
		}
		if(name==null) {
			String oname = o.getClass().getSimpleName();
			if(oname.endsWith("[]")) {
				oname = oname.substring(0,oname.length()-2);
			}
			if(Character.isUpperCase(oname.charAt(0))) {
				char[] namechars = oname.toCharArray();
				int stop = 1;
				for(int i=1;i<namechars.length;i++) {
					if(Character.isUpperCase(namechars[i])) {
						stop++;
						continue;
					}
					if(stop>1) {
						stop--;
					}
					break;
				}
				for(int i=0;i<stop;i++) {
					namechars[i] = Character.toLowerCase(namechars[i]);
				}
				oname = new String(namechars);
			}
			name = getTagName(namespace, oname);
		}
		ELEMENT_NAME_CACHE.put(c, name);
		return name;
	}
	
	@SuppressWarnings("rawtypes")
	private void doVisitList(Object o) {
		try {
			super.visitList((Iterable)o);
		}
		catch(RuntimeException e) {
			throw e;
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void visitList(@SuppressWarnings("rawtypes") Iterable o) throws Exception {
		if(root) {
			root=false;
			//create root element
			writeTag(getRootElementName(o), o, this::doVisitList);
		}
		else {
			this.doVisitList(o);
		}
	}
	
	public void visit(Object o) throws Exception {
		super.visit(o);
	}
	
	@SuppressWarnings("rawtypes")
	public void visitObjectField(String name, Object value) throws Exception{
		if(inAttribute) {
			writer.write(" ");
			writer.write(name);
			writer.write("=\"");
			super.visitObjectField(name, value);
			writer.write("\"");
			return;
		}
		boolean writeTag = true;
		Object currentObject = getCurrentObject();
		MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(currentObject.getClass());
		MetaProperty mp = mc.hasProperty(currentObject, name);
		XmlAttribute xa = getAnnotation(mp, XmlAttribute.class);
		if(xa!=null) {
			//attributes are written with open tag
			return;
		}
		String listElementName = null;
		//aggressively resolve closures and futures so we can determine type
		if(value instanceof Closure) {
			value = ((Closure) value).call();
		}
		else if(value instanceof Future) {
			value = ((Future) value).get();
		}
		if(value instanceof List || (value!=null && value.getClass().isArray()) 
				|| (mp!=null && (mp.getType().isArray() || List.class.isAssignableFrom(mp.getType())))) {
			writeTag = false;
			listElementName = name;
			Map<Class<?>,String> listTypedNames=NO_NAMES;
			XmlElementWrapper xew = getAnnotation(mp, XmlElementWrapper.class);
			if(xew!=null) {
				writeTag = true;
				if(!"##default".equals(xew.name())) {
					name = xew.name();
				}
				name = getTagName(xew.namespace(), name);
			}
			XmlElement xe = getAnnotation(mp, XmlElement.class);
			if(xe!=null) {
				if(!"##default".equals(xe.name())) {
					listElementName = xe.name();
				}
				listElementName = getTagName(xe.namespace(), listElementName);
			}
			if(xe==null) {
				XmlElements xes = getAnnotation(mp, XmlElements.class);
				if(xes!=null) {
					listTypedNames = new HashMap<>();
					for(int i=0; i<xes.value().length;i++) {
						XmlElement e = xes.value()[i];
						listTypedNames.put(e.type(), e.name());
					}
				}
			}
			XmlList xel = getAnnotation(mp, XmlList.class);
			if(xel!=null) {
				writeTag = true;
				name = listElementName;
				value = transformField(mp, value);
			}
			XmlMixed xm = getAnnotation(mp, XmlMixed.class);
			if(xm!=null) {
				listTypedNames = SKIP_TAG;
			}
			listElementNames.push(Optional.of(listElementName));
			listTypedElementNames.push(Optional.ofNullable(listTypedNames));
		}
		else {
			XmlElement xe = getAnnotation(mp, XmlElement.class);
			if(xe!=null) {
				if(!"##default".equals(xe.name())) {
					name = xe.name();
				}
				name = getTagName(xe.namespace(), name);
			}
			listElementNames.push(Optional.empty());
			listTypedElementNames.push(Optional.empty());
		}
		doDelimit = true;
		if(writeTag) {
			final String n = name;
			writeTag(name,value, o ->{
				try {
					super.visitObjectField(n, o);
				} catch(RuntimeException e) {
					throw e;
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
		else {
			super.visitObjectField(name, value);
		}
		listElementNames.pop();
		listTypedElementNames.pop();
	}
	
	private Object transformField(MetaProperty mp, Object value) {
		if(mp==null) {
			return value;
		}
		if(getAnnotation(mp, XmlList.class) != null
				|| getAnnotation(mp, XmlAttribute.class) != null
				|| getAnnotation(mp, XmlValue.class) != null) {
			Iterable<?> i = toIterableIfPossible(value);
			if(i!=null) {
				StringBuilder builder = new StringBuilder();
				boolean delim = false;
				for(Object o: i) {
					if(delim) {
						builder.append(" ");
					}
					else {
						delim=true;
					}
					builder.append(String.valueOf(o));
				}
				value = builder.toString();
			}
		}
		return value;
	}
	
	protected void writeTag(String name, Object value, Consumer<Object> body) throws Exception {
		int s = name.lastIndexOf("/");
		int h = name.lastIndexOf("#");
		if(s!=-1 || h!=-1) {
			int l = s>h?s:h;
			name = getTagName(name.substring(0, l+1), name.substring(l+1));
		}
		delimit();
		writer.write('<');
		writer.write(name);
		List<String> removeNamespaces = null;
		Object xmlValue = null;
		boolean foundXmlValue = false;
		if(value!=null) {
			MetaProperty[] props = MetaPropertyLookup.getOrderedGettableProperties(value);
			AtomicInteger pos = new AtomicInteger(0);
			positions.push(pos);
			for(int i=0; i< props.length; i++) {
				MetaProperty mp = props[i];
				XmlAttribute xa = getAnnotation(mp, XmlAttribute.class);
				if(xa!=null) {
					String attName = xa.name();
					Object attValue = transformField(mp, mp.getProperty(value));
					if("##default".equals(attName)) {
						attName = mp.getName();
					}
					attName = getTagName(xa.namespace(), attName);
					inAttribute = true;
					handleField(attName, attValue);
					inAttribute = false;
				}
				XmlValue xv = getAnnotation(mp, XmlValue.class);
				if(xv!=null) {
					if(foundXmlValue) {
						log.warning("Found more than one @XmlValue on "+value.getClass().getName());
					}
					else {
						foundXmlValue = true;
						xmlValue = transformField(mp, mp.getProperty(value));
					}
				}
				XmlElement xe = getAnnotation(mp, XmlElement.class);
				if(xe!=null && !"##default".equals(xe.namespace())){
					getNamespacePrefix(xe.namespace());
				}
				XmlElementWrapper xew = getAnnotation(mp, XmlElementWrapper.class);
				if(xew!=null &&  !"##default".equals(xew.namespace())){
					getNamespacePrefix(xew.namespace());
				}
			}
			positions.pop();
		}
		if(declareNamespaces!=null && !declareNamespaces.isEmpty()) {
			removeNamespaces = new ArrayList<>();
			for(Iterator<Entry<String, String>> iter = declareNamespaces.entrySet().iterator(); iter.hasNext();) {
				Entry<String, String> ns = iter.next();
				writer.write(" xmlns:");
				writer.write(ns.getValue());
				writer.write("=\"");
				escape.write(ns.getKey());
				writer.write("\"");
				iter.remove();
				removeNamespaces.add(ns.getKey());
			}
		}
		writer.write('>');
		if(foundXmlValue) {
			value = xmlValue;
		}
		if(indent>=0) {
			indent++;
		}
		body.accept(value);
		if(indent>=0) {
			indent--;
		}
		delimit();
		writer.write("</");
		writer.write(name);
		writer.write('>');
		doDelimit=true;
		if(removeNamespaces!=null) {
			for(int i= 0; i<removeNamespaces.size();i++) {
				namespacePrefixes.remove(removeNamespaces.get(i));
			}
		}
	}
	
	public void visitListMember(Object value) throws Exception{
		doDelimit = true;
		delimit();
		Optional<String> oen = listElementNames.peek();
		String en = oen != null && oen.isPresent() ? oen.get() : null;
		boolean writeTag = !(value instanceof Node);
		if(writeTag){
			Optional<Map<Class<?>,String>> otn = listTypedElementNames.peek();
			Map<Class<?>,String> typedNames = otn != null && otn.isPresent() ? otn.get() : null;
			if(typedNames == SKIP_TAG) {
				writeTag = false;
			}
			else {
				if(typedNames!=NO_NAMES && typedNames!=null) {
					if(typedNames.containsKey(value.getClass())) {
						en = typedNames.get(value.getClass());
					}
					else if(typedNames.containsKey(XmlElement.DEFAULT.class)) {
						en = typedNames.get(XmlElement.DEFAULT.class);
					}
				}
				if(en==null) {
					en = getElementName(value);
				}
				listElementNames.push(Optional.empty());
				listTypedElementNames.push(Optional.empty());
				writeTag(en, value, t -> {
					try {
						super.visitListMember(t);
					}
					catch(RuntimeException e) {
						throw e;
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
				listElementNames.pop();
				listTypedElementNames.pop();
			}
		}
		if(!writeTag) {
			super.visitListMember(value);
		}
	}
	
	public static String toXmlString(Object o, String indent) throws Exception{
		CharArrayWriter caw = new CharArrayWriter();
		new ModelXmlWriter(caw, indent).visit(o);
		return caw.toString();
	}
	
	public void escape(CharSequence seq) throws IOException {
		escape.append(seq);
	}
	
	public void print(CharSequence seq) throws IOException {
		writer.append(seq);
	}

	public String getRootElementName() {
		return rootElementName;
	}

	public void setRootElementName(String rootElementName) {
		this.rootElementName = rootElementName;
	}
	
	public void setNamespacePrefixes(Map<String, String> prefixes) {
		if(usedNamespacePrefixs==null) {
			usedNamespacePrefixs = new HashMap<>();
		}
		for(Iterator<Map.Entry<String, String>> iter = prefixes.entrySet().iterator(); iter.hasNext(); ) {
			Map.Entry<String, String> entry = iter.next();
			usedNamespacePrefixs.put(entry.getValue(), entry.getKey());
		}
	}
	
}
