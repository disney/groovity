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

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.Script;
import groovy.transform.Field;
import groovyjarjarasm.asm.MethodVisitor;
import groovyjarjarasm.asm.Opcodes;
import groovyjarjarasm.asm.Type;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassCodeExpressionTransformer;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ArrayExpression;
import org.codehaus.groovy.ast.expr.AttributeExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BitwiseNegationExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ClosureListExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.ElvisOperatorExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ExpressionTransformer;
import org.codehaus.groovy.ast.expr.FieldExpression;
import org.codehaus.groovy.ast.expr.GStringExpression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.MethodPointerExpression;
import org.codehaus.groovy.ast.expr.NotExpression;
import org.codehaus.groovy.ast.expr.PostfixExpression;
import org.codehaus.groovy.ast.expr.PrefixExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.RangeExpression;
import org.codehaus.groovy.ast.expr.SpreadExpression;
import org.codehaus.groovy.ast.expr.SpreadMapExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.TernaryExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.UnaryMinusExpression;
import org.codehaus.groovy.ast.expr.UnaryPlusExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.AssertStatement;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.BreakStatement;
import org.codehaus.groovy.ast.stmt.CaseStatement;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.ast.stmt.ContinueStatement;
import org.codehaus.groovy.ast.stmt.DoWhileStatement;
import org.codehaus.groovy.ast.stmt.EmptyStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.SwitchStatement;
import org.codehaus.groovy.ast.stmt.SynchronizedStatement;
import org.codehaus.groovy.ast.stmt.ThrowStatement;
import org.codehaus.groovy.ast.stmt.TryCatchStatement;
import org.codehaus.groovy.ast.stmt.WhileStatement;
import org.codehaus.groovy.classgen.BytecodeExpression;
import org.codehaus.groovy.classgen.asm.BytecodeHelper;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.codehaus.groovy.transform.sc.StaticCompilationMetadataKeys;

import com.disney.groovity.Groovity;
import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Loadable;
import com.disney.groovity.ScriptBody;
import com.disney.groovity.Taggable;
import com.disney.groovity.Taggables;
import com.disney.groovity.doc.Arg;
import com.disney.groovity.util.ScriptHelper;
/**
 * Perform AST transformations on groovity sources including adding marker APIs, fixing line numbers, instrumenting
 * statistics, adding built-in load, run, stream and tag functions, and wiring missing property support to expose the binding
 * in static contexts and inner classes
 *
 * @author Alex Vigdor
 */
@GroovyASTTransformation(phase = CompilePhase.CONVERSION)
public class GroovityASTTransformation implements ASTTransformation, Opcodes, GroovityConstants {
	private static List<String> IGNORE_TAGS = Arrays.asList("if","else","unless","each","while");
	private static Log log = LogFactory.getLog(GroovityASTTransformation.class);
	private Map<Integer,Integer> sourceLineNumbers;
	private Collection<String> initDependencies;
	Groovity factory;

	public GroovityASTTransformation(Groovity factory, Map<Integer,Integer> lineNumbers, Collection<String> initDependencies){
		this.sourceLineNumbers=lineNumbers;
		this.factory=factory;
		this.initDependencies=initDependencies;
	}

	public void visit(ASTNode[] nodes, final SourceUnit sourceUnit) {
		ModuleNode mn = sourceUnit.getAST();
		try{
			if(mn!=null){
				ClassNode scriptClassNode = mn.getScriptClassDummy();
				LoadFieldVisitor loadFieldVisitor = new LoadFieldVisitor(sourceUnit);
				loadFieldVisitor.visitClass(scriptClassNode);
				if(mn.getStatementBlock().isEmpty()){
					//System.out.println("Adding dummy statement to force script");
					mn.getStatementBlock().addStatement(new ExpressionStatement(
							new DeclarationExpression(
									new VariableExpression("___groovy__run__stub___"),
									Token.newSymbol(Types.EQUAL, 1, 1), 
									new ConstantExpression(null)
									)
							));
				}
				else{
					//check whether script body really does anything, if so add ScriptBody marker API
					final AtomicBoolean runnable = new AtomicBoolean(false);
					mn.getStatementBlock().visit(new CodeVisitorSupport() {
						public void visitExpressionStatement(ExpressionStatement statement){
							Expression expression = statement.getExpression();
							if(expression instanceof DeclarationExpression){
								List<AnnotationNode> fa = expression.getAnnotations();
								for(AnnotationNode n: fa){
									if("Field".equals(n.getClassNode().getName()) || Field.class.equals(n.getClassNode().getTypeClass())){
										//short-circuit and ignore the field declaration
										return;
									}
								}
								DeclarationExpression de = (DeclarationExpression) expression;
								if((de.getVariableExpression().getModifiers() & ACC_STATIC) !=0) {
									//static declarations are implied fields
									return;
								}
							}
							runnable.set(true);
						}
						public void visitReturnStatement(ReturnStatement statement){
							runnable.set(true);
						}
						public void visitAssertStatement(AssertStatement statement){
							runnable.set(true);
						}
					});
					if(runnable.get()){
						scriptClassNode.addInterface(new ClassNode(ScriptBody.class));
					}
				}
				LoadFinder loadFinder = new LoadFinder(sourceUnit);
				loadFinder.visitClass(scriptClassNode);
				if(loadFinder.loadMethod!=null){
					scriptClassNode.addInterface(new ClassNode(Loadable.class));
				}
				
				LineNumberVisitor lineNumberVisitor = null;
				if(sourceLineNumbers!=null){
					lineNumberVisitor = new LineNumberVisitor(sourceUnit);
				}
				boolean isLibrary = false;
				ArgVisitor argVisitor = new ArgVisitor(sourceUnit);
				InitDependencyVisitor initVisitor = new InitDependencyVisitor(sourceUnit);
				TagFinder tagFinder = new TagFinder(sourceUnit);
				TagCallFinder tagCallFinder = new TagCallFinder(sourceUnit, scriptClassNode, factory.getTaggables());
				StaticBindingTransformer staticBindingTransformer = new StaticBindingTransformer(sourceUnit);
				StaticFieldVisitor staticFieldVisitor = new StaticFieldVisitor(sourceUnit);
				staticFieldVisitor.visitClass(scriptClassNode);
				
				List<MethodNode> methods = mn.getMethods();
				if(methods!=null){
					for(MethodNode method: methods){
						boolean isFunction = false;
						for (AnnotationNode annotation : method.getAnnotations()) {
							if("Function".equals(annotation.getClassNode().getName())){
								//If this script contains at least one Function declaration, mark it as a library
								isFunction=true;
								break;
							}
						}
						if(isFunction){
							isLibrary=true;
							break;
						}
					}
				}
				List<ClassNode> cnodes = mn.getClasses();
				iterateClassNodes:
				for(final ClassNode cn: cnodes){
					//remap GSP line numbers so they match up with original source
					if(lineNumberVisitor!=null){
						lineNumberVisitor.visitClass(cn);
					}
					tagCallFinder.visitClass(cn);
					//add arg annotations to methods to preserve parameter names
					argVisitor.visitClass(cn);
					if(cn.isInterface()){
						continue;
					}
					//Skip further processing for Traits as they don't handle static things well yet ...
					if(cn.getAnnotations()!=null) {
						for(AnnotationNode anno:cn.getAnnotations()) {
							if(anno.getClassNode().getName().equals("groovy.transform.Trait")) {
								//System.out.println("SKIPPING TRAIT "+cn.getName());
								continue iterateClassNodes;
							}
						}
					}
					staticBindingTransformer.visitClass(cn);
					//add statistics gathering to methods and closures
					initVisitor.visitClass(cn);
					tagFinder.visitClass(cn);
					
					final String internalClassName = BytecodeHelper.getClassInternalName(cn);
					//add static missing property support to all classes
					BytecodeExpression staticGetPropertyMissingExpression = new BytecodeExpression() {
						@Override
						public void visit(MethodVisitor mv) {
							mv.visitFieldInsn(GETSTATIC, internalClassName, GROOVITY_SCRIPT_HELPER_FIELD, BytecodeHelper.getTypeDescription(ScriptHelper.class));
							//BytecodeHelper.visitClassLiteral(mv, cn);
							mv.visitVarInsn(ALOAD, 0);
							mv.visitMethodInsn(INVOKEVIRTUAL, BytecodeHelper.getClassInternalName(ScriptHelper.class), "staticPropertyMissing", BytecodeHelper.getMethodDescriptor(Object.class,new Class[] {String.class}),false);
							mv.visitInsn(ARETURN);
						}
					};
					MethodNode staticGetPropMethod = new MethodNode("$static_propertyMissing", ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, 
							new ClassNode(Object.class), 
							new Parameter[]{new Parameter(ClassHelper.make(String.class), "propertyName")}, 
							new ClassNode[]{}, 
							new BlockStatement(new Statement[] { 
									new ReturnStatement(staticGetPropertyMissingExpression)
							},new VariableScope())
					);
					staticGetPropMethod.setSynthetic(true);
					staticGetPropMethod.putNodeMetaData(StaticCompilationMetadataKeys.STATIC_COMPILE_NODE,true);
					cn.addMethod(staticGetPropMethod);
					//add static missing method support to all classes
					BytecodeExpression staticGetMethodMissingExpression = new BytecodeExpression() {
						@Override
						public void visit(MethodVisitor mv) {
							mv.visitFieldInsn(GETSTATIC, internalClassName, GROOVITY_SCRIPT_HELPER_FIELD, BytecodeHelper.getTypeDescription(ScriptHelper.class));
							//BytecodeHelper.visitClassLiteral(mv, cn);
							mv.visitVarInsn(ALOAD, 0);
							mv.visitVarInsn(ALOAD, 1);
							mv.visitMethodInsn(INVOKEVIRTUAL, BytecodeHelper.getClassInternalName(ScriptHelper.class), "invokeMethod", BytecodeHelper.getMethodDescriptor(Object.class,new Class[] {String.class, Object.class}),false);
							mv.visitInsn(ARETURN);
						}
					};
					MethodNode staticGetMissingMethod = new MethodNode("$static_methodMissing", ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, 
							new ClassNode(Object.class), 
							new Parameter[]{new Parameter(ClassHelper.make(String.class), "methodName"),new Parameter(ClassHelper.make(Object.class), "methodArgs")}, 
							new ClassNode[]{}, 
							new BlockStatement(new Statement[] { 
									new ReturnStatement(staticGetMethodMissingExpression)
							},new VariableScope())
					);
					staticGetMissingMethod.setSynthetic(true);
					staticGetMissingMethod.putNodeMetaData(StaticCompilationMetadataKeys.STATIC_COMPILE_NODE,true);
					cn.addMethod(staticGetMissingMethod);
					if(!(cn instanceof InnerClassNode) || cn.isStaticClass()){
						FieldNode gsfNode = new FieldNode(GROOVITY_SCRIPT_HELPER_FIELD,ACC_PROTECTED | ACC_STATIC ,new ClassNode(ScriptHelper.class),cn,ConstantExpression.NULL);
						cn.addField(gsfNode);
						//add missing method support to all classes
						BytecodeExpression getMethodMissingExpression = new BytecodeExpression() {
							@Override
							public void visit(MethodVisitor mv) {
								mv.visitFieldInsn(GETSTATIC, internalClassName, GROOVITY_SCRIPT_HELPER_FIELD, BytecodeHelper.getTypeDescription(ScriptHelper.class));
								//BytecodeHelper.visitClassLiteral(mv, cn);
								mv.visitVarInsn(ALOAD, 1);
								mv.visitVarInsn(ALOAD, 2);
								mv.visitMethodInsn(INVOKEVIRTUAL, BytecodeHelper.getClassInternalName(ScriptHelper.class), "invokeMethod", BytecodeHelper.getMethodDescriptor(Object.class,new Class[] {String.class, Object.class}),false);
								mv.visitInsn(ARETURN);
							}
						};
						MethodNode getMissingMethod = new MethodNode("methodMissing", ACC_PUBLIC | ACC_SYNTHETIC, 
								new ClassNode(Object.class), 
								new Parameter[]{new Parameter(ClassHelper.make(String.class), "methodName"),new Parameter(ClassHelper.make(Object.class), "methodArgs")}, 
								new ClassNode[]{new ClassNode(Exception.class)}, 
								new BlockStatement(new Statement[] { 
										new ReturnStatement(getMethodMissingExpression)
								},new VariableScope())
						);
						getMissingMethod.setSynthetic(true);
						getMissingMethod.putNodeMetaData(StaticCompilationMetadataKeys.STATIC_COMPILE_NODE,true);
						cn.addMethod(getMissingMethod);	
						
						//add missing property lookup to top-level classes
						BytecodeExpression instanceGetPropertyMissingExpression = new BytecodeExpression() {
							@Override
							public void visit(MethodVisitor mv) {
								mv.visitFieldInsn(GETSTATIC, internalClassName, GROOVITY_SCRIPT_HELPER_FIELD, BytecodeHelper.getTypeDescription(ScriptHelper.class));
								mv.visitVarInsn(ALOAD, 1);
								mv.visitMethodInsn(INVOKEVIRTUAL, BytecodeHelper.getClassInternalName(ScriptHelper.class), "getProperty", BytecodeHelper.getMethodDescriptor(Object.class,new Class[] {String.class}), false);
								mv.visitInsn(ARETURN);
							}
						};
						MethodNode instanceGetMethod = new MethodNode("propertyMissing", ACC_PUBLIC | ACC_SYNTHETIC, 
								new ClassNode(Object.class), 
								new Parameter[]{new Parameter(ClassHelper.make(String.class), "propertyName")}, 
								new ClassNode[]{}, 
								new BlockStatement(new Statement[] { 
										new ReturnStatement(instanceGetPropertyMissingExpression)
								},new VariableScope())
						);
						instanceGetMethod.setSynthetic(true);
						instanceGetMethod.putNodeMetaData(StaticCompilationMetadataKeys.STATIC_COMPILE_NODE,true);
						cn.addMethod(instanceGetMethod);
						
						BytecodeExpression instanceSetPropertyMissingExpression = new BytecodeExpression() {
							@Override
							public void visit(MethodVisitor mv) {
								mv.visitFieldInsn(GETSTATIC, internalClassName, GROOVITY_SCRIPT_HELPER_FIELD, BytecodeHelper.getTypeDescription(ScriptHelper.class));
								mv.visitVarInsn(ALOAD, 1);
								mv.visitVarInsn(ALOAD, 2);
								mv.visitMethodInsn(INVOKEVIRTUAL, BytecodeHelper.getClassInternalName(ScriptHelper.class), "setProperty", Type.getMethodDescriptor(Type.VOID_TYPE,Type.getType(String.class), Type.getType(Object.class)), false);
								mv.visitInsn(RETURN);
							}
						};
						MethodNode setMethod = new MethodNode("propertyMissing", ACC_PUBLIC | ACC_SYNTHETIC, 
								ClassHelper.VOID_TYPE, 
								new Parameter[]{new Parameter(ClassHelper.make(String.class), "propertyName"), new Parameter(new ClassNode(Object.class), "newValue")}, 
								new ClassNode[]{}, 
								new BlockStatement(new Statement[] { 
										new ExpressionStatement(instanceSetPropertyMissingExpression)
								},new VariableScope())
						);
						setMethod.setSynthetic(true);
						setMethod.putNodeMetaData(StaticCompilationMetadataKeys.STATIC_COMPILE_NODE,true);
						cn.addMethod(setMethod);
						if(cn!=scriptClassNode){
							//add getBinding to other classes 
							BytecodeExpression getFactoryCall = new BytecodeExpression() {
								@Override
								public void visit(MethodVisitor mv) {
									mv.visitFieldInsn(GETSTATIC, internalClassName, GROOVITY_SCRIPT_HELPER_FIELD, BytecodeHelper.getTypeDescription(ScriptHelper.class));
									mv.visitMethodInsn(INVOKEVIRTUAL, BytecodeHelper.getClassInternalName(ScriptHelper.class), "getBinding", BytecodeHelper.getMethodDescriptor(Binding.class,new Class[] {}),false);
									mv.visitInsn(ARETURN);
								}
							};
							MethodNode getMethod = new MethodNode("getBinding", ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, 
									new ClassNode(Binding.class), 
									new Parameter[]{}, 
									new ClassNode[]{}, 
									new BlockStatement(new Statement[] { 
											new ReturnStatement(getFactoryCall)
									},new VariableScope())
							);
							getMethod.setSynthetic(true);
							getMethod.putNodeMetaData(StaticCompilationMetadataKeys.STATIC_COMPILE_NODE,true);
							cn.addMethod(getMethod);
						}
						
						//add load, run and tag methods to all top level classes
						BytecodeExpression loadFactoryCall = new BytecodeExpression() {
							@Override
							public void visit(MethodVisitor mv) {
								mv.visitFieldInsn(GETSTATIC, internalClassName, GROOVITY_SCRIPT_HELPER_FIELD, BytecodeHelper.getTypeDescription(ScriptHelper.class));
								mv.visitVarInsn(ALOAD, 0);
								mv.visitMethodInsn(INVOKEVIRTUAL, BytecodeHelper.getClassInternalName(ScriptHelper.class), LOAD, BytecodeHelper.getMethodDescriptor(Script.class,new Class[] {String.class}),false);
								mv.visitInsn(ARETURN);
							}
						};
						MethodNode loadMethod = new MethodNode(LOAD, ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, 
								new ClassNode(Script.class), 
								new Parameter[]{new Parameter(new ClassNode(String.class), "path")}, 
								new ClassNode[]{new ClassNode(InstantiationException.class), new ClassNode(IllegalAccessException.class), new ClassNode(ClassNotFoundException.class)}, 
								new BlockStatement(new Statement[] { 
										new ReturnStatement(loadFactoryCall)
								},new VariableScope())
						);
						loadMethod.setSynthetic(true);
						loadMethod.putNodeMetaData(StaticCompilationMetadataKeys.STATIC_COMPILE_NODE,true);
						cn.addMethod(loadMethod);
						
						BytecodeExpression runFactoryCall = new BytecodeExpression() {
							@Override
							public void visit(MethodVisitor mv) {
								mv.visitFieldInsn(GETSTATIC, internalClassName, GROOVITY_SCRIPT_HELPER_FIELD, BytecodeHelper.getTypeDescription(ScriptHelper.class));
								mv.visitVarInsn(ALOAD, 0);
								mv.visitMethodInsn(INVOKEVIRTUAL, BytecodeHelper.getClassInternalName(ScriptHelper.class), RUN, BytecodeHelper.getMethodDescriptor(Object.class,new Class[] {String.class}),false);
								mv.visitInsn(ARETURN);
							}
						};
						MethodNode runMethod = new MethodNode(RUN, ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, 
								ClassHelper.OBJECT_TYPE, 
								new Parameter[]{new Parameter(new ClassNode(String.class), "path")}, 
								new ClassNode[]{new ClassNode(InstantiationException.class), new ClassNode(IllegalAccessException.class), new ClassNode(ClassNotFoundException.class), new ClassNode(IOException.class)}, 
								new BlockStatement(new Statement[] { 
										new ReturnStatement(runFactoryCall)
								},new VariableScope())
						);
						runMethod.setSynthetic(true);
						runMethod.putNodeMetaData(StaticCompilationMetadataKeys.STATIC_COMPILE_NODE,true);
						cn.addMethod(runMethod);
						
						BytecodeExpression doStreamCall = new BytecodeExpression() {
							@Override
							public void visit(MethodVisitor mv) {
								mv.visitFieldInsn(GETSTATIC, internalClassName, GROOVITY_SCRIPT_HELPER_FIELD, BytecodeHelper.getTypeDescription(ScriptHelper.class));
								mv.visitVarInsn(ALOAD, 0);
								mv.visitMethodInsn(INVOKEVIRTUAL, BytecodeHelper.getClassInternalName(ScriptHelper.class), STREAM, Type.getMethodDescriptor(Type.VOID_TYPE,Type.getType(Object.class)),false);
								mv.visitInsn(RETURN);
							}
						};
						MethodNode doStreamMethod = new MethodNode(STREAM, ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, 
								ClassHelper.VOID_TYPE, 
								new Parameter[]{new Parameter(ClassHelper.make(Object.class), "obj")}, 
								new ClassNode[]{}, 
								new BlockStatement(new Statement[] { 
										new ExpressionStatement(doStreamCall)
								},new VariableScope())
						);
						doStreamMethod.setSynthetic(true);
						doStreamMethod.putNodeMetaData(StaticCompilationMetadataKeys.STATIC_COMPILE_NODE,true);
						cn.addMethod(doStreamMethod);
						
						BytecodeExpression doTagFullFactoryCall = new BytecodeExpression() {
							@Override
							public void visit(MethodVisitor mv) {
								mv.visitFieldInsn(GETSTATIC, internalClassName, GROOVITY_SCRIPT_HELPER_FIELD, BytecodeHelper.getTypeDescription(ScriptHelper.class));
								mv.visitVarInsn(ALOAD, 0);
								mv.visitVarInsn(ALOAD, 1);
								mv.visitVarInsn(ALOAD, 2);
								mv.visitMethodInsn(INVOKEVIRTUAL, BytecodeHelper.getClassInternalName(ScriptHelper.class), TAG, BytecodeHelper.getMethodDescriptor(Object.class,new Class[] {String.class,Map.class,Closure.class}),false);
								mv.visitInsn(ARETURN);
							}
						};
						MethodNode doTagFullMethod = new MethodNode(TAG, ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, 
								ClassHelper.OBJECT_TYPE, 
								new Parameter[]{new Parameter(ClassHelper.make(String.class), "tagName"), new Parameter(new ClassNode(Map.class), "attributes"), new Parameter(new ClassNode(Closure.class), "body")}, 
								new ClassNode[]{new ClassNode(Exception.class)}, 
								new BlockStatement(new Statement[] { 
										new ReturnStatement(doTagFullFactoryCall)
								},new VariableScope())
						);
						doTagFullMethod.setSynthetic(true);
						doTagFullMethod.putNodeMetaData(StaticCompilationMetadataKeys.STATIC_COMPILE_NODE,true);
						cn.addMethod(doTagFullMethod);
						
						BytecodeExpression doTagShortBodyFactoryCall = new BytecodeExpression() {
							@Override
							public void visit(MethodVisitor mv) {
								mv.visitFieldInsn(GETSTATIC, internalClassName, GROOVITY_SCRIPT_HELPER_FIELD, BytecodeHelper.getTypeDescription(ScriptHelper.class));
								mv.visitVarInsn(ALOAD, 0);
								mv.visitVarInsn(ALOAD, 1);
								mv.visitMethodInsn(INVOKEVIRTUAL, BytecodeHelper.getClassInternalName(ScriptHelper.class), TAG, BytecodeHelper.getMethodDescriptor(Object.class,new Class[] {String.class,Closure.class}),false);
								mv.visitInsn(ARETURN);
							}
						};
						MethodNode doTagShortBodyMethod = new MethodNode(TAG, ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, 
								ClassHelper.OBJECT_TYPE, 
								new Parameter[]{new Parameter(ClassHelper.make(String.class), "tagName"), new Parameter(new ClassNode(Closure.class), "body")}, 
								new ClassNode[]{new ClassNode(Exception.class)}, 
								new BlockStatement(new Statement[] { 
										new ReturnStatement(doTagShortBodyFactoryCall)
								},new VariableScope())
						);
						doTagShortBodyMethod.setSynthetic(true);
						doTagShortBodyMethod.putNodeMetaData(StaticCompilationMetadataKeys.STATIC_COMPILE_NODE,true);
						cn.addMethod(doTagShortBodyMethod);

					}
					
				}

				//Record whether the class is a library with @Functions in a static final boolean.
				final FieldNode isLibraryFieldNode = new FieldNode("isGroovyLibrary",ACC_PUBLIC | ACC_STATIC | ACC_FINAL,new ClassNode(Boolean.class),scriptClassNode,new ConstantExpression(isLibrary));
				scriptClassNode.addField(isLibraryFieldNode);
				
				ListExpression dependencyExpression = new ListExpression();
				for(String dep:initDependencies){
					dependencyExpression.addExpression(new ConstantExpression(dep));
				}
				//Store pointers to all dependencies to help control load order
				final FieldNode initDependenciesFieldNode = new FieldNode("initDependencies",ACC_PUBLIC | ACC_STATIC | ACC_FINAL,new ClassNode(new ArrayList<String>().getClass()),scriptClassNode,dependencyExpression);
				scriptClassNode.addField(initDependenciesFieldNode);
			}
		}
		catch(Exception e){
			log.error("Error generating stats AST: ",e);
		}
	}

	
	
	private class LoadFinder extends ClassCodeVisitorSupport{
		final SourceUnit sourceUnit;
		MethodNode loadMethod;

		public LoadFinder(SourceUnit sourceUnit){
			this.sourceUnit=sourceUnit;
		}

		@Override
		protected SourceUnit getSourceUnit() {
			return sourceUnit;
		}
		
		public void visitMethod(MethodNode node){
			if(!node.isStatic() && node.getName().equals(LOAD) && node.getParameters().length==0){
				if(node.getReturnType()==ClassHelper.VOID_TYPE){
					node.setReturnType(ClassHelper.OBJECT_TYPE);
				}
				this.loadMethod=node;
			}
			super.visitMethod(node);
		}
	}

	private class TagFinder extends ClassCodeVisitorSupport{
		final SourceUnit sourceUnit;
		private ClassNode classNode;
		private boolean isTag = false;

		public TagFinder(SourceUnit sourceUnit){
			this.sourceUnit=sourceUnit;
		}

		@Override
		protected SourceUnit getSourceUnit() {
			return sourceUnit;
		}

		public void visitClass(ClassNode classNode){
			this.classNode = classNode;
			isTag=false;
			for(AnnotationNode node: classNode.getAnnotations()){
				if("Tag".equals(node.getClassNode().getName())){
					isTag=true;
				}
			}
			super.visitClass(classNode);
		}

		public void visitMethod(MethodNode node){
			if(isTag && !node.isStatic() && node.getName().equals(TAG) && node.getParameters().length==2 && node.getReturnType().equals(ClassHelper.OBJECT_TYPE)){
				ClassNode taggableNode = new ClassNode(Taggable.class);
				if(!classNode.declaresInterface(taggableNode)){
					classNode.addInterface(taggableNode);
				}
			}
			super.visitMethod(node);
		}
	}
	
	private class TagCallFinder extends ClassCodeVisitorSupport{
		final SourceUnit sourceUnit;
		final Taggables tags;

		public TagCallFinder(SourceUnit sourceUnit, ClassNode scriptClassNode, Taggables tags){
			this.sourceUnit=sourceUnit;
			this.tags=tags;
		}

		@Override
		protected SourceUnit getSourceUnit() {
			return sourceUnit;
		}
		
		private boolean isTagCall(MethodCallExpression ex){
			Expression oe = ex.getObjectExpression();
			if(oe instanceof VariableExpression && ((VariableExpression)oe).isThisExpression()){
				String methodName = ex.getMethodAsString();
				if(tags.hasTag(methodName) && !IGNORE_TAGS.contains(methodName)){
					List<Expression> args = ((TupleExpression)ex.getArguments()).getExpressions();
					if(args.size()>0){
						Expression firstArg = args.get(0);
						if(firstArg.getType().isDerivedFrom(new ClassNode(Map.class))){
							if(args.size()==2){
								Expression secondArg = args.get(1);
								if(secondArg.getType().isDerivedFrom(new ClassNode(Closure.class))){
									//System.out.println("CLOSURE VARIABLE 2 SCOPE IS "+((ClosureExpression)secondArg).getVariableScope());
									return true;
								}
								return false;
							}
							return true;
						}
						else if(firstArg.getType().isDerivedFrom(new ClassNode(Closure.class))){
							//System.out.println("CLOSURE VARIABLE SCOPE 1 IS "+((ClosureExpression)firstArg).getVariableScope());
							return true;
						}
						return false;
					}
					return true;
				}
			}
			return false;
		}
		
		public void visitMethodCallExpression(MethodCallExpression ex){
			if(isTagCall(ex)){
				//System.out.println("FOUND TAG CALL "+ex+" and rewriting ");
				List<Expression> args = ((TupleExpression)ex.getArguments()).getExpressions();
				List<Expression> newArgs= new ArrayList<>();
				newArgs.add(new ConstantExpression(ex.getMethodAsString()));
				newArgs.addAll(args);
				//Automatically add a closure so that scope is available to the tag
				if(newArgs.size()==0 || !(newArgs.get(newArgs.size()-1) instanceof ClosureExpression)){
					newArgs.add(new ClosureExpression(new Parameter[0], new EmptyStatement()));
				}
				ex.setArguments(new TupleExpression(newArgs));
				ex.setMethod(new ConstantExpression(TAG));
			}
			
			super.visitMethodCallExpression(ex);
		}
	}
	
	private class StaticFieldVisitor extends ClassCodeVisitorSupport{
		final SourceUnit sourceUnit;
		boolean inRunMethod = false;
		ArrayDeque<ClosureExpression> context = new ArrayDeque<>();
		
		public StaticFieldVisitor(SourceUnit sourceUnit){
			this.sourceUnit=sourceUnit;
		}

		@Override
		protected SourceUnit getSourceUnit() {
			return sourceUnit;
		}
		
		public void visitMethod(MethodNode node){
			inRunMethod = node.isScriptBody();
			super.visitMethod(node);
			inRunMethod=false;
		}
		public void visitClosureExpression(ClosureExpression expr){
			context.push(expr);
			super.visitClosureExpression(expr);
			context.pop();
		}
		public void visitDeclarationExpression(DeclarationExpression expr){
			if(inRunMethod && !expr.isMultipleAssignmentDeclaration() && context.isEmpty()){
				VariableExpression ve = expr.getVariableExpression();
				if((ve.getModifiers() & ACC_STATIC) !=0){
					boolean isField = false;
					List<AnnotationNode> nodes = expr.getAnnotations();
					if(nodes!=null){
						for(AnnotationNode node:nodes){
							if(node.getClassNode().getName().equals("Field")){
								isField=true;
							}
						}
					}
					//automatically add @Field annotation to static variable declarations
					if(!isField){
						expr.addAnnotation(new AnnotationNode(new ClassNode(Field.class)));
					}
				}
			}
			super.visitDeclarationExpression(expr);
		}
	}
	
	private class LoadFieldVisitor extends ClassCodeVisitorSupport{
		final SourceUnit sourceUnit;
		boolean inRunMethod = false;
		
		public LoadFieldVisitor(SourceUnit sourceUnit){
			this.sourceUnit=sourceUnit;
		}

		@Override
		protected SourceUnit getSourceUnit() {
			return sourceUnit;
		}
		
		public void visitMethod(MethodNode node){
			inRunMethod = node.isScriptBody();
			super.visitMethod(node);
			inRunMethod=false;
		}
		
		public void visitBlockStatement(BlockStatement st){
			if(inRunMethod){
				List<Statement> ss = st.getStatements();
				for(int i=0;i<ss.size();i++){
					Statement s = ss.get(i);
					if(s instanceof ExpressionStatement){
						ExpressionStatement es = (ExpressionStatement) s;
						if(es.getExpression() instanceof MethodCallExpression){
							MethodCallExpression mce = (MethodCallExpression) es.getExpression();
							if(mce.getMethodAsString().equals(LOAD)){
								Expression argEx = mce.getArguments();
								if(argEx!=null && argEx instanceof TupleExpression){
									List<Expression> args = ((TupleExpression)mce.getArguments()).getExpressions();
									if(args.size()>0){
										String library = args.get(0).getText();
										int lp = library.lastIndexOf('/',library.length()-1);
										if(lp>=0){
											library = library.substring(lp+1);
										}
										//convert top-level bare load calls to Field assignments automatically
										if(log.isDebugEnabled()){
											log.debug("CONVERTING load("+args.get(0).getText()+") to @Field "+library);
										}
										VariableExpression libraryVar = new VariableExpression(library);
										libraryVar.setType(new ClassNode(Script.class));
										DeclarationExpression de = new DeclarationExpression(libraryVar, Token.newSymbol(Types.EQUAL, 1, 1), mce);
										de.addAnnotation(new AnnotationNode(new ClassNode(Field.class)));
										ss.set(i,new ExpressionStatement(de));
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	private class InitDependencyVisitor extends ClassCodeVisitorSupport{
		final SourceUnit sourceUnit;

		public InitDependencyVisitor(SourceUnit sourceUnit){
			this.sourceUnit=sourceUnit;
		}

		@Override
		protected SourceUnit getSourceUnit() {
			return sourceUnit;
		}
		
		public void visitMethodCallExpression(MethodCallExpression ex){
			Expression argEx = ex.getArguments();
			if(argEx!=null && argEx instanceof TupleExpression){
				List<Expression> args = ((TupleExpression)ex.getArguments()).getExpressions();
				if(args.size()>0 && (ex.getMethodAsString().equals(LOAD) || ex.getMethodAsString().equals(RUN))){
					String dependency = args.get(args.size()-1).getText();
					//System.out.println("Found init dependency "+dependency);
					initDependencies.add(dependency);
				}
			}
			super.visitMethodCallExpression(ex);
		}
	}


	private class ArgVisitor extends ClassCodeVisitorSupport{
		final SourceUnit sourceUnit;

		public ArgVisitor(SourceUnit sourceUnit){
			this.sourceUnit=sourceUnit;
		}

		@Override
		protected SourceUnit getSourceUnit() {
			return sourceUnit;
		}

		public void visitMethod(MethodNode method){
			super.visitMethod(method);
			Parameter[] parms = method.getParameters();
			for(Parameter parm: parms){
				AnnotationNode argNode = null;
				for (AnnotationNode annotation : parm.getAnnotations()) {
					if("Arg".equals(annotation.getClassNode().getName())){
						argNode=annotation;
						break;
					}
				}
				if(argNode==null){
					//dynamically add annotation so we can retain the parameter name
					AnnotationNode dynamicAnno = new AnnotationNode(new ClassNode(Arg.class));
					dynamicAnno.addMember("name", new ConstantExpression(parm.getName()));
					parm.addAnnotation(dynamicAnno);
				}
				else{
					//fill in name on existing annotation if it is missing
					if(argNode.getMember("name")==null){
						argNode.addMember("name", new ConstantExpression(parm.getName()));
					}
				}
			}
		}
	}

	private class StaticBindingTransformer  extends ClassCodeExpressionTransformer implements ExpressionTransformer {
		final SourceUnit sourceUnit;
		boolean inStatic = false;
		
		public StaticBindingTransformer(SourceUnit sourceUnit){
			this.sourceUnit=sourceUnit;
		}
		@Override
		protected SourceUnit getSourceUnit() {
			return sourceUnit;
		}
		
		
		
		public Expression transform(Expression exp) {
			if(inStatic){
				if(exp instanceof VariableExpression){
					VariableExpression vexp = (VariableExpression) exp;
					if(vexp.getName().equals(BINDING)){
						MethodCallExpression mexp = new MethodCallExpression(new VariableExpression(GROOVITY_SCRIPT_HELPER_FIELD), "getBinding", new TupleExpression());
						return mexp;
					}
				}
				return exp.transformExpression(this);
			}
			return exp;
		}

		
		public void visitMethod(MethodNode node){
			if(node.isStatic()){
				inStatic = true;
			}
			super.visitMethod(node);
			inStatic = false;
		}
		
		
	}

	private class LineNumberVisitor extends ClassCodeVisitorSupport {
		final static String LINE_NUMBERS_FIXED = "LineNumberVisitor.complete";
		final SourceUnit sourceUnit;
		
		private boolean fixLineNumbers(ASTNode node){
			if(node.getNodeMetaData(LINE_NUMBERS_FIXED) != null) {
				return false;
			}
			node.setNodeMetaData(LINE_NUMBERS_FIXED, LINE_NUMBERS_FIXED);
			if(node.getLineNumber()>0){
				Integer nn = sourceLineNumbers.get(node.getLineNumber());
				if(nn!=null){
					node.setLineNumber(nn);
				}
				else{
					node.setLineNumber(-1);
				}
			}
			if(node.getLastLineNumber()>0){
				Integer nn = sourceLineNumbers.get(node.getLastLineNumber());
				if(nn!=null){
					node.setLastLineNumber(nn);
				}
				else{
					node.setLastLineNumber(-1);
				}
			}
			if(node instanceof Expression) {
				ClassNode type = ((Expression)node).getType();
				if(type!=null) {
					visitClass(type);
				}
			}
			return true;
		}
		
		public LineNumberVisitor(SourceUnit sourceUnit){
			this.sourceUnit=sourceUnit;
		}
		@Override
		protected SourceUnit getSourceUnit() {
			return sourceUnit;
		}
		@Override
		public void visitArgumentlistExpression(ArgumentListExpression arg0) {
			fixLineNumbers(arg0);
			super.visitArgumentlistExpression(arg0);
		}
		@Override
		public void visitArrayExpression(ArrayExpression arg0) {
			fixLineNumbers(arg0);
			super.visitArrayExpression(arg0);
		}
		@Override
		public void visitAssertStatement(AssertStatement arg0) {
			fixLineNumbers(arg0);
			super.visitAssertStatement(arg0);
		}
		@Override
		public void visitAttributeExpression(AttributeExpression arg0) {
			fixLineNumbers(arg0);
			super.visitAttributeExpression(arg0);
		}
		@Override
		public void visitBinaryExpression(BinaryExpression arg0) {
			fixLineNumbers(arg0);
			super.visitBinaryExpression(arg0);
		}
		@Override
		public void visitBitwiseNegationExpression(BitwiseNegationExpression arg0) {
			fixLineNumbers(arg0);
			super.visitBitwiseNegationExpression(arg0);
		}
		@Override
		public void visitBlockStatement(BlockStatement arg0) {
			fixLineNumbers(arg0);
			super.visitBlockStatement(arg0);
		}
		@Override
		public void visitBooleanExpression(BooleanExpression arg0) {
			fixLineNumbers(arg0);
			super.visitBooleanExpression(arg0);
		}
		@Override
		public void visitBreakStatement(BreakStatement arg0) {
			fixLineNumbers(arg0);
			super.visitBreakStatement(arg0);
		}
		@Override
		public void visitBytecodeExpression(BytecodeExpression arg0) {
			fixLineNumbers(arg0);
			super.visitBytecodeExpression(arg0);
		}
		@Override
		public void visitCaseStatement(CaseStatement arg0) {
			fixLineNumbers(arg0);
			super.visitCaseStatement(arg0);
		}
		@Override
		public void visitCastExpression(CastExpression arg0) {
			fixLineNumbers(arg0);
			super.visitCastExpression(arg0);
		}
		@Override
		public void visitCatchStatement(CatchStatement arg0) {
			fixLineNumbers(arg0);
			super.visitCatchStatement(arg0);
		}
		@Override
		public void visitClassExpression(ClassExpression arg0) {
			fixLineNumbers(arg0);
			super.visitClassExpression(arg0);
		}
		@Override
		public void visitClosureExpression(ClosureExpression arg0) {
			fixLineNumbers(arg0);
			super.visitClosureExpression(arg0);
		}
		@Override
		public void visitClosureListExpression(ClosureListExpression arg0) {
			fixLineNumbers(arg0);
			super.visitClosureListExpression(arg0);
		}
		@Override
		public void visitConstantExpression(ConstantExpression arg0) {
			fixLineNumbers(arg0);
			super.visitConstantExpression(arg0);
		}
		@Override
		public void visitConstructorCallExpression(ConstructorCallExpression arg0) {
			fixLineNumbers(arg0);
			super.visitConstructorCallExpression(arg0);
		}
		@Override
		public void visitContinueStatement(ContinueStatement arg0) {
			fixLineNumbers(arg0);
			super.visitContinueStatement(arg0);
		}
		@Override
		public void visitDeclarationExpression(DeclarationExpression arg0) {
			fixLineNumbers(arg0);
			super.visitDeclarationExpression(arg0);
		}
		@Override
		public void visitDoWhileLoop(DoWhileStatement arg0) {
			fixLineNumbers(arg0);
			super.visitDoWhileLoop(arg0);
		}
		@Override
		public void visitExpressionStatement(ExpressionStatement arg0) {
			fixLineNumbers(arg0);
			super.visitExpressionStatement(arg0);
		}
		@Override
		public void visitFieldExpression(FieldExpression arg0) {
			fixLineNumbers(arg0);
			super.visitFieldExpression(arg0);
		}
		@Override
		public void visitForLoop(ForStatement arg0) {
			fixLineNumbers(arg0);
			super.visitForLoop(arg0);
		}
		@Override
		public void visitGStringExpression(GStringExpression arg0) {
			fixLineNumbers(arg0);
			super.visitGStringExpression(arg0);
		}
		@Override
		public void visitIfElse(IfStatement arg0) {
			fixLineNumbers(arg0);
			super.visitIfElse(arg0);
		}
		@Override
		public void visitListExpression(ListExpression arg0) {
			fixLineNumbers(arg0);
			super.visitListExpression(arg0);
		}
		@Override
		public void visitMapEntryExpression(MapEntryExpression arg0) {
			fixLineNumbers(arg0);
			super.visitMapEntryExpression(arg0);
		}
		@Override
		public void visitMapExpression(MapExpression arg0) {
			fixLineNumbers(arg0);
			super.visitMapExpression(arg0);
		}
		@Override
		public void visitMethodCallExpression(MethodCallExpression arg0) {
			fixLineNumbers(arg0);
			super.visitMethodCallExpression(arg0);
		}
		@Override
		public void visitMethodPointerExpression(MethodPointerExpression arg0) {
			fixLineNumbers(arg0);
			super.visitMethodPointerExpression(arg0);
		}
		@Override
		public void visitNotExpression(NotExpression arg0) {
			fixLineNumbers(arg0);
			super.visitNotExpression(arg0);
		}
		@Override
		public void visitPostfixExpression(PostfixExpression arg0) {
			fixLineNumbers(arg0);
			super.visitPostfixExpression(arg0);
		}
		@Override
		public void visitPrefixExpression(PrefixExpression arg0) {
			fixLineNumbers(arg0);
			super.visitPrefixExpression(arg0);
		}
		@Override
		public void visitPropertyExpression(PropertyExpression arg0) {
			fixLineNumbers(arg0);
			super.visitPropertyExpression(arg0);
		}
		@Override
		public void visitRangeExpression(RangeExpression arg0) {
			fixLineNumbers(arg0);
			super.visitRangeExpression(arg0);
		}
		@Override
		public void visitReturnStatement(ReturnStatement arg0) {
			fixLineNumbers(arg0);
			super.visitReturnStatement(arg0);
		}
		@Override
		public void visitShortTernaryExpression(ElvisOperatorExpression arg0) {
			fixLineNumbers(arg0);
			super.visitShortTernaryExpression(arg0);
		}
		@Override
		public void visitSpreadExpression(SpreadExpression arg0) {
			fixLineNumbers(arg0);
			super.visitSpreadExpression(arg0);
		}
		@Override
		public void visitSpreadMapExpression(SpreadMapExpression arg0) {
			fixLineNumbers(arg0);
			super.visitSpreadMapExpression(arg0);
		}
		@Override
		public void visitStaticMethodCallExpression(StaticMethodCallExpression arg0) {
			fixLineNumbers(arg0);
			super.visitStaticMethodCallExpression(arg0);
		}
		@Override
		public void visitSwitch(SwitchStatement arg0) {
			fixLineNumbers(arg0);
			super.visitSwitch(arg0);
		}
		@Override
		public void visitSynchronizedStatement(SynchronizedStatement arg0) {
			fixLineNumbers(arg0);
			super.visitSynchronizedStatement(arg0);
		}
		@Override
		public void visitTernaryExpression(TernaryExpression arg0) {
			fixLineNumbers(arg0);
			super.visitTernaryExpression(arg0);
		}
		@Override
		public void visitThrowStatement(ThrowStatement arg0) {
			fixLineNumbers(arg0);
			super.visitThrowStatement(arg0);
		}
		@Override
		public void visitTryCatchFinally(TryCatchStatement arg0) {
			fixLineNumbers(arg0);
			super.visitTryCatchFinally(arg0);
		}
		@Override
		public void visitTupleExpression(TupleExpression arg0) {
			fixLineNumbers(arg0);
			super.visitTupleExpression(arg0);
		}
		@Override
		public void visitUnaryMinusExpression(UnaryMinusExpression arg0) {
			fixLineNumbers(arg0);
			super.visitUnaryMinusExpression(arg0);
		}
		@Override
		public void visitUnaryPlusExpression(UnaryPlusExpression arg0) {
			fixLineNumbers(arg0);
			super.visitUnaryPlusExpression(arg0);
		}
		@Override
		public void visitVariableExpression(VariableExpression arg0) {
			fixLineNumbers(arg0);
			super.visitVariableExpression(arg0);
		}
		@Override
		public void visitWhileLoop(WhileStatement arg0) {
			fixLineNumbers(arg0);
			super.visitWhileLoop(arg0);
		}
		@Override
		public void visitClass(ClassNode arg0) {
			if(fixLineNumbers(arg0)) {
				ClassNode ct = arg0.getComponentType();
				if(ct!=null) {
					visitClass(ct);
				}
				GenericsType[] generics = arg0.getGenericsTypes();
				if(generics!=null) {
					for(GenericsType gt: generics) {
						fixLineNumbers(gt);
						ClassNode g = gt.getType();
						if(g!=null) {
							visitClass(g);
						}
						ClassNode l = gt.getLowerBound();
						if(l!=null) {
							visitClass(l);
						}
						ClassNode[] ts = gt.getUpperBounds();
						if(ts!=null) {
							for(ClassNode t: ts) {
								visitClass(t);
							}
						}
					}
				}
				super.visitClass(arg0);
			}
		}
		@Override
		public void visitField(FieldNode arg0) {
			if(arg0.getType()!=null) {
				visitClass(arg0.getType());
			}
			super.visitField(arg0);
		}
		@Override
		public void visitProperty(PropertyNode arg0) {
			if(arg0.getType()!=null) {
				visitClass(arg0.getType());
			}
			super.visitProperty(arg0);
		}
		@Override
		protected void visitConstructorOrMethod(MethodNode node, boolean isConstructor) {
			for (Parameter param : node.getParameters()) {
				if(param.getInitialExpression()!=null) {
					param.getInitialExpression().visit(this);
				}
				if(param.getType()!=null) {
					visitClass(param.getType());
				}
			}
			ClassNode cn = node.getReturnType();
			if(cn!=null) {
				visitClass(cn);
			}
			super.visitConstructorOrMethod(node, isConstructor);
		}
		public void visitAnnotations(AnnotatedNode node) {
			fixLineNumbers(node);
			List<AnnotationNode> annotations = node.getAnnotations();
			for(AnnotationNode an: annotations) {
				fixLineNumbers(an);
			}
			super.visitAnnotations(node);
		}
	}
}
