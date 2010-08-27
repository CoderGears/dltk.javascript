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
package org.eclipse.dltk.javascript.core;

import org.eclipse.dltk.compiler.problem.IProblem;

public interface JavaScriptProblems {

	public static final int UNKNOWN_TYPE = IProblem.TypeRelated + 1;
	public static final int DEPRECATED_TYPE = IProblem.TypeRelated + 2;

	public static final int UNDEFINED_METHOD = IProblem.MethodRelated + 1;
	public static final int WRONG_PARAMETERS = IProblem.MethodRelated + 2;
	public static final int DEPRECATED_METHOD = IProblem.MethodRelated + 3;
	/**
	 * @since 3.0
	 */
	public static final int DEPRECATED_FUNCTION = IProblem.MethodRelated + 4;

	public static final int UNDEFINED_PROPERTY = IProblem.FieldRelated + 1;
	public static final int DEPRECATED_PROPERTY = IProblem.FieldRelated + 2;

	public static final int EQUAL_AS_ASSIGN = IProblem.Internal + 1;
	public static final int INVALID_ASSIGN_LEFT = IProblem.Internal + 2;

	public static final int UNREACHABLE_CODE = IProblem.Internal + 3;
	public static final int RETURN_INCONSISTENT = IProblem.Internal + 4;
	public static final int FUNCTION_NOT_ALWAYS_RETURN_VALUE = IProblem.Internal + 5;

	public static final int CONTINUE_NON_LOOP_LABEL = IProblem.Internal + 6;
	public static final int BREAK_NON_LOOP_LABEL = IProblem.Internal + 7;

}
