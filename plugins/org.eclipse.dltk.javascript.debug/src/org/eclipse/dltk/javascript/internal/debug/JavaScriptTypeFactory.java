package org.eclipse.dltk.javascript.internal.debug;

import org.eclipse.dltk.debug.core.model.ArrayScriptType;
import org.eclipse.dltk.debug.core.model.AtomicScriptType;
import org.eclipse.dltk.debug.core.model.ComplexScriptType;
import org.eclipse.dltk.debug.core.model.IScriptType;
import org.eclipse.dltk.debug.core.model.IScriptTypeFactory;
import org.eclipse.dltk.debug.core.model.StringScriptType;

public class JavaScriptTypeFactory implements IScriptTypeFactory {
	private static final String[] atomicTypes = { "number", "boolean", "date" };

	public JavaScriptTypeFactory() {

	}

	public IScriptType buildType(String type) {
		for (int i = 0; i < atomicTypes.length; ++i) {
			if (atomicTypes[i].equals(type)) {
				return new AtomicScriptType(type);
			}
		}

		if ("javaarray".equals(type) || "array".equals(type)) {
			return new ArrayScriptType();
		}

		if ("string".equals(type)) {
			return new StringScriptType("string");
		}
		return new ComplexScriptType(type);
	}
}
