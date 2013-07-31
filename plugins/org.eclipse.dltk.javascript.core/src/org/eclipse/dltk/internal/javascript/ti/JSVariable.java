package org.eclipse.dltk.internal.javascript.ti;

import org.eclipse.dltk.javascript.typeinfo.IModelBuilder.IVariable;
import org.eclipse.dltk.javascript.typeinfo.model.JSType;
import org.eclipse.dltk.javascript.typeinfo.model.Visibility;

public class JSVariable extends JSElement implements IVariable {

	private boolean deprecated;
	private Visibility visibility;
	private JSType typeDef;

	public JSVariable() {
		super();
	}

	public JSVariable(String name) {
		super(name);
	}

	public void setTypeDef(JSType typeDef) {
		this.typeDef = typeDef;
	}

	public JSType getTypeDef() {
		return typeDef;
	}

	public void setDeprecated(boolean deprecated) {
		this.deprecated = deprecated;
	}

	public boolean isDeprecated() {
		return deprecated;
	}

	public Visibility getVisibility() {
		return visibility;
	}

	public void setVisibility(Visibility visibility) {
		this.visibility = visibility;
	}

}
