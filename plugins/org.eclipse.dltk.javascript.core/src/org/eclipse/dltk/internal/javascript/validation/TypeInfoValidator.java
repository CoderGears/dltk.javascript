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
package org.eclipse.dltk.internal.javascript.validation;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.core.builder.IBuildContext;
import org.eclipse.dltk.core.builder.IBuildParticipant;
import org.eclipse.dltk.internal.javascript.ti.ElementValue;
import org.eclipse.dltk.internal.javascript.ti.IReferenceAttributes;
import org.eclipse.dltk.internal.javascript.ti.ITypeInferenceContext;
import org.eclipse.dltk.internal.javascript.ti.IValueCollection;
import org.eclipse.dltk.internal.javascript.ti.IValueReference;
import org.eclipse.dltk.internal.javascript.ti.JSMethod;
import org.eclipse.dltk.internal.javascript.ti.MemberPredicates;
import org.eclipse.dltk.internal.javascript.ti.TypeInferencer2;
import org.eclipse.dltk.internal.javascript.ti.TypeInferencerVisitor;
import org.eclipse.dltk.javascript.ast.CallExpression;
import org.eclipse.dltk.javascript.ast.Expression;
import org.eclipse.dltk.javascript.ast.Identifier;
import org.eclipse.dltk.javascript.ast.NewExpression;
import org.eclipse.dltk.javascript.ast.PropertyExpression;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.core.JavaScriptProblems;
import org.eclipse.dltk.javascript.parser.Reporter;
import org.eclipse.dltk.javascript.typeinfo.IModelBuilder.IParameter;
import org.eclipse.dltk.javascript.typeinfo.model.Element;
import org.eclipse.dltk.javascript.typeinfo.model.Method;
import org.eclipse.dltk.javascript.typeinfo.model.Parameter;
import org.eclipse.dltk.javascript.typeinfo.model.ParameterKind;
import org.eclipse.dltk.javascript.typeinfo.model.Property;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.eclipse.dltk.javascript.typeinfo.model.TypeKind;
import org.eclipse.emf.common.util.EList;
import org.eclipse.osgi.util.NLS;

public class TypeInfoValidator implements IBuildParticipant, JavaScriptProblems {

	public void build(IBuildContext context) throws CoreException {
		final Script script = JavaScriptValidations.parse(context);
		if (script == null) {
			return;
		}
		TypeInferencer2 inferencer = new TypeInferencer2();
		inferencer.setModelElement(context.getSourceModule());
		inferencer.setVisitor(new ValidationVisitor(inferencer,
				JavaScriptValidations.createReporter(context)));
		inferencer.doInferencing(script);
	}

	private static enum VisitorMode {
		NORMAL, CALL
	}

	private static class UnresolvedCall {
		final IValueCollection collection;
		final CallExpression expression;

		public UnresolvedCall(IValueCollection collection,
				CallExpression expression) {
			this.collection = collection;
			this.expression = expression;
		}
	}

	public static class ValidationVisitor extends TypeInferencerVisitor {

		private final Reporter reporter;

		private final List<UnresolvedCall> unresolvedCallExpressions = new ArrayList<UnresolvedCall>();

		public ValidationVisitor(ITypeInferenceContext context,
				Reporter reporter) {
			super(context);
			this.reporter = reporter;
		}

		private final Map<ASTNode, VisitorMode> modes = new IdentityHashMap<ASTNode, VisitorMode>();

		private final Stack<ASTNode> visitStack = new Stack<ASTNode>();

		@Override
		public IValueReference visit(ASTNode node) {
			boolean rootNode = visitStack.isEmpty();
			visitStack.push(node);
			try {
				return super.visit(node);
			} finally {
				if (rootNode && !unresolvedCallExpressions.isEmpty()) {
					for (UnresolvedCall call : unresolvedCallExpressions
							.toArray(new UnresolvedCall[unresolvedCallExpressions
									.size()])) {
						enterContext(call.collection);
						try {
							visitCallExpressionImpl(call.expression, true);
						} finally {
							leaveContext();
						}
					}
				}
				visitStack.pop();
			}
		}

		private VisitorMode currentMode() {
			final VisitorMode mode = modes.get(visitStack.peek());
			return mode != null ? mode : VisitorMode.NORMAL;
		}

		@Override
		public IValueReference visitCallExpression(CallExpression node) {
			return visitCallExpressionImpl(node, false);
		}

		/**
		 * @param node
		 * @return
		 */
		private IValueReference visitCallExpressionImpl(CallExpression node,
				boolean reportNotUsedCalls) {
			final ASTNode expression = node.getExpression();
			final ASTNode methodNode;
			if (expression instanceof PropertyExpression) {
				methodNode = ((PropertyExpression) expression).getProperty();
			} else {
				methodNode = expression;
			}
			modes.put(expression, VisitorMode.CALL);
			final IValueReference reference = visit(expression);
			modes.remove(expression);
			if (reference != null) {
				if (reference.getName() == IValueReference.ARRAY_OP) {
					// ignore array lookup function calls like: array[1](),
					// those are dynamic.
					return reference.getChild(IValueReference.FUNCTION_OP);
				}
				final List<Method> methods = JavaScriptValidations
						.extractElements(reference, Method.class);
				if (methods != null) {
					final List<ASTNode> callArgs = node.getArguments();
					IValueReference[] arguments = new IValueReference[callArgs
							.size()];
					for (int i = 0, size = callArgs.size(); i < size; ++i) {
						arguments[i] = visit(callArgs.get(i));
					}
					Method method = JavaScriptValidations.selectMethod(methods,
							arguments);
					if (method == null) {
						final Type type = JavaScriptValidations
								.typeOf(reference.getParent());
						if (type != null) {
							if (type.getKind() == TypeKind.JAVA) {
								reporter.reportProblem(
										JavaScriptProblems.WRONG_PARAMETERS,
										NLS.bind(
												ValidationMessages.MethodNotSelected,
												new String[] {
														reference.getName(),
														type.getName(),
														describeArgTypes(arguments) }),
										methodNode.sourceStart(), methodNode
												.sourceEnd());

							} else {
								// TODO also a JS error (that should be
								// configurable)
							}
						}
						return null;
					}
					if (!validateParameterCount(method, callArgs)) {
						reportMethodParameterError(methodNode, arguments,
								method);
					}
					if (method.isDeprecated()) {
						reportDeprecatedMethod(methodNode, reference, method);
					}
				} else {
					final Type type = JavaScriptValidations.typeOf(reference
							.getParent());
					if (type != null) {
						if (type.getKind() == TypeKind.JAVA) {
							reporter.reportProblem(
									JavaScriptProblems.UNDEFINED_METHOD,
									NLS.bind(
											ValidationMessages.UndefinedMethod,
											reference.getName(), type.getName()),
									methodNode.sourceStart(), methodNode
											.sourceEnd());
						} else if (JavaScriptValidations.isStatic(reference
								.getParent())
								&& !ElementValue.findMembers(type,
										reference.getName(),
										MemberPredicates.NON_STATIC).isEmpty()) {
							reporter.reportProblem(
									JavaScriptProblems.NON_STATIC_METHOD,
									NLS.bind(
											"Cannot make a static reference to the non-static method {0}() from {1}",
											reference.getName(), type.getName()),
									methodNode.sourceStart(), methodNode
											.sourceEnd());
						} else {
							// TODO also report a JS error (that should be
							// configurable)
						}
					} else {
						Object attribute = reference.getAttribute(
								IReferenceAttributes.PARAMETERS, true);
						if (attribute instanceof JSMethod) {
							JSMethod method = (JSMethod) attribute;
							if (method.isDeprecated()) {
								reporter.reportProblem(
										JavaScriptProblems.DEPRECATED_FUNCTION,
										NLS.bind(
												ValidationMessages.DeprecatedFunction,
												reference.getName()),
										methodNode.sourceStart(), methodNode
												.sourceEnd());
							}
							List<IParameter> parameters = method
									.getParameters();
							final List<ASTNode> callArgs = node.getArguments();
							IValueReference[] arguments = new IValueReference[callArgs
									.size()];
							for (int i = 0, size = callArgs.size(); i < size; ++i) {
								arguments[i] = visit(callArgs.get(i));
							}
							if (!validateParameters(parameters, arguments)) {
								reporter.reportProblem(
										JavaScriptProblems.WRONG_PARAMETERS,
										NLS.bind(
												ValidationMessages.MethodNotApplicableInScript,
												new String[] {
														method.getName(),
														describeParamTypes(parameters),
														describeArgTypes(arguments) }),
										methodNode.sourceStart(), methodNode
												.sourceEnd());
							}
						} else {
							if (expression instanceof NewExpression) {
								Object value = reference
										.getChild(IValueReference.FUNCTION_OP);
								if (value instanceof IValueReference) {
									Type newType = JavaScriptValidations
											.typeOf((IValueReference) value);
									if (newType != null) {
										return (IValueReference) value;
									}
								}
							}
							if (reportNotUsedCalls) {
								if (expression instanceof NewExpression) {

									reporter.reportProblem(
											JavaScriptProblems.UNKNOWN_TYPE,
											NLS.bind(
													ValidationMessages.UnknownType,
													((NewExpression) expression)
															.getObjectClass()
															.toSourceString("")),
											methodNode.sourceStart(),
											methodNode.sourceEnd());

								} else {
									reporter.reportProblem(
											JavaScriptProblems.UNDEFINED_METHOD,
											NLS.bind(
													ValidationMessages.UndefinedMethodInScript,
													reference.getName()),
											methodNode.sourceStart(),
											methodNode.sourceEnd());
								}
							} else {
								unresolvedCallExpressions
										.add(new UnresolvedCall(peekContext(),
												node));
							}
						}
					}
				}
				return reference.getChild(IValueReference.FUNCTION_OP);
			} else {
				return null;
			}
		}

		private void reportDeprecatedMethod(ASTNode methodNode,
				IValueReference reference, Method method) {
			if (method.getDeclaringType() != null) {
				reporter.reportProblem(
						JavaScriptProblems.DEPRECATED_METHOD,
						NLS.bind(ValidationMessages.DeprecatedMethod, reference
								.getName(), method.getDeclaringType().getName()),
						methodNode.sourceStart(), methodNode.sourceEnd());
			} else {
				reporter.reportProblem(JavaScriptProblems.DEPRECATED_METHOD,
						NLS.bind(ValidationMessages.DeprecatedTopLevelMethod,
								reference.getName()), methodNode.sourceStart(),
						methodNode.sourceEnd());
			}
		}

		private void reportMethodParameterError(ASTNode methodNode,
				IValueReference[] arguments, Method method) {
			if (method.getDeclaringType() != null) {
				reporter.reportProblem(JavaScriptProblems.WRONG_PARAMETERS, NLS
						.bind(ValidationMessages.MethodNotApplicable,
								new String[] {
										method.getName(),
										describeParamTypes(method
												.getParameters()),
										method.getDeclaringType().getName(),
										describeArgTypes(arguments) }),
						methodNode.sourceStart(), methodNode.sourceEnd());
			} else {
				reporter.reportProblem(JavaScriptProblems.WRONG_PARAMETERS, NLS
						.bind(ValidationMessages.TopLevelMethodNotApplicable,
								new String[] {
										method.getName(),
										describeParamTypes(method
												.getParameters()),
										describeArgTypes(arguments) }),
						methodNode.sourceStart(), methodNode.sourceEnd());
			}
		}

		private boolean validateParameters(List<IParameter> parameters,
				IValueReference[] arguments) {
			if (arguments.length > parameters.size())
				return false;
			int testTypesSize = parameters.size();
			if (parameters.size() != arguments.length) {
				for (int i = arguments.length; i < parameters.size(); i++) {
					if (!parameters.get(i).isOptional())
						return false;
				}
				testTypesSize = arguments.length;
			}

			for (int i = 0; i < testTypesSize; i++) {
				Type param = parameters.get(i).getType();
				if (arguments[i] == null)
					continue;
				Type argumentType = arguments[i].getDeclaredType();
				if (argumentType == null) {
					if (!arguments[i].getTypes().isEmpty()) {
						argumentType = arguments[i].getTypes().iterator()
								.next();
					}
				}
				if (param != null && argumentType != null
						&& !param.getName().equals(argumentType.getName()))
					return false;
			}
			return true;
		}

		/**
		 * @param parameters
		 * @return
		 */
		private String describeParamTypes(EList<Parameter> parameters) {
			StringBuilder sb = new StringBuilder();
			for (Parameter parameter : parameters) {
				if (sb.length() != 0) {
					sb.append(',');
				}
				if (parameter.getKind() == ParameterKind.OPTIONAL)
					sb.append('[');
				if (parameter.getType() != null) {
					sb.append(parameter.getType().getName());
				} else {
					sb.append('?');
				}
				if (parameter.getKind() == ParameterKind.OPTIONAL)
					sb.append(']');
			}
			return sb.toString();
		}

		private String describeParamTypes(List<IParameter> parameters) {
			StringBuilder sb = new StringBuilder();
			for (IParameter parameter : parameters) {
				if (sb.length() != 0) {
					sb.append(',');
				}
				if (parameter.getType() != null) {
					sb.append(parameter.getType().getName());
				} else {
					sb.append('?');
				}
			}
			return sb.toString();
		}

		/**
		 * @param arguments
		 * @return
		 */
		private String describeArgTypes(IValueReference[] arguments) {
			StringBuilder sb = new StringBuilder();
			for (IValueReference argument : arguments) {
				if (sb.length() != 0) {
					sb.append(',');
				}
				if (argument == null) {
					sb.append("null");
				} else if (argument.getDeclaredType() != null) {
					sb.append(argument.getDeclaredType().getName());
				} else {
					final Set<Type> types = argument.getTypes();
					if (types.size() == 1) {
						sb.append(types.iterator().next().getName());
					} else {
						sb.append('?');
					}
				}
			}
			return sb.toString();
		}

		private <E extends Element> E extractElement(IValueReference reference,
				Class<E> elementType) {
			final List<E> elements = JavaScriptValidations.extractElements(
					reference, elementType);
			return elements != null ? elements.get(0) : null;
		}

		/**
		 * Validates the parameter count, returns <code>true</code> if correct.
		 * 
		 * @param method
		 * @param callArgs
		 * 
		 * @return
		 */
		private boolean validateParameterCount(Method method,
				List<ASTNode> callArgs) {
			final EList<Parameter> params = method.getParameters();
			if (params.size() == callArgs.size()) {
				return true;
			}
			if (params.size() < callArgs.size()
					&& !params.isEmpty()
					&& params.get(params.size() - 1).getKind() == ParameterKind.VARARGS) {
				return true;
			}
			if (params.size() > callArgs.size()
					&& params.get(callArgs.size()).getKind() == ParameterKind.OPTIONAL) {
				return true;
			}
			return false;
		}

		@Override
		public IValueReference visitPropertyExpression(PropertyExpression node) {
			final IValueReference result = super.visitPropertyExpression(node);
			if (result != null) {
				if (currentMode() != VisitorMode.CALL) {
					validateProperty(result, node.getProperty());
				}
				return result;
			} else {
				return null;
			}
		}

		@Override
		public IValueReference visitIdentifier(Identifier node) {
			final IValueReference result = peekContext().getChild(
					node.getName());
			final Property property = extractElement(result, Property.class);
			if (property != null && property.isDeprecated()) {
				reportDeprecatedProperty(property, null, node);
			}
			return result;
		}

		private void validateProperty(IValueReference result,
				Expression propName) {
			final Property property = extractElement(result, Property.class);
			if (property != null) {
				if (property.isDeprecated()) {
					final Property parentProperty = extractElement(
							result.getParent(), Property.class);
					if (parentProperty != null
							&& parentProperty.getDeclaringType() == null) {
						reportDeprecatedProperty(property, parentProperty,
								propName);
					} else {
						reportDeprecatedProperty(property,
								property.getDeclaringType(), propName);
					}
				}
			} else if (JavaScriptValidations.extractElements(result,
					Method.class) == null) {
				final Type type = JavaScriptValidations.typeOf(result
						.getParent());
				if (type != null && type.getKind() == TypeKind.JAVA) {
					reporter.reportProblem(
							JavaScriptProblems.UNDEFINED_PROPERTY, NLS.bind(
									ValidationMessages.UndefinedProperty,
									result.getName(), type.getName()), propName
									.sourceStart(), propName.sourceEnd());
				} else if (type != null
						&& (type.getKind() == TypeKind.JAVASCRIPT || type
								.getKind() == TypeKind.PREDEFINED)) {
					reporter.reportProblem(
							JavaScriptProblems.UNDEFINED_PROPERTY,
							NLS.bind(
									ValidationMessages.UndefinedPropertyInScript,
									result.getName(), type.getName()), propName
									.sourceStart(), propName.sourceEnd());
				}
			}
		}

		private void reportDeprecatedProperty(Property property, Element owner,
				ASTNode node) {
			final String msg;
			if (owner instanceof Type) {
				msg = NLS.bind(ValidationMessages.DeprecatedProperty,
						property.getName(), owner.getName());
			} else if (owner instanceof Property) {
				msg = NLS.bind(ValidationMessages.DeprecatedPropertyOfInstance,
						property.getName(), owner.getName());
			} else {
				msg = NLS.bind(ValidationMessages.DeprecatedPropertyNoType,
						property.getName());
			}
			reporter.reportProblem(JavaScriptProblems.DEPRECATED_PROPERTY, msg,
					node.sourceStart(), node.sourceEnd());
		}

		@Override
		protected Type resolveType(org.eclipse.dltk.javascript.ast.Type type) {
			final Type result = super.resolveType(type);
			if (result != null) {
				if (result.getKind() == TypeKind.UNKNOWN) {
					reporter.reportProblem(
							JavaScriptProblems.UNKNOWN_TYPE,
							NLS.bind(ValidationMessages.UnknownType,
									type.getName()), type.sourceStart(),
							type.sourceEnd());
				} else if (result.isDeprecated()) {
					reporter.reportProblem(
							JavaScriptProblems.DEPRECATED_TYPE,
							NLS.bind(ValidationMessages.DeprecatedType,
									type.getName()), type.sourceStart(),
							type.sourceEnd());
				}
			}
			return result;
		}
	}

}
