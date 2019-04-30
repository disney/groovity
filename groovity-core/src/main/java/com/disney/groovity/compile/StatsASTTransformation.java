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
package com.disney.groovity.compile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.TryCatchStatement;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.classgen.asm.BytecodeHelper;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import com.disney.groovity.GroovityConstants;
import com.disney.groovity.stats.GroovityStatistics;
import com.disney.groovity.util.ClosureWritable;
import com.disney.groovity.util.ScriptHelper;

import groovy.lang.Script;
import groovyjarjarasm.asm.MethodVisitor;
import groovyjarjarasm.asm.Opcodes;
import groovyjarjarasm.asm.Type;
/**
 * Appy stats gathering bytecode in the canonicalization phase, after traits have been copied so we can instrument them in place.
 *
 * @author Alex Vigdor
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class StatsASTTransformation implements ASTTransformation, Opcodes, GroovityConstants {
	Map<String, ClassNode> traits;

	@Override
	public void visit(ASTNode[] nodes, SourceUnit sourceUnit) {
		StatisticsVisitor statsVisitor = new StatisticsVisitor(sourceUnit);
		ModuleNode mn = sourceUnit.getAST();
		List<ClassNode> cnodes = mn.getClasses();
		//gather up traits to match up with helpers
		traits = new HashMap<>();
		for(final ClassNode cn: cnodes){
			if(cn.isInterface() && isTrait(cn, false)) {
				traits.put(cn.getName(), cn);
			}
		}
		//process trait helpers first
		statsVisitor.inTrait = true;
		for(final ClassNode cn: cnodes){
			if(cn.isInterface()) {
				continue;
			}
			if(isTrait(cn, true)) {
				statsVisitor.visitClass(cn);
			}
		}
		statsVisitor.inTrait = false;
		//regular classes last:
		for(final ClassNode cn: cnodes){
			if(cn.isInterface()) {
				continue;
			}
			if(!isTrait(cn, true)) {
				statsVisitor.visitClass(cn);
			}
		}
	}
	
	protected boolean isTrait(ClassNode checkNode, boolean recurse) {
		while(checkNode !=null) {
			if(checkNode.getAnnotations()!=null) {
				for(AnnotationNode anno:checkNode.getAnnotations()) {
					if(anno.getClassNode().getName().equals("groovy.transform.Trait")) {
						//continue iterateClassNodes;
						return true;
					}
				}
			}
			if(!recurse) {
				return false;
			}
			checkNode = checkNode.getOuterClass()!=checkNode ? checkNode.getOuterClass() : null;
		}
		return false;
	}
	
	protected String simpleName(String name) {
		int i = name.lastIndexOf(".");
		if(i>0) {
			return name.substring(i+1);
		}
		return name;
	}
	
	private class StatisticsVisitor extends ClassCodeVisitorSupport{
		final SourceUnit sourceUnit;
		ClassNode classNode;
		String classNodeName;
		boolean closureStats=false;
		String traitName;

		boolean inTrait = false;
		Set<MethodSignature> apiSkipStats;

		public StatisticsVisitor(SourceUnit sourceUnit){
			this.sourceUnit=sourceUnit;
		}
		@Override
		protected SourceUnit getSourceUnit() {
			return sourceUnit;
		}

		public void visitConstructorCallExpression(ConstructorCallExpression ale) {
			if(ale.getType().getName().equals(ClosureWritable.class.getName())) {
				closureStats=true;
			}
			super.visitConstructorCallExpression(ale);
			closureStats=false;
		}
		
		protected String getClassLabel(ClassNode classNode){
			String name = classNode.getName();
			if(inTrait) {
				int d = name.indexOf("$");
				if(d>0) {
					name = name.substring(0,d);
				}
				traitName = name;
			}
			if(!sourceUnit.getName().startsWith(name)){
				name = sourceUnit.getName().substring(0, sourceUnit.getName().lastIndexOf("."))+"->"+name;
			}
			StringBuilder builder = new StringBuilder();
			String[] parts = name.split("___",-1);
			for(int i=0;i<parts.length;i++){
				if(i>0){
					builder.append("_");
				}
				builder.append(parts[i].replaceAll("_", "/"));
			}
			return builder.toString();
		}
		
		private void inspectInterface(ClassNode cn, Set<ClassNode> crawled) {
			List<MethodNode> apiMethods = cn.getAllDeclaredMethods();
			for(MethodNode apiMethod: apiMethods) {
				List<AnnotationNode> skipNodes = apiMethod.getAnnotations(new ClassNode(SkipStatistics.class));
				if(skipNodes !=null && !skipNodes.isEmpty()) {
					MethodSignature ms = new MethodSignature(apiMethod, false);
					apiSkipStats.add(ms);
				}
			}
			if(!crawled.contains(cn)) {
				crawlParents(cn, crawled);
			}
		}
		
		private void crawlParents(ClassNode classNode, Set<ClassNode> crawled) {
			crawled.add(classNode);
			Set<ClassNode> apis = classNode.getAllInterfaces();
			if(apis!=null) {
				for(ClassNode cn: apis) {
					inspectInterface(cn, crawled);
				}
			}
			if(inTrait) {
				String cname = classNode.getName();
				int d = cname.indexOf("$");
				if(d>0) {
					cname = cname.substring(0,d);
				}
				ClassNode trait = traits.get(cname);
				if(trait!=null) {
					inspectInterface(trait, crawled);
				}
			}
		}
		
		public void visitClass(ClassNode classNode){
			this.classNode = classNode;
			traitName = null;
			classNodeName = getClassLabel(classNode);
			apiSkipStats = new HashSet<>();
			crawlParents(classNode, new HashSet<>());
			visitAnnotations(classNode);
			visitPackage(classNode.getPackage());
			visitImports(classNode.getModule());
			
			for (MethodNode mn : classNode.getMethods()) {
				visitMethod(mn);
	        }
			
			for (ConstructorNode cn : classNode.getDeclaredConstructors()) {
	            visitConstructor(cn);
	        }
			
			for (PropertyNode pn : classNode.getProperties()) {
	            visitProperty(pn);
	        }
			
			//for (FieldNode fn : classNode.getFields()) {
	        //    visitField(fn);
	        //}
			
			visitObjectInitializerStatements(classNode);
		}

		public void visitMethod(final MethodNode method){
			super.visitMethod(method);
			if(!method.isStaticConstructor() && !method.isSynthetic() && !method.getName().contains("$") && !method.isAbstract()){
				final boolean isScriptRunMethod = method.getName().equals(RUN) && method.getParameters().length==0 && method.isScriptBody();
				if(!isScriptRunMethod) {
					List<AnnotationNode> annotations = method.getAnnotations(new ClassNode(SkipStatistics.class));
					if(annotations!=null && !annotations.isEmpty()) {
						//this method is marked for skip
						return;
					}
					annotations = method.getAnnotations(new ClassNode(GatherStatistics.class));
					if(annotations!=null && !annotations.isEmpty()) {
						//this method has already been instrumented, perhaps in a trait
						for(Iterator<AnnotationNode> i = method.getAnnotations().iterator(); i.hasNext();) {
							AnnotationNode an = i.next();
							if(an.getClassNode().getTypeClass().equals(GatherStatistics.class)) {
								i.remove();
							}
						}
						return;
					}
					if(method.getParameters().length==0 && method.getName().equals("hashCode")) {
						return;
					}
					if(method.getParameters().length==1 && method.getName().equals("equals")) {
						return;
					}
					Statement st = method.getCode();
					if(method.isStatic() && method.getName().equals("main") && method.getParameters().length==1) {
						if(st instanceof ExpressionStatement) {
							Expression ex = ((ExpressionStatement)st).getExpression();
							if(ex instanceof MethodCallExpression) {
								if("runScript".equals(((MethodCallExpression)ex).getMethodAsString())) {
									return;
								}
							}
						}
					}
					if(inTrait) {
						if(st instanceof ExpressionStatement) {
							Expression ex = ((ExpressionStatement)st).getExpression();
							if(ex instanceof MethodCallExpression) {
								String methodCall = ((MethodCallExpression)ex).getMethodAsString();
								if((methodCall.endsWith("$get") || methodCall.endsWith("$set")) &&
										(method.getName().startsWith("get") || method.getName().startsWith("set") || method.getName().startsWith("is"))) {
									//let's not instrument simple getters/setters in the interest of performance
									//mark it skip so descendants don't instrument
									method.addAnnotation(new AnnotationNode(new ClassNode(SkipStatistics.class)));
									if(traitName!=null) {
										ClassNode traitAPI = traits.get(traitName);
										MethodNode apiMethod = traitAPI.getMethod(method.getName(), Arrays.copyOfRange(method.getParameters(),1,method.getParameters().length));
										if(apiMethod!=null) {
											apiMethod.addAnnotation(new AnnotationNode(new ClassNode(SkipStatistics.class)));
										}
									}
									return;
								}
							}
						}
					}
					//mask simple setters from stats
					if(method.getName().startsWith("set") && method.getParameters().length==1) {
						if(st instanceof BlockStatement) {
							BlockStatement bs = (BlockStatement) st;
							if(bs.getStatements().size() == 1) {
								Statement bst = bs.getStatements().get(0);
								if(bst instanceof ExpressionStatement) {
									Expression ex = ((ExpressionStatement)bst).getExpression();
									if(ex instanceof BinaryExpression) {
										BinaryExpression be = (BinaryExpression) ex;
										Expression l = be.getLeftExpression();
										Expression r = be.getRightExpression();
										if(l instanceof PropertyExpression && r instanceof VariableExpression) {
											return;
										}
									}
								}
							}
						}
					}
					MethodSignature ms = new MethodSignature(method, inTrait);
					if(apiSkipStats.contains(ms)) {
						//System.out.println("Method implements api that is marked for SkipStatistics "+ms);
						return;
					}
					//we made it this far, mark for stats gathering
					method.addAnnotation(new AnnotationNode(new ClassNode(GatherStatistics.class)));
				}
				//System.out.println("Visit method "+method.getName()+" "+method.getVariableScope());
				//final String methodCallName = "__method_call__"+(methodNum++);
				StringBuilder methodCallLabelBuilder = new StringBuilder(classNodeName).append(".").append(method.getName()).append("(");
				String delimiter = "";
				Parameter[] params = method.getParameters();
				for(int i=0; i< params.length; i++) {
					Parameter p = params[i];
					ClassNode pType = p.getType();
					if(inTrait && i==0 ) {
						if(classNode.getUnresolvedName().startsWith(pType.getUnresolvedName()+"$Trait")) {
							//mask the first parameter in trait methods since it is a reference to the API
							continue;
						}
					}
					String pLabel = simpleName(pType.isArray() ? pType.getComponentType().getUnresolvedName()+"[]" : pType.getUnresolvedName());
					methodCallLabelBuilder.append(delimiter).append(pLabel);
					GenericsType[] gts = p.getType().getGenericsTypes();
					if(gts!=null && gts.length>0) {
						methodCallLabelBuilder.append("<");
						String gDelim = "";
						for(GenericsType gt: gts) {
							methodCallLabelBuilder.append(gDelim).append(simpleName(gt.getType().getUnresolvedName()));
							gDelim=", ";
						}
						methodCallLabelBuilder.append(">");
					}
					delimiter=", ";
				}
				methodCallLabelBuilder.append(")");
				final String methodCallLabel = methodCallLabelBuilder.toString();
				//System.out.println("Processing method call "+methodCallLabel);
				final String classNodeDesc = BytecodeHelper.getClassInternalName(classNode);
				Statement startStats = new BytecodeSequence(new BytecodeInstruction() {
					@Override
					public void visit(MethodVisitor mv) {
						if(isScriptRunMethod){
							mv.visitFieldInsn(GETSTATIC, classNodeDesc, GROOVITY_SCRIPT_HELPER_FIELD, BytecodeHelper.getTypeDescription(ScriptHelper.class));
							mv.visitVarInsn(ALOAD, 0);
							mv.visitMethodInsn(INVOKEVIRTUAL, BytecodeHelper.getClassInternalName(ScriptHelper.class), "startRun", Type.getMethodDescriptor(Type.VOID_TYPE,Type.getType(Script.class)),false);				
						}
						mv.visitLdcInsn(methodCallLabel);
						mv.visitMethodInsn(INVOKESTATIC,BytecodeHelper.getClassInternalName(GroovityStatistics.class),"startExecution",Type.getMethodDescriptor(Type.VOID_TYPE,Type.getType(Object.class)),false);
					}
				});
				Statement endStats = new BytecodeSequence(new BytecodeInstruction() {
					@Override
					public void visit(MethodVisitor mv) {
						mv.visitMethodInsn(INVOKESTATIC,BytecodeHelper.getClassInternalName(GroovityStatistics.class),"endExecution",Type.getMethodDescriptor(Type.VOID_TYPE),false);
						if(isScriptRunMethod){
							mv.visitFieldInsn(GETSTATIC, classNodeDesc, GROOVITY_SCRIPT_HELPER_FIELD, BytecodeHelper.getTypeDescription(ScriptHelper.class));
							mv.visitVarInsn(ALOAD, 0);
							mv.visitMethodInsn(INVOKEVIRTUAL, BytecodeHelper.getClassInternalName(ScriptHelper.class), "endRun", Type.getMethodDescriptor(Type.VOID_TYPE,Type.getType(Script.class)),false);				
						}
					}
				});
				Statement code = method.getCode();
				if(method.getReturnType()!=null && method.getReturnType()!=ClassHelper.VOID_TYPE){
					code = createImplicitReturn(code);
				}
				method.setCode(new BlockStatement(new Statement[] {startStats,new TryCatchStatement(code,endStats)},method.getVariableScope()));
			}
		}
		
		public void visitClosureExpression(final ClosureExpression ex){
			if(closureStats) {
				closureStats=false;
				//System.out.println("Visiting closure expression "+sourceUnit.getName()+" lines "+ex.getLineNumber()+"-"+ex.getLastLineNumber()+" "+ex.getVariableScope());
				//final String closureCallName = "__closure_call__"+(closureNum++);
				StringBuilder closureLabelBuilder = new StringBuilder(classNodeName);
				closureLabelBuilder.append(".{").append(ex.getLineNumber());
				if(ex.getLastLineNumber() > ex.getLineNumber()) {
					closureLabelBuilder.append("-").append(ex.getLastLineNumber());
				}
				closureLabelBuilder.append("}");
				final String closureLabel = closureLabelBuilder.toString();
				//System.out.println("Processing closure "+closureLabelBuilder);
				Statement startStats = new BytecodeSequence(new BytecodeInstruction() {
					@Override
					public void visit(MethodVisitor mv) {
						mv.visitLdcInsn(closureLabel);
						mv.visitMethodInsn(INVOKESTATIC,BytecodeHelper.getClassInternalName(GroovityStatistics.class),"startExecution",Type.getMethodDescriptor(Type.VOID_TYPE,Type.getType(Object.class)),false);
					}
				});
				Statement endStats = new BytecodeSequence(new BytecodeInstruction() {
					@Override
					public void visit(MethodVisitor mv) {
						mv.visitMethodInsn(INVOKESTATIC,BytecodeHelper.getClassInternalName(GroovityStatistics.class),"endExecution",Type.getMethodDescriptor(Type.VOID_TYPE),false);
					}
				});
				Statement code = ex.getCode();
				code = createImplicitReturn(code);
				ex.setCode(new BlockStatement(new Statement[] {startStats,new TryCatchStatement(code,endStats)},ex.getVariableScope()));
			}
			super.visitClosureExpression(ex);
		}
	}
	
	private Statement createImplicitReturn(Statement st){
		if(st instanceof ExpressionStatement) {
			return new ReturnStatement(((ExpressionStatement)st).getExpression());
		}
		if(st instanceof BlockStatement){
			List<Statement> ls =  ((BlockStatement)st).getStatements();
			int pos = ls.size()-1;
			if(pos>=0){
				Statement li;
				if(((li=ls.get(pos)) instanceof ExpressionStatement)){
					//convert to return, groovy will miss this since we are wrapping in try/catch
					ls.set(pos, new ReturnStatement(((ExpressionStatement)li).getExpression()));
				}
			}
		}
		return st;
	}
	
	private class MethodSignature{
		String name;
		String[] params;
		
		private MethodSignature(MethodNode mn, boolean inTrait) {
			name = mn.getName();
			Parameter[] mps = mn.getParameters();
			int offset = inTrait?1:0;
			params = new String[mps.length-offset];
			for(int i=offset; i<mps.length;i++) {
				Parameter p = mps[i];
				params[i-offset] = simpleName(p.getOriginType().getUnresolvedName());
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + Arrays.hashCode(params);
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
			MethodSignature other = (MethodSignature) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (!Arrays.equals(params, other.params))
				return false;
			return true;
		}

		private StatsASTTransformation getOuterType() {
			return StatsASTTransformation.this;
		}

		@Override
		public String toString() {
			return "MethodSignature [name=" + name + ", params=" + Arrays.toString(params) + "]";
		}
	}
}
