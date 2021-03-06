/*
 *
 *  Copyright (c) 2017 Otávio Santana and others
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
 *
 */
package org.jnosql.diana.api.column.query;

import org.jnosql.diana.api.TypeReference;
import org.jnosql.diana.api.column.Column;
import org.jnosql.diana.api.column.ColumnEntity;
import org.jnosql.diana.api.column.ColumnFamilyManager;
import org.jnosql.diana.api.column.ColumnFamilyManagerAsync;
import org.jnosql.diana.api.column.ColumnObserverParser;
import org.jnosql.diana.api.column.ColumnPreparedStatement;
import org.jnosql.diana.api.column.ColumnPreparedStatementAsync;
import org.jnosql.diana.api.QueryException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateQueryParserTest {

    private UpdateQueryParser parser = new UpdateQueryParser();

    private ColumnFamilyManager manager = Mockito.mock(ColumnFamilyManager.class);
    private ColumnFamilyManagerAsync managerAsync = Mockito.mock(ColumnFamilyManagerAsync.class);
    private final ColumnObserverParser observer = new ColumnObserverParser() {
    };

    @ParameterizedTest(name = "Should parser the query {0}")
    @ValueSource(strings = {"update God (name = \"Diana\")"})
    public void shouldReturnParserQuery(String query) {
        ArgumentCaptor<ColumnEntity> captor = ArgumentCaptor.forClass(ColumnEntity.class);
        parser.query(query, manager, observer);
        Mockito.verify(manager).update(captor.capture());
        ColumnEntity entity = captor.getValue();


        assertEquals("God", entity.getName());
        assertEquals(Column.of("name", "Diana"), entity.find("name").get());
    }

    @ParameterizedTest(name = "Should parser the query {0}")
    @ValueSource(strings = {"update God (age = 30, name = \"Artemis\")"})
    public void shouldReturnParserQuery1(String query) {
        ArgumentCaptor<ColumnEntity> captor = ArgumentCaptor.forClass(ColumnEntity.class);
        parser.query(query, manager, observer);
        Mockito.verify(manager).update(captor.capture());
        ColumnEntity entity = captor.getValue();

        assertEquals("God", entity.getName());
        assertEquals(Column.of("name", "Artemis"), entity.find("name").get());
        assertEquals(Column.of("age", 30L), entity.find("age").get());
    }

    @ParameterizedTest(name = "Should parser the query {0}")
    @ValueSource(strings = {"update God (name = @name)"})
    public void shouldReturnParserQuery2(String query) {

        assertThrows(QueryException.class, () -> parser.query(query, manager, observer));
    }
    @ParameterizedTest(name = "Should parser the query {0}")
    @ValueSource(strings = {"update Person {\"name\":\"Ada Lovelace\"}"})
    public void shouldReturnParserQuery3(String query) {

        ArgumentCaptor<ColumnEntity> captor = ArgumentCaptor.forClass(ColumnEntity.class);

        parser.query(query, manager, observer);
        Mockito.verify(manager).update(captor.capture());
        ColumnEntity entity = captor.getValue();

        assertEquals("Person", entity.getName());
        assertEquals(Column.of("name", "Ada Lovelace"), entity.find("name").get());
    }


    @ParameterizedTest(name = "Should parser the query {0}")
    @ValueSource(strings = {"update Person {\"name\": \"Ada Lovelace\", \"age\": 12, \"sibling\":" +
            " [\"Ana\" ,\"Maria\"]," +
            " \"address\":{\"country\": \"United Kingdom\", \"city\": \"London\"}}"})
    public void shouldReturnParserQuery4(String query) {

        ArgumentCaptor<ColumnEntity> captor = ArgumentCaptor.forClass(ColumnEntity.class);

        parser.query(query, manager, observer);
        Mockito.verify(manager).update(captor.capture());
        ColumnEntity entity = captor.getValue();
        List<String> siblings = entity.find("sibling").get().get(new TypeReference<List<String>>() {
        });
        List<Column> address = entity.find("address").get().get(new TypeReference<List<Column>>() {
        });
        assertEquals("Person", entity.getName());
        assertEquals(Column.of("name", "Ada Lovelace"), entity.find("name").get());
        assertEquals(Column.of("age", BigDecimal.valueOf(12)), entity.find("age").get());
        assertThat(siblings, contains("Ana", "Maria"));
        assertThat(address, containsInAnyOrder(
                Column.of("country", "United Kingdom"),
                Column.of("city", "London")));
    }

    @ParameterizedTest(name = "Should parser the query {0}")
    @ValueSource(strings = {"update God (name = @name)"})
    public void shouldReturnErrorWhenDoesNotBindBeforeExecuteQuery(String query) {

        ColumnPreparedStatement prepare = parser.prepare(query, manager, observer);
        assertThrows(QueryException.class, prepare::getResultList);
    }


    @ParameterizedTest(name = "Should parser the query {0}")
    @ValueSource(strings = {"update God (name = @name)"})
    public void shouldExecutePrepareStatement(String query) {
        ArgumentCaptor<ColumnEntity> captor = ArgumentCaptor.forClass(ColumnEntity.class);
        ColumnPreparedStatement prepare = parser.prepare(query, manager, observer);
        prepare.bind("name", "Diana");
        prepare.getResultList();
        Mockito.verify(manager).update(captor.capture());
        ColumnEntity entity = captor.getValue();
        assertEquals("God", entity.getName());
        assertEquals(Column.of("name", "Diana"), entity.find("name").get());

    }

    @ParameterizedTest(name = "Should parser the query {0}")
    @ValueSource(strings = {"update God (name = @name)"})
    public void shouldReturnErrorWhenShouldUsePrepareStatementAsync(String query) {

        assertThrows(QueryException.class, () -> parser.queryAsync(query, managerAsync, s->{}, observer));
    }


    @ParameterizedTest(name = "Should parser the query {0}")
    @ValueSource(strings = {"update God (name = @name)"})
    public void shouldReturnErrorWhenDoesNotBindBeforeExecuteQueryAsync(String query) {

        ColumnPreparedStatementAsync prepare = parser.prepareAsync(query, managerAsync, observer);
        assertThrows(QueryException.class, () -> prepare.getResultList(s ->{}));
    }


    @ParameterizedTest(name = "Should parser the query {0}")
    @ValueSource(strings = {"update God (name = @name)"})
    public void shouldExecutePrepareStatementAsync(String query) {
        ArgumentCaptor<ColumnEntity> captor = ArgumentCaptor.forClass(ColumnEntity.class);
        ColumnPreparedStatementAsync prepare = parser.prepareAsync(query, managerAsync, observer);
        prepare.bind("name", "Diana");
        Consumer<List<ColumnEntity>> callBack = s -> {
        };
        prepare.getResultList(callBack);
        Mockito.verify(managerAsync).update(captor.capture(), Mockito.any(Consumer.class));
        ColumnEntity entity = captor.getValue();
        assertEquals("God", entity.getName());
        assertEquals(Column.of("name", "Diana"), entity.find("name").get());

    }
}