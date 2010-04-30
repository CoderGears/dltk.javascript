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

import java.util.Collections;
import java.util.Set;

import org.eclipse.dltk.javascript.typeinfo.model.Element;
import org.eclipse.dltk.javascript.typeinfo.model.Member;
import org.eclipse.dltk.javascript.typeinfo.model.Method;
import org.eclipse.dltk.javascript.typeinfo.model.Property;
import org.eclipse.dltk.javascript.typeinfo.model.Type;

public abstract class ElementValue implements IValue {

	public static ElementValue findMember(Type type, String name) {
		if (type != null) {
			for (Member member : type.getMembers()) {
				if (name.equals(member.getName())) {
					return createFor(member);
				}
			}
		}
		return null;
	}

	public static ElementValue createFor(Element element) {
		if (element instanceof Method) {
			return new MethodValue((Method) element);
		} else if (element instanceof Property) {
			return new PropertyValue((Property) element);
		} else {
			return new TypeValue((Type) element);
		}
	}

	private static class TypeValue extends ElementValue implements IValue {

		private final Type type;

		public TypeValue(Type type) {
			this.type = type;
		}

		@Override
		protected Element getElement() {
			return type;
		}

		public IValue getChild(String name) {
			return findMember(type, name);
		}

		public Type getDeclaredType() {
			return type;
		}

	}

	private static class MethodValue extends ElementValue implements IValue {

		private final Method method;

		public MethodValue(Method method) {
			this.method = method;
		}

		@Override
		protected Element getElement() {
			return method;
		}

		@Override
		public ReferenceKind getKind() {
			return ReferenceKind.METHOD;
		}

		public IValue getChild(String name) {
			if (IValueReference.FUNCTION_OP.equals(name)) {
				return new TypeValue(method.getType());
			}
			return null;
		}

		public Type getDeclaredType() {
			return null;
		}

	}

	private static class PropertyValue extends ElementValue implements IValue {

		private final Property property;

		public PropertyValue(Property property) {
			this.property = property;
		}

		@Override
		protected Element getElement() {
			return property;
		}

		@Override
		public ReferenceKind getKind() {
			return ReferenceKind.PROPERTY;
		}

		public IValue getChild(String name) {
			return ElementValue.findMember(property.getType(), name);
		}

		public Type getDeclaredType() {
			return property.getType();
		}

	}

	protected abstract Element getElement();

	public final void clear() {
	}

	public final void addValue(IValue src) {
	}

	public final Object getAttribute(String key) {
		if (IReferenceAttributes.ELEMENT.equals(key)) {
			return getElement();
		}
		return null;
	}

	public final void removeAttribute(String key) {
	}

	public final void setAttribute(String key, Object value) {
	}

	public final Set<String> getDirectChildren() {
		return Collections.emptySet();
	}

	public final boolean hasChild(String name) {
		return false;
	}

	public final IValue createChild(String name) {
		throw new UnsupportedOperationException();
	}

	public ReferenceKind getKind() {
		return ReferenceKind.UNKNOWN;
	}

	public final ReferenceLocation getLocation() {
		return ReferenceLocation.UNKNOWN;
	}

	public final Set<Type> getTypes() {
		return Collections.emptySet();
	}

	public final void setDeclaredType(Type declaredType) {
	}

	public final void setKind(ReferenceKind kind) {
	}

	public final void setLocation(ReferenceLocation location) {
	}

}
