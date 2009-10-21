/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
/**
 * 
 */
package org.eclipse.dltk.internal.javascript.typeinference;

import org.eclipse.dltk.core.IOpenable;
import org.eclipse.dltk.core.ISourceRange;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.core.ModelElement;
import org.eclipse.dltk.internal.core.SourceField;
import org.eclipse.dltk.internal.core.SourceRange;

public class FakeField extends SourceField implements IProposalHolder {

	private int offset;
	private int length;
	private String snippet;
	private String proposalInfo;
	private final String type;

	public FakeField(ModelElement parent, String name, int offset, int length) {
		this(parent, name, offset, length, null);
	}

	/**
	 * @since 2.0
	 */
	public FakeField(ModelElement parent, String name, int offset, int length,
			String type) {
		super(parent, name);
		this.offset = offset;
		this.length = length;
		this.type = type;
	}

	public ISourceRange getNameRange() throws ModelException {
		if (offset == 0 && length == 0) {
			return super.getNameRange();
		}
		return new SourceRange(offset, length);
	}

	public ISourceRange getSourceRange() throws ModelException {
		if (offset == 0 && length == 0) {
			if (getElementInfo() == null)
				return null;
			return super.getSourceRange();
		}
		return new SourceRange(offset, length);
	}

	public String getSnippet() {
		return snippet;
	}

	/**
	 * @see org.eclipse.dltk.internal.core.SourceField#getType()
	 * @since 2.0
	 */
	public String getType() throws ModelException {
		return type;
	}

	/**
	 * @see org.eclipse.dltk.internal.core.ModelElement#exists()
	 * @since 2.0
	 */
	public boolean exists() {
		return true;
	}

	public void setSnippet(String snippet) {
		this.snippet = snippet;
	}

	/**
	 * @param proposalInfo
	 */
	public void setProposalInfo(String proposalInfo) {
		this.proposalInfo = proposalInfo;

	}

	/**
	 * @see org.eclipse.dltk.internal.javascript.typeinference.IProposalHolder#getProposalInfo()
	 */
	public String getProposalInfo() {
		return proposalInfo;
	}

	/**
	 * @see org.eclipse.dltk.internal.core.SourceRefElement#getOpenableParent()
	 */
	public IOpenable getOpenableParent() {

		IOpenable openableParent = super.getOpenableParent();
		try {
			// test if a buffer can be made, if not then just return null.
			// Is there a better way? (if it is not a script file)
			openableParent.getBuffer();
		} catch (Exception e) {
			return null;
		}
		return openableParent;
	}
}