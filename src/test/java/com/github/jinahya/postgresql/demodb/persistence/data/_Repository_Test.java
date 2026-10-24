package com.github.jinahya.postgresql.demodb.persistence.data;

import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The base of the unit tests of a repository, on the {@code demonstration-test} unit: an in-memory
 * H2 in PostgreSQL mode, whose schema Hibernate generates from the entities and drops again when
 * the factory closes. The views of the database are tables there, since each is an entity.
 * <p>
 * The database starts empty, so a test inserts what it queries.
 *
 * @param <R> the type of the repository under test.
 * @param <T> the type of the entity of the repository.
 * @param <K> the type of the entity's id.
 * @see _Repository_IT
 */
@AddBeanClasses(_Repository_Test.EntityManagerFactoryProducer.class)
abstract class _Repository_Test<R extends _Repository<T, K>, T, K>
        extends __Repository_TestBase<R, T, K> {

    static final String PERSISTENCE_UNIT_NAME = "demonstration-test";

    /**
     * Produces the factory of the {@code demonstration-test} unit, and closes it when the container shuts
     * down.
     * <p>
     * The scope is {@link Singleton @Singleton}, a pseudo-scope, not
     * {@code @ApplicationScoped}: a normal scope injects a client proxy, and the generated
     * repository's {@code unwrap(SessionFactory.class)} on that proxy fails with a
     * {@link ClassCastException}.
     */
    static class EntityManagerFactoryProducer {

        @Produces
        @Singleton
        EntityManagerFactory produceEntityManagerFactory() {
            return Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        }

        void disposeEntityManagerFactory(
                @Disposes final EntityManagerFactory entityManagerFactory) {
            entityManagerFactory.close();
        }
    }

    _Repository_Test(final Class<R> repositoryClass, final Class<T> entityClass,
                     final Class<K> keyClass) {
        super(repositoryClass, entityClass, keyClass);
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("findAll()")
    @Nested
    class FindAllTest {

        /**
         * Runs the query against the schema Hibernate generated, with no data: only the SQL is
         * checked, which {@code org.hibernate.SQL} logs.
         */
        @DisplayName("runs its SQL")
        @Test
        void __sql() {
            assertThatCode(() -> {
                try (var stream = getRepositoryInstance().findAll()) {
                    stream.forEach(e -> {
                    });
                }
            }).doesNotThrowAnyException();
        }
    }
}
