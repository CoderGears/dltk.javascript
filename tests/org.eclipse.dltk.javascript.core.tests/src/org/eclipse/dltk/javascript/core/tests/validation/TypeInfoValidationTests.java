/*******************************************************************************
 * Copyright (c) 2010 xored software, Inc.  
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html  
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.javascript.core.tests.validation;

import java.util.List;

import org.eclipse.dltk.compiler.problem.IProblem;
import org.eclipse.dltk.core.builder.IBuildParticipant;
import org.eclipse.dltk.core.tests.util.StringList;
import org.eclipse.dltk.internal.javascript.validation.TypeInfoValidator;
import org.eclipse.dltk.javascript.core.JavaScriptProblems;

@SuppressWarnings("nls")
public class TypeInfoValidationTests extends AbstractValidationTest {

	@Override
	protected IBuildParticipant createValidator() {
		return new TypeInfoValidator();
	}

	public void testKnownType() {
		final List<IProblem> problems = validate("var x:String");
		assertTrue(problems.isEmpty());
	}

	public void testUnknownType() {
		final List<IProblem> problems = validate("var x:LongString");
		assertEquals(problems.toString(), 1, problems.size());
		assertEquals(JavaScriptProblems.UNKNOWN_TYPE, problems.get(0).getID());
	}

	public void testUnknownFunctionType() {
		StringList code = new StringList();
		code.add("/** @type LongString */");
		code.add("function x(){}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(problems.toString(), 1, problems.size());
		assertEquals(JavaScriptProblems.UNKNOWN_TYPE, problems.get(0).getID());
	}

	public void testUnknownParamType() {
		StringList code = new StringList();
		code.add("/** @param {LongString} x */");
		code.add("function y(x){}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(problems.toString(), 1, problems.size());
		assertEquals(JavaScriptProblems.UNKNOWN_TYPE, problems.get(0).getID());
	}

	public void testUnknownJavaScriptFunctionType() {
		final List<IProblem> problems = validate("var x = new LongString()");
		assertEquals(problems.toString(), 1, problems.size());
		assertEquals(JavaScriptProblems.UNKNOWN_TYPE, problems.get(0).getID());
	}

	public void testUnknownJavaScriptFunctionCall() {
		final List<IProblem> problems = validate("var x = longString()");
		assertEquals(1, problems.size());
		assertEquals(JavaScriptProblems.UNDEFINED_METHOD, problems.get(0)
				.getID());
	}

	public void testDeprecatedType() {
		final List<IProblem> problems = validate("var x:ExampleService2");
		assertEquals(problems.toString(), 1, problems.size());
		assertEquals(JavaScriptProblems.DEPRECATED_TYPE, problems.get(0)
				.getID());
	}

	public void testValidMethodCall() {
		StringList code = new StringList();
		code.add("var x:ExampleService");
		code.add("x.execute()");
		final List<IProblem> problems = validate(code.toString());
		assertTrue(problems.isEmpty());
	}

	public void testUndefinedMethodCall() {
		StringList code = new StringList();
		code.add("var x:ExampleService");
		code.add("x.runUndefindMethod()");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(1, problems.size());
		assertEquals(JavaScriptProblems.UNDEFINED_JAVA_METHOD, problems.get(0)
				.getID());
	}

	public void testMethodOverload1() {
		StringList code = new StringList();
		code.add("var x:ExampleService");
		code.add("x.run()");
		code.add("x.run('Hello')");
		code.add("x.run('Hello','World')");
		final List<IProblem> problems = validate(code.toString());
		assertTrue(problems.isEmpty());
	}

	public void testMethodOverload2() {
		StringList code = new StringList();
		code.add("var x:ExampleService");
		code.add("x.run('Hello','World','!')");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(1, problems.size());
		assertEquals(JavaScriptProblems.WRONG_JAVA_PARAMETERS, problems.get(0)
				.getID());
	}

	public void testMethodCallWrongParamCount() {
		StringList code = new StringList();
		code.add("var x:ExampleService");
		code.add("x.execute(1)");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(1, problems.size());
		assertEquals(JavaScriptProblems.WRONG_JAVA_PARAMETERS, problems.get(0)
				.getID());
	}

	public void testDeprecatedMethodCall() {
		StringList code = new StringList();
		code.add("var x:ExampleService");
		code.add("x.executeCompatible()");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(1, problems.size());
		assertEquals(JavaScriptProblems.DEPRECATED_METHOD, problems.get(0)
				.getID());
	}

	public void testDeprecatedMethodCall_TypeInference() {
		StringList code = new StringList();
		code.add("var x:ExampleService");
		code.add("var q = x.execute().service");
		code.add("q.executeCompatible()");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(1, problems.size());
		assertEquals(JavaScriptProblems.DEPRECATED_METHOD, problems.get(0)
				.getID());
	}

	public void testPropertyAccess() {
		StringList code = new StringList();
		code.add("var x:ExampleService");
		code.add("var name = x.name");
		final List<IProblem> problems = validate(code.toString());
		assertTrue(problems.isEmpty());
	}

	public void testDeprecatedPropertyAccess() {
		StringList code = new StringList();
		code.add("var x:ExampleService");
		code.add("var name = x.nameCompatible");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(1, problems.size());
		assertEquals(JavaScriptProblems.DEPRECATED_PROPERTY, problems.get(0)
				.getID());
	}

	public void testDeprecatedTopLevelProperty() {
		StringList code = new StringList();
		code.add("myExampleForms.service");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(1, problems.size());
		assertEquals(JavaScriptProblems.DEPRECATED_PROPERTY, problems.get(0)
				.getID());
		assertTrue(problems.get(0).getMessage().contains("myExampleForms"));
	}

	public void testUndefinedPropertyAccess() {
		StringList code = new StringList();
		code.add("var x:ExampleService");
		code.add("var name = x.noname");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(1, problems.size());
		assertEquals(JavaScriptProblems.UNDEFINED_JAVA_PROPERTY, problems
				.get(0).getID());
	}

	public void testUndefinedTypePropertyAccess() {
		StringList code = new StringList();
		code.add("/** @type PersonClassIsNotDefined */");
		code.add("var person");
		code.add("var name = person.name");
		code.add("var address = person.address");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(problems.toString(), 3, problems.size());
		assertEquals(JavaScriptProblems.UNKNOWN_TYPE, problems.get(0).getID());
		assertEquals(JavaScriptProblems.UNDEFINED_PROPERTY, problems.get(1)
				.getID());
		assertEquals(JavaScriptProblems.UNDEFINED_PROPERTY, problems.get(2)
				.getID());
	}

	public void testUndefinedPropertyAssignment() {
		StringList code = new StringList();
		code.add("var x:ExampleService");
		code.add("x.noname = true");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(1, problems.size());
		assertEquals(JavaScriptProblems.UNDEFINED_JAVA_PROPERTY, problems
				.get(0).getID());
	}

	public void testUndefinedVariableAssignment() {
		StringList code = new StringList();
		code.add("x.noname = true");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(1, problems.size());
		assertEquals(JavaScriptProblems.UNDECLARED_VARIABLE, problems.get(0)
				.getID());
	}

	public void testPropertyAssignment() {
		StringList code = new StringList();
		code.add("var x = { }");
		code.add("x.noname = true");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(problems.toString(), 0, problems.size());
	}

	public void testMethodAsPropertyAccess() {
		StringList code = new StringList();
		code.add("var x:ExampleService");
		code.add("var name = x.execute");
		final List<IProblem> problems = validate(code.toString());
		assertTrue(problems.toString(), problems.isEmpty());
	}

	public void testElementResolver() {
		StringList code = new StringList();
		code.add("exampleForms.service.name");
		code.add("exampleForms.service.nameCompatible");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(1, problems.size());
		assertEquals(JavaScriptProblems.DEPRECATED_PROPERTY, problems.get(0)
				.getID());
	}

	public void testFunctionCallWithCorrectFunctionObjectArgument() {
		StringList code = new StringList();
		code.add("function Node1() {");
		code.add("this.a=10;");
		code.add("}");
		code.add("function Node2() {");
		code.add("this.a=10;");
		code.add("}");
		code.add("function call(arg:Node1){");
		code.add("}");
		code.add("function test(){");
		code.add("call(new Node1());");
		code.add("}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(problems.toString(), 0, problems.size());
	}

	public void testFunctionCallWithInCorrectFunctionObjectArgument() {
		StringList code = new StringList();
		code.add("function Node1() {");
		code.add("this.a=10;");
		code.add("}");
		code.add("function Node2() {");
		code.add("this.a=10;");
		code.add("}");
		code.add("function call(arg:Node1){");
		code.add("}");
		code.add("function test(){");
		code.add("call(new Node2());");
		code.add("}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(problems.toString(), 1, problems.size());
	}

	public void testFunctionCallWithCorrectFunctionObjectArgumentJSDOC() {
		StringList code = new StringList();
		code.add("function Node1() {");
		code.add("this.a=10;");
		code.add("}");
		code.add("function Node2() {");
		code.add("this.a=10;");
		code.add("}");
		code.add("/**");
		code.add(" * @param {Node1} arg");
		code.add(" */");
		code.add("function call(arg){");
		code.add("}");
		code.add("function test(){");
		code.add("call(new Node1());");
		code.add("}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(problems.toString(), 0, problems.size());
	}

	public void testFunctionCallWithInCorrectFunctionObjectArgumentJSDOC() {
		StringList code = new StringList();
		code.add("function Node1() {");
		code.add("this.a=10;");
		code.add("}");
		code.add("function Node2() {");
		code.add("this.a=10;");
		code.add("}");
		code.add("/**");
		code.add(" * @param {Node1} arg");
		code.add(" */");
		code.add("function call(arg){");
		code.add("}");
		code.add("function test(){");
		code.add("call(new Node2());");
		code.add("}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(problems.toString(), 1, problems.size());
	}

	public void testLazyJSObject() {
		StringList code = new StringList();
		code.add("var x = new function x(){");
		code.add("this.newNode = function newNode(){");
		code.add("return new Node()");
		code.add("}");
		code.add("function Node() {");
		code.add("this.a=10;");
		code.add("}");
		code.add("}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(problems.toString(), 0, problems.size());
	}

	public void testReturnType() {
		StringList code = new StringList();
		code.add("/**");
		code.add(" * @return {String}");
		code.add(" */");
		code.add("function test() {");
		code.add("	return '';");
		code.add("}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(problems.toString(), 0, problems.size());
	}

	public void testReturnType2() {
		StringList code = new StringList();
		code.add("/**");
		code.add(" * @param {ExampleService} srv");
		code.add(" * @return {ExampleService}");
		code.add(" */");
		code.add("function test(srv) {");
		code.add("	return srv;");
		code.add("}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(problems.toString(), 0, problems.size());
	}

	public void testReturnWrongType() {
		StringList code = new StringList();
		code.add("/**");
		code.add(" * @return {String}");
		code.add(" */");
		code.add("function test() {");
		code.add("	return 1;");
		code.add("}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(problems.toString(), 1, problems.size());
		assertEquals(
				JavaScriptProblems.DECLARATION_MISMATCH_ACTUAL_RETURN_TYPE,
				problems.get(0).getID());
	}

	public void testLazyReturnTypeWithJSDocParam() {
		StringList code = new StringList();
		code.add("/**");
		code.add(" * @param {Node} node");
		code.add(" */");
		code.add("function addChild(node) {");
		code.add("	return node;");
		code.add("}");
		code.add("function TestObject() {");
		code.add("	var x = addChild(new Node());");
		code.add("	x.a;");
		code.add("}");
		code.add("function Node() {");
		code.add("	this.a = 10;");
		code.add("}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(problems.toString(), 0, problems.size());
	}

	public void testLazyReturnTypeWithTypeInfo() {
		StringList code = new StringList();
		code.add("/**");
		code.add(" * @param node");
		code.add(" */");
		code.add("function addChild(node:Node) {");
		code.add("	return node;");
		code.add("}");
		code.add("function TestObject() {");
		code.add("	var x = addChild(new Node());");
		code.add("	x.a;");
		code.add("}");
		code.add("function Node() {");
		code.add("	this.a = 10;");
		code.add("}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(problems.toString(), 0, problems.size());
	}

	public void testLazyReturnTypeWithNoTypeParamInfo() {
		StringList code = new StringList();
		code.add("/**");
		code.add(" * @param node");
		code.add(" */");
		code.add("function addChild(node) {");
		code.add("	return node;");
		code.add("}");
		code.add("function TestObject() {");
		code.add("	var x = addChild(new Node());");
		code.add("	x.a;");
		code.add("}");
		code.add("function Node() {");
		code.add("	this.a = 10;");
		code.add("}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(problems.toString(), 1, problems.size());
	}

	public void testGenericParamWtihNoneGenericCall() throws Exception {
		List<String> code = new StringList();
		code.add("/**");
		code.add(" * @param {Array<String>} array the value of the node");
		code.add(" */");
		code.add("function testArray(array) {");
		code.add("}");
		code.add("function test() {");
		code.add("testArray(new Array());");
		code.add("}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(problems.toString(), 1, problems.size());
		assertEquals(JavaScriptProblems.WRONG_PARAMETERS, problems.get(0)
				.getID());
	}

	public void testGenericParamWtihGenericCall() throws Exception {
		List<String> code = new StringList();
		code.add("/**");
		code.add(" * @param {Array<String>} array the value of the node");
		code.add(" */");
		code.add("function testArray(array) {");
		code.add("}");
		code.add("function test() {");
		code.add("testArray(['test','test2']);");
		code.add("}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(problems.toString(), 0, problems.size());
	}

	public void testNoneGenericParamWtihGenericCall() throws Exception {
		List<String> code = new StringList();
		code.add("/**");
		code.add(" * @param {Array} array the value of the node");
		code.add(" */");
		code.add("function testArray(array) {");
		code.add("}");
		code.add("function test() {");
		code.add("testArray(['test','test2']);");
		code.add("}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(problems.toString(), 0, problems.size());
	}

	public void testFunctionWithOptionalParams() throws Exception {
		List<String> code = new StringList();
		code.add("/**");
		code.add(" * @param {String} a");
		code.add(" * @param {String} [b]");
		code.add(" */");
		code.add("function tester(a,b) {");
		code.add("}");
		code.add("function test() {");
		code.add("tester('test');");
		code.add("tester('test','test2');");
		code.add("}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(problems.toString(), 0, problems.size());
	}

	public void testFunctionCallWithUndefinedArgumments() throws Exception {
		List<String> code = new StringList();
		code.add("/**");
		code.add(" * @param {String} a");
		code.add(" * @param {String} b");
		code.add(" */");
		code.add("function tester(a,b) {");
		code.add("}");
		code.add("function test() {");
		code.add("tester(a,b);");
		code.add("}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(2, problems.size());
		assertEquals(JavaScriptProblems.UNDECLARED_VARIABLE, problems.get(0)
				.getID());
		assertEquals(JavaScriptProblems.UNDECLARED_VARIABLE, problems.get(1)
				.getID());
	}

	public void testUndefinedVariables() throws Exception {
		List<String> code = new StringList();
		code.add("function test() {");
		code.add("a=10");
		code.add("b['test']='1'");
		code.add("}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(2, problems.size());
		assertEquals(JavaScriptProblems.UNDECLARED_VARIABLE, problems.get(0)
				.getID());
		assertEquals(JavaScriptProblems.UNDECLARED_VARIABLE, problems.get(1)
				.getID());
	}

	public void testArrayLookupWithoutAssign() throws Exception {
		List<String> code = new StringList();
		code.add("function test() {");
		code.add("var object = new Array();");
		code.add("var x = object['test'].length;");
		code.add("object.test;");
		code.add("}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(1, problems.size());
		assertEquals(JavaScriptProblems.UNDEFINED_PROPERTY, problems.get(0)
				.getID());
	}

	public void testArrayLookupWithAssign() throws Exception {
		List<String> code = new StringList();
		code.add("function test() {");
		code.add("var object = new Array();");
		code.add("object['test'] = '';");
		code.add("var x = object['test'].length;");
		code.add("object.test;");
		code.add("}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(0, problems.size());
	}

	public void testObjectInitalizerWithPropertyAndFunction() {
		List<String> code = new StringList();
		code.add("var initializer = {x:10,y:function(z){}};");
		code.add("function testInitializer() {");
		code.add("var a = initializer.x;");
		code.add("var b = initializer.y(a);");
		code.add("}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(problems.toString(), 0, problems.size());
	}

	public void testDifferentReturnTypesThenDeclared() {
		List<String> code = new StringList();
		code.add("/**");
		code.add("* @return {String} some result info");
		code.add("*/");
		code.add("function someFunction() {");
		code.add("var result_1 = 1;");
		code.add("var condition_1 = false;");
		code.add("if(condition_1) {");
		code.add("return result_1;");
		code.add("}");
		code.add("return '';");
		code.add("}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(problems.toString(), 1, problems.size());
	}

	public void testDifferentReturnTypesThenDeclaredWithUndefined() {
		List<String> code = new StringList();
		code.add("/**");
		code.add("* @return {String} some result info");
		code.add("*/");
		code.add("function someFunction() {");
		code.add("var result_1 = 1;");
		code.add("var condition_1 = false;");
		code.add("if(condition_1) {");
		code.add("return undefined;");
		code.add("}");
		code.add("return '';");
		code.add("}");
		final List<IProblem> problems = validate(code.toString());
		assertEquals(problems.toString(), 0, problems.size());
	}

}
