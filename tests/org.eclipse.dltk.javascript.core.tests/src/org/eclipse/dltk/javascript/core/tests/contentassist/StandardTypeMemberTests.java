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
package org.eclipse.dltk.javascript.core.tests.contentassist;

public class StandardTypeMemberTests extends AbstractCompletionTest {

	public void testString() {
		assertEquals(40, getMembersOfString().size());
	}

	public void testNumber() {
		assertEquals(10, getMembersOfNumber().size());
	}

	public void testObject() {
		assertEquals(7, getMembersOfObject().size());
	}

	public void testXML() {
		assertEquals(39, getMembersOfXML().size());
	}
	
	public void testArray() {
		assertEquals(25, getMembersOfArray().size());
	}

	public void testFunction() {
		assertEquals(12, getMembersOfFunction().size());
	}


}
