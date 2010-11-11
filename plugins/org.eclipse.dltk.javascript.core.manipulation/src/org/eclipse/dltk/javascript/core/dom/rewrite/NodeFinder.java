/*******************************************************************************
 * Copyright (c) 2010 xored software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Vladislav Kuzkokov)
 *******************************************************************************/
package org.eclipse.dltk.javascript.core.dom.rewrite;

import org.eclipse.dltk.javascript.core.dom.DomPackage;
import org.eclipse.dltk.javascript.core.dom.Node;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;

public class NodeFinder {
	public static Node findExpression(Node node, int left,int right) {
		TreeIterator<EObject> it = node.eAllContents();
		while(it.hasNext()) {
			Node cur = (Node)it.next();
			if (right <= cur.getBegin() || cur.getEnd() <= left) {
				it.prune();
			}
			if (left <= cur.getBegin() && cur.getEnd() <= right) {
				return cur;
			}
			// check for atomic expressions
			if (cur.getBegin() <= left && left == right && right <= cur.getEnd()) {
				switch (cur.eClass().getClassifierID()) {
				case DomPackage.BOOLEAN_LITERAL:
				case DomPackage.NULL_LITERAL:
				case DomPackage.NUMERIC_LITERAL:
				case DomPackage.REGULAR_EXPRESSION_LITERAL:
				case DomPackage.STRING_LITERAL:
				case DomPackage.THIS_EXPRESSION:
				case DomPackage.VARIABLE_REFERENCE:
					return cur;
				}
			}
		}
		return null;
	}
}
