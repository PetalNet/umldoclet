/*
 * Copyright 2016-2026 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.talsmasoftware.umldoclet.javadoc;

import nl.talsmasoftware.umldoclet.uml.TypeName;

import javax.lang.model.element.Element;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/// Simple data object containing a (possibly) derived type name with a cardinality.
///
/// Container types are unwrapped into a *chain* of candidates, outermost first:
/// `Map<String, List<Customer>>` yields `List` (`"*"`) followed by `Customer` (`"*"`).
/// The caller picks the first candidate that is part of its diagram, so a container element that is itself
/// in the diagram (e.g. `List<CustomerGroup>` where `CustomerGroup implements Iterable<Customer>`)
/// is still referenced directly, exactly like before nested unwrapping existed.
///
/// @author Sjoerd Talsma
final class TypeNameWithCardinality {
    /// Guard against pathological nesting.
    private static final int MAX_DEPTH = 8;

    final TypeName typeName;
    final String cardinality;
    /// The element of the type, if it is a declared type, otherwise `null`.
    final Element element;
    /// Whether this candidate stems from a code path that did not exist upstream
    /// (a `java.util.Map` or a nested container), requiring exact diagram membership.
    final boolean derived;
    /// The next (more deeply unwrapped) candidate, or `null`.
    final TypeNameWithCardinality nested;
    /// Key types of the `java.util.Map` containers that were unwrapped to reach this candidate (outermost first).
    final List<TypeNameWithCardinality> keys;

    private TypeNameWithCardinality(TypeName typeName, String cardinality, Element element, boolean derived,
                                    TypeNameWithCardinality nested, List<TypeNameWithCardinality> keys) {
        this.typeName = typeName;
        this.cardinality = cardinality;
        this.element = element;
        this.derived = derived;
        this.nested = nested;
        this.keys = keys;
    }

    /// Returns a function that applies the TypeNameVisitor, but also:
    /// <ol>
    /// <li>Checks if a type is an `Array`, `Iterable` or `Stream` to return the type argument with cardinality `"*"`</li>
    /// <li>Checks if a type is a `java.util.Map` to return the *value* type argument with cardinality `"*"`
    /// (and the *key* type argument in [#keys]). `Map` takes priority over `Iterable` for types that are both.</li>
    /// <li>Checks if a type is a Java 8 or Guava `Optional` object to return the type argument with cardinality `"0..1"`</li>
    /// <li>Unwraps element types recursively into the [#nested] candidate chain</li>
    /// <li>Otherwise, the name of the actual type is returned with cardinality `null`</li>
    /// </ol>
    ///
    /// @param typeUtils The type utils to use for supertype introspection (required).
    /// @return The function to return TypeName with cardinality for use in same-package references.
    static Function<TypeMirror, TypeNameWithCardinality> function(final Types typeUtils) {
        requireNonNull(typeUtils, "Type utils are <null>.");
        return type -> resolve(typeUtils, type, 0);
    }

    private static TypeNameWithCardinality resolve(Types typeUtils, TypeMirror type, int depth) {
        Container container = findContainer(typeUtils, type);
        if (container == null) return plain(type, depth > 0);
        Set<String> seen = new HashSet<>();
        seen.add(type.toString());
        return unwrap(typeUtils, container, null, 1, emptyList(), seen, depth);
    }

    private static TypeNameWithCardinality plain(TypeMirror type, boolean derived) {
        return new TypeNameWithCardinality(TypeNameVisitor.INSTANCE.visit(type), null, elementOf(type), derived, null, emptyList());
    }

    private static TypeNameWithCardinality unwrap(Types typeUtils, Container container, String outerCardinality,
                                                  int level, List<TypeNameWithCardinality> outerKeys,
                                                  Set<String> seen, int keyDepth) {
        final String cardinality = combine(outerCardinality, container.cardinality);
        List<TypeNameWithCardinality> keys = outerKeys;
        if (container.key != null && keyDepth < MAX_DEPTH) {
            keys = new ArrayList<>(outerKeys);
            keys.add(resolve(typeUtils, bound(container.key), keyDepth + 1));
            keys = unmodifiableList(keys);
        }
        final TypeMirror elementType = bound(container.element);
        final boolean derived = container.isMap || level > 1 || !outerKeys.isEmpty();
        TypeNameWithCardinality nested = null;
        if (level < MAX_DEPTH && seen.add(elementType.toString())) {
            Container inner = findContainer(typeUtils, elementType);
            if (inner != null) nested = unwrap(typeUtils, inner, cardinality, level + 1, keys, seen, keyDepth);
        }
        return new TypeNameWithCardinality(
                TypeNameVisitor.INSTANCE.visit(elementType), cardinality, elementOf(elementType), derived, nested, keys);
    }

    /// Find the 'container' kind of a type: array, Map, Iterable, Stream or Optional.
    ///
    /// The whole supertype hierarchy is walked; a `java.util.Map` supertype takes priority, otherwise the
    /// first other container in breadth-first order is used. Raw types (or an unexpected number of type
    /// arguments) are not unwrapped.
    private static Container findContainer(Types typeUtils, TypeMirror type) {
        if (type instanceof ArrayType) {
            return new Container(((ArrayType) type).getComponentType(), "*", null, false);
        } else if (!(type instanceof DeclaredType)) {
            return null;
        }
        Container other = null;
        Queue<TypeMirror> superTypes = new ArrayDeque<>(singleton(type));
        Set<String> checkedTypes = new HashSet<>();
        while (!superTypes.isEmpty()) {
            TypeMirror superType = superTypes.poll();
            String qName = TypeNameVisitor.INSTANCE.visit(superType).qualified;
            if (checkedTypes.add(qName)) { // Don't reiterate
                Container found = knownContainer(qName, superType);
                if (found != null && found.isMap) return found;
                if (found != null && other == null) other = found;
                superTypes.addAll(typeUtils.directSupertypes(superType));
            }
        }
        return other;
    }

    private static Container knownContainer(String qName, TypeMirror type) {
        if (!(type instanceof DeclaredType)) return null;
        final List<? extends TypeMirror> args = ((DeclaredType) type).getTypeArguments();
        if ("java.util.Map".equals(qName)) {
            return args.size() == 2 ? new Container(args.get(1), "*", args.get(0), true) : null;
        }
        final String cardinality;
        if ("java.util.Optional".equals(qName) || "com.google.common.base.Optional".equals(qName)) {
            cardinality = "0..1";
        } else if ("java.lang.Iterable".equals(qName) || "java.util.stream.Stream".equals(qName)) {
            cardinality = "*";
        } else {
            return null;
        }
        // The 'iterable' and 'optional' types are DeclaredTypes with a single TypeArgument.
        return args.size() == 1 ? new Container(args.get(0), cardinality, null, false) : null;
    }

    /// Wildcards are replaced by their bound (`? extends X` and `? super X` both become `X`).
    private static TypeMirror bound(TypeMirror type) {
        if (type instanceof WildcardType) {
            WildcardType wildcard = (WildcardType) type;
            if (wildcard.getExtendsBound() != null) return wildcard.getExtendsBound();
            if (wildcard.getSuperBound() != null) return wildcard.getSuperBound();
        }
        return type;
    }

    private static Element elementOf(TypeMirror type) {
        return type instanceof DeclaredType ? ((DeclaredType) type).asElement() : null;
    }

    private static String combine(String outer, String inner) {
        if ("*".equals(outer) || "*".equals(inner)) return "*";
        return outer != null ? outer : inner;
    }

    private static final class Container {
        private final TypeMirror element;
        private final String cardinality;
        private final TypeMirror key;
        private final boolean isMap;

        private Container(TypeMirror element, String cardinality, TypeMirror key, boolean isMap) {
            this.element = element;
            this.cardinality = cardinality;
            this.key = key;
            this.isMap = isMap;
        }
    }
}
