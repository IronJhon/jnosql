/*
 *  Copyright (c) 2019 Otávio Santana and others
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.jnosql.artemis.graph;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.jnosql.artemis.Page;
import org.jnosql.artemis.Pagination;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;

/**
 * The {@link Page} implementation to Graph database
 *
 * @param <T> the entity type
 */
public final class GraphPage<T> implements Page<T> {

    private final Pagination pagination;

    private final GraphConverter converter;

    private final GraphTraversal<?, ?> graphTraversal;

    private final List<T> entities;


    private GraphPage(Pagination pagination, GraphConverter converter, GraphTraversal<?, ?> graphTraversal, List<T> entities) {
        this.pagination = pagination;
        this.converter = converter;
        this.graphTraversal = graphTraversal;
        this.entities = entities;
    }

    @Override
    public Pagination getPagination() {
        return pagination.unmodifiable();
    }

    @Override
    public Page<T> next() {
        return GraphPage.of(pagination.next(), converter, graphTraversal);
    }

    @Override
    public List<T> getContent() {
        return entities;
    }

    @Override
    public <C extends Collection<T>> C getContent(Supplier<C> collectionFactory) {
        requireNonNull(collectionFactory, "collectionFactory is required");
        return entities.stream().collect(toCollection(collectionFactory));
    }

    @Override
    public Stream<T> get() {
        return entities.stream();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GraphPage<?> graphPage = (GraphPage<?>) o;
        return Objects.equals(pagination, graphPage.pagination) &&
                Objects.equals(graphTraversal, graphPage.graphTraversal) &&
                Objects.equals(entities, graphPage.entities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pagination, graphTraversal, entities);
    }

    @Override
    public String toString() {
        return "GraphPage{" +
                "pagination=" + pagination +
                ", graphTraversal=" + graphTraversal +
                ", entities=" + entities +
                '}';
    }

    /**
     * Creates a {@link Page} instance to Graph
     *
     * @param pagination     the pagination
     * @param converter      the converter
     * @param graphTraversal the traversal
     * @param <T>            the type
     * @return a new {@link Page} instance
     * @throws NullPointerException when there are null parameters
     */
    public static <T> Page<T> of(Pagination pagination, GraphConverter converter,
                                 GraphTraversal<?, ?> graphTraversal) {

        Objects.requireNonNull(pagination, "pagination is required");
        Objects.requireNonNull(converter, "converter is required");
        Objects.requireNonNull(graphTraversal, "graphTraversal is required");
        graphTraversal.skip(pagination.getSkip());
        return new GraphPage<>(pagination, converter, graphTraversal,
                (List<T>) graphTraversal
                        .next((int) pagination.getLimit()).stream()
                        .map(v -> converter.toEntity((Vertex) v))
                        .collect(Collectors.toList()));
    }

    /**
     * Creates a {@link Page} instance to Graph
     *
     * @param pagination     the pagination
     * @param converter      the converter
     * @param graphTraversal the traversal
     * @param entities       the entities at the first interaction
     * @param <T>            the type
     * @return a new {@link Page} instance
     * @throws NullPointerException when there are null parameters
     */
    public static <T> Page<T> of(Pagination pagination, GraphConverter converter,
                                 GraphTraversal<?, ?> graphTraversal, List<T> entities) {

        Objects.requireNonNull(pagination, "pagination is required");
        Objects.requireNonNull(converter, "converter is required");
        Objects.requireNonNull(graphTraversal, "graphTraversal is required");
        Objects.requireNonNull(entities, "entities is required");
        return new GraphPage<>(pagination, converter, graphTraversal, entities);
    }
}
