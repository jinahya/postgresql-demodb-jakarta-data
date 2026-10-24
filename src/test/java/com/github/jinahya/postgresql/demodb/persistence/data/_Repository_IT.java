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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The base of the integration tests of a repository, on the {@code demonstration-it} unit: the
 * demonstration database upstream's {@code _docker_compose_up.sh} brings up on
 * {@code localhost:5432}.
 * <p>
 * The sample data is the fixture. A test reads the rows that are there, and anything it writes it
 * does inside a transaction it rolls back.
 * <p>
 * Run with {@code -Pfailsafe}; see {@code _mvn_verify.sh}.
 *
 * @param <R> the type of the repository under test.
 * @param <T> the type of the entity of the repository.
 * @param <K> the type of the entity's id.
 * @see _Repository_Test
 */
@AddBeanClasses(_Repository_IT.EntityManagerFactoryProducer.class)
abstract class _Repository_IT<R extends _Repository<T, K>, T, K>
        extends __Repository_TestBase<R, T, K> {

    static final String PERSISTENCE_UNIT_NAME = "demonstration-it";

    /**
     * Produces the factory of the {@code demonstration-it} unit, and closes it when the container shuts
     * down.
     *
     * @see _Repository_Test.EntityManagerFactoryProducer
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

    _Repository_IT(final Class<R> repositoryClass, final Class<T> entityClass,
                   final Class<K> keyClass) {
        super(repositoryClass, entityClass, keyClass);
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("findAll()")
    @Nested
    class FindAllTest {

        @DisplayName("finds the sample rows, all of the entity type")
        @Test
        void __notEmpty() {
            try (var stream = getRepositoryInstance().findAll()) {
                assertThat(stream)
                        .isNotEmpty()
                        .hasOnlyElementsOfType(entityClass);
            }
        }
    }
}
