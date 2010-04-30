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
package org.eclipse.dltk.internal.javascript.ti;

public class AnonymousValue extends AbstractReference {

	private final Value value = new Value();

	@Override
	public Value getValue() {
		return value;
	}

	@Override
	public Value createValue() {
		return getValue();
	}

	public void delete() {
		// TODO Auto-generated method stub
	}

	public String getName() {
		return "";
	}

	public ITypeInferenceContext getContext() {
		return null;
	}

	public IValueParent getParent() {
		return null;
	}

}
