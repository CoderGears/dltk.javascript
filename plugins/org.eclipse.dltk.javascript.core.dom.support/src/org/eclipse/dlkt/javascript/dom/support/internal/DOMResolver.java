package org.eclipse.dlkt.javascript.dom.support.internal;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.dlkt.javascript.dom.support.IDesignTimeDOMProvider;
import org.eclipse.dlkt.javascript.dom.support.IProposalHolder;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IMember;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ISourceRange;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.javascript.reference.resolvers.IReferenceResolver;
import org.eclipse.dltk.internal.javascript.reference.resolvers.IResolvableReference;
import org.eclipse.dltk.internal.javascript.reference.resolvers.ReferenceResolverContext;
import org.eclipse.dltk.internal.javascript.typeinference.AbstractCallResultReference;
import org.eclipse.dltk.internal.javascript.typeinference.CompletionString;
import org.eclipse.dltk.internal.javascript.typeinference.IClassReference;
import org.eclipse.dltk.internal.javascript.typeinference.IReference;
import org.eclipse.dltk.internal.javascript.typeinference.IReferenceLocation;
import org.eclipse.dltk.internal.javascript.typeinference.ReferenceFactory;
import org.eclipse.dltk.internal.javascript.typeinference.ReferenceLocation;
import org.eclipse.dltk.internal.javascript.typeinference.ScriptableScopeReference;
import org.eclipse.dltk.internal.javascript.typeinference.StandardSelfCompletingReference;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.MemberBox;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeJavaTopPackage;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class DOMResolver implements IReferenceResolver {

	final static class ClassReference extends StandardSelfCompletingReference
			implements IClassReference {
		private ClassReference(String paramOrVarName, boolean childIsh) {
			super(paramOrVarName, childIsh);
		}
	}

	public Set<IReference> getChilds(IResolvableReference ref) {
		final Map<String, Class<Scriptable>> classRef = getClassMap();
		Set<IReference> result = new HashSet<IReference>();
		if (ref instanceof AbstractCallResultReference) {
			final AbstractCallResultReference cm = (AbstractCallResultReference) ref;
			// if (cm instanceof NewReference) {
			Class<Scriptable> clzz = classRef.get(cm.getId());
			if (clzz != null) {
				Method[] methods = clzz.getMethods();
				for (int a = 0; a < methods.length; a++) {
					String string = "jsFunction_";
					String stringget = "jsGet_";
					String stringset = "jsSet_";
					Method method = methods[a];
					if (method.getName().startsWith(string)) {
						StandardSelfCompletingReference r = new StandardSelfCompletingReference(
								method.getName().substring(string.length()),
								true);

						result.add(r);
						r.setFunctionRef();
					} else if (method.getName().startsWith(stringget)) {
						IReference r = new StandardSelfCompletingReference(
								method.getName().substring(stringget.length()),
								true);
						result.add(r);
					} else if (method.getName().startsWith(stringset)) {
						IReference r = new StandardSelfCompletingReference(
								method.getName().substring(stringset.length()),
								true);
						result.add(r);
					}
				}
			}
			result.addAll(resolveGlobals(cm.getId() + "."));
		} else if (ref instanceof ScriptableScopeReference) {
			ScriptableScopeReference ssr = (ScriptableScopeReference) ref;
			org.mozilla.javascript.Scriptable scriptable = ssr.getScriptable();
			Map<String, Object> results = new HashMap<String, Object>();
			fillMap(results, scriptable, false, null);
			Set<IReference> rs = new HashSet<IReference>();
			createReferences("", results, rs);
			return rs;
		}
		return result;
	}

	IDesignTimeDOMProvider[] providers;
	ISourceModule module;

	public Map<String, Class<Scriptable>> getClassMap() {
		if (classRef != null) {
			Map<String, Class<Scriptable>> object = classRef.get();
			if (object != null)
				return object;
		}
		Map<String, Class<Scriptable>> mp = new HashMap<String, Class<Scriptable>>();

		for (int a = 0; a < providers.length; a++) {
			if (providers[a].canResolve(module)) {
				Class<Scriptable>[] resolveHostObjectClasses = providers[a]
						.resolveHostObjectClasses(module);
				if (resolveHostObjectClasses == null)
					continue;
				for (int b = 0; b < resolveHostObjectClasses.length; b++) {
					Class<Scriptable> class1 = resolveHostObjectClasses[b];

					try {
						Scriptable dc = class1.newInstance();
						mp.put(dc.getClassName(), class1);
					} catch (InstantiationException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
					// mp.put(class1.getSimpleName(), class1);
				}
			}
		}
		classRef = new WeakReference<Map<String, Class<Scriptable>>>(mp);
		return mp;
	}

	private WeakReference<Map<String, Class<Scriptable>>> classRef;
	private ReferenceResolverContext owner;

	private Map<String, Object> getGlobalMap(String key) {
		Map<String, Object> mp = new HashMap<String, Object>();
		for (int a = 0; a < providers.length; a++) {
			if (providers[a].canResolve(module)) {
				Scriptable resolveTopLevelScope = providers[a]
						.resolveTopLevelScope(module);
				if (resolveTopLevelScope == null)
					continue;
				fillMap(mp, resolveTopLevelScope, true, key);
			}
		}
		return mp;
	}

	void fillMap(Map<String, Object> mp, Scriptable scope, boolean walkParent,
			String idToFind) {
		String returnType = null;
		if (scope instanceof IProposalHolder
				&& (returnType = ((IProposalHolder) scope).getReturnType()) != null) {
			// the scope overrides its return type
			IReference typeReference = ReferenceFactory.createTypeReference(
					idToFind, returnType, owner);
			if (typeReference instanceof ScriptableScopeReference) {
				Scriptable resolved = ((ScriptableScopeReference) typeReference)
						.getScriptable();
				if (!(resolved instanceof IProposalHolder && returnType
						.equals(((IProposalHolder) resolved).getReturnType()))) {
					// only override if it was transformed to something else.
					scope = resolved;
				}
			} else {
				for (IReference ref : typeReference.getChilds(true)) {
					if (idToFind == null || ref.getName().equals(idToFind)) {
						mp.put(ref.getName(), new ReferenceScope(ref));
					}
				}
				if (mp.size() > 0)
					return;
			}
		}
		try {
			// some scriptable want to have a context when you get the value or
			// ids.
			Context.enter();
			Scriptable prototype = scope.getPrototype();
			if (prototype != null) {
				fillMap(mp, prototype, walkParent, idToFind);
				if (idToFind != null && mp.size() == 1)
					return;
			}
			if (walkParent) {
				Scriptable parentScope = scope.getParentScope();
				if (parentScope != null) {
					fillMap(mp, parentScope, walkParent, idToFind);
					if (idToFind != null && mp.size() == 1)
						return;
				}
			}
			Object[] allIds = null;
			for (int a = 0; a < providers.length; a++) {
				if (providers[a].canResolve(module)) {
					allIds = providers[a].resolveIds(scope, idToFind);
					if (allIds != null)
						break;
				}
			}
			if (allIds == null) {
				if (scope instanceof ScriptableObject) {
					allIds = ((ScriptableObject) scope).getAllIds();
				} else {
					allIds = scope.getIds();
				}
			}
			for (int b = 0; b < allIds.length; b++) {
				String key = allIds[b].toString();
				if (idToFind != null && !idToFind.equals(key))
					continue;
				try {
					Object object = null;
					for (int a = 0; a < providers.length; a++) {
						if (providers[a].canResolve(module)) {
							object = providers[a].getProposal(scope, key);
							if (object != null)
								break;
						}
					}
					if (object == null) {
						object = scope.get(key, scope);
					}
					mp.put(key, object);
					if (idToFind != null)
						break;
				} catch (Throwable e) {
					e.printStackTrace();
				}

			}
		} finally {
			Context.exit();
		}
		return;
	}

	public void init(ReferenceResolverContext owner) {
		this.owner = owner;
		this.module = owner.getModule();
		providers = DomResolverSupport.getProviders();

	}

	public void processCall(String call, String objId) {

	}

	public Set<IReference> resolveGlobals(String completion) {
		completion = CompletionString.parse(completion, true);
		int pos = completion.indexOf('.');
		String key = pos == -1 ? completion : completion.substring(0, pos);

		Map<String, Object> globals = getGlobalMap(pos == -1 ? null : key);
		Map<String, Class<Scriptable>> clss = getClassMap();
		Set<IReference> rs = new HashSet<IReference>();

		if (pos == -1) {
			createReferences(key, globals, rs);
			for (String s : clss.keySet()) {
				if (s.startsWith(key)) {
					rs.add(new ClassReference(s, false));
				}
			}
		} else {
			while (pos != -1) {
				Object object = globals.get(key);
				if (object instanceof IProposalHolder) {
					object = ((IProposalHolder) object).getObject();
				}
				object = getObjectReferenceScope(key, object);
				if (object instanceof Scriptable) {
					completion = completion.substring(pos + 1);
					pos = completion.indexOf('.');
					key = pos == -1 ? completion : completion.substring(0, pos);
					Scriptable sc = (Scriptable) object;
					globals = new HashMap<String, Object>();
					fillMap(globals, sc, false,
							(pos == -1 && !"[]".equals(key)) ? null : key);
				} else {
					// not at match at all clear it
					globals = new HashMap<String, Object>();
					break;
				}
			}
			createReferences(key, globals, rs);
		}
		return rs;
	}

	void createReferences(String key, Map<String, Object> globals,
			Set<IReference> rs) {
		for (Map.Entry<String, Object> entry : globals.entrySet()) {
			String s = entry.getKey();

			if (s.startsWith(key)) {
				IFile sourceFile = null;
				Object object = entry.getValue();
				IReference ref = null;
				if (object instanceof ReferenceScope) {
					ref = ((ReferenceScope) object).getReference();
				} else if (object instanceof Scriptable) {
					StandardSelfCompletingReference uref = new ScriptableScopeReference(
							s, (Scriptable) object, owner);
					if (object instanceof IProposalHolder) {
						IProposalHolder fapn = (IProposalHolder) object;
						uref.setParameterNames(fapn.getParameterNames());
						uref.setProposalInfo(fapn.getProposalInfo());
						sourceFile = fapn.getSourceFile();
						uref.setReturnType(fapn.getReturnType());
						uref.setImageUrl(fapn.getImageURL());
						if (fapn.isFunctionRef())
							uref.setFunctionRef();
					}
					ref = uref;
				} else if (object instanceof IProposalHolder) {
					IProposalHolder fapn = (IProposalHolder) object;
					object = getObjectReferenceScope(s,
							((IProposalHolder) object).getObject());
					if (object instanceof ReferenceScope) {
						ref = ((ReferenceScope) object).getReference();
					} else {
						ref = new StandardSelfCompletingReference(s, false);
					}
					if (ref instanceof StandardSelfCompletingReference) {
						StandardSelfCompletingReference uref = (StandardSelfCompletingReference) ref;
						uref.setParameterNames(fapn.getParameterNames());
						uref.setProposalInfo(fapn.getProposalInfo());
						uref.setImageUrl(fapn.getImageURL());
						uref.setReturnType(fapn.getReturnType());
						sourceFile = fapn.getSourceFile();
						object = fapn.getObject();
						if (fapn.isFunctionRef())
							uref.setFunctionRef();
					}
				} else {
					ref = new StandardSelfCompletingReference(s, false);
				}

				if (ref instanceof StandardSelfCompletingReference) {
					StandardSelfCompletingReference uref = (StandardSelfCompletingReference) ref;
					if (!ref.isFunctionRef() && object instanceof Function
							&& !(object instanceof NativeJavaTopPackage)) {
						uref.setFunctionRef();
					}

					if (sourceFile != null) {
						IReferenceLocation location = uref.getLocation();
						if ("js"
								.equalsIgnoreCase(sourceFile.getFileExtension())) {
							try {
								ISourceModule sourceFileModule = DLTKCore
										.createSourceModuleFrom(sourceFile);
								IModelElement[] children = sourceFileModule
										.getChildren();
								ISourceRange nameRange = null;
								String name = uref.getName();
								for (int i = 0; i < children.length; i++) {
									IModelElement child = children[i];
									if (child instanceof IMember) {
										if (name.equals(child.getElementName())) {
											nameRange = ((IMember) child)
													.getNameRange();
											// if it is an exact match break
											// method == function reference
											// field != function reference
											// else try the next.
											if ((child.getElementType() == IModelElement.METHOD && uref
													.isFunctionRef())
													|| (child.getElementType() == IModelElement.FIELD && !uref
															.isFunctionRef())) {
												break;
											}
										}
									}
								}
								if (nameRange != null) {
									location = new ReferenceLocation(
											sourceFileModule, nameRange
													.getOffset(), nameRange
													.getLength());
								}
							} catch (ModelException ex) {
							}
						}
						ref.setLocationInformation(location);
					} else {
						if (uref.getLocation() != null) {
							ref.setLocationInformation(uref.getLocation());
						} else {
							ref.setLocationInformation(new ReferenceLocation(
									module, 0, 0));
						}
					}
				}
				rs.add(ref);
			}
		}
	}

	/**
	 * @param s
	 * @param object
	 * @return
	 */
	private Object getObjectReferenceScope(String s, Object object) {
		if (object == null)
			return null;
		StandardSelfCompletingReference uref = null;
		Class<?> clz = object.getClass();
		if (object instanceof NativeJavaObject) {
			clz = ((NativeJavaObject) object).unwrap().getClass();
		} else if (object instanceof NativeJavaMethod) {
			MemberBox[] methods = ((NativeJavaMethod) object).getMethods();
			for (int i = 0; i < methods.length; i++) {
				if (methods[i].getReturnType() != null) {
					clz = methods[i].getReturnType();
					break;
				}
				// TODO param names and method overrides (dup names)
			}
		}
		if (clz == String.class) {
			uref = ReferenceFactory.createStringReference(s);
		} else if (clz == Boolean.class || clz == boolean.class) {
			uref = ReferenceFactory.createBooleanReference(s);
		} else if (Date.class.isAssignableFrom(clz)) {
			uref = ReferenceFactory.createDateReference(s);
			// booleans are just above.
		} else if (Number.class.isAssignableFrom(clz) || clz.isPrimitive()) {
			uref = ReferenceFactory.createNumberReference(s);
		} else if (clz.isArray()) {
			uref = ReferenceFactory.createArrayReference(s);
		}
		if (uref != null)
			return new ReferenceScope(uref);
		return object;
	}

	public boolean canResolve(ISourceModule module) {
		return true;
	}

}
