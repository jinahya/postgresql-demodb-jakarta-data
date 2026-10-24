package com.github.jinahya.postgresql.demodb.persistence.data;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceUnit;
import org.hibernate.StatelessSession;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.jboss.weld.junit5.auto.EnableAutoWeld;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/**
 * The root of the repository tests, in a CDI container, Weld SE, as the generated repositories run
 * in an application server: the container creates the {@code <Repository>_} class through its
 * {@code @Inject} constructor, and the class opens its own session.
 * <p>
 * A concrete test adds the generated class and names its types:
 * <pre>{@code
 * @AddBeanClasses(ActorRepository_.class)
 * class ActorRepository_Test
 *         extends _Repository_Test<ActorRepository, Actor, Integer> {
 *
 *     ActorRepository_Test() {
 *         super(ActorRepository.class, Actor.class, Integer.class);
 *     }
 * }
 * }</pre>
 * The unit base it extends adds, with an {@link AddBeanClasses @AddBeanClasses} of its own, the
 * producer of the factory, which the generated class takes from a
 * {@link PersistenceUnit @PersistenceUnit} field; {@link __WeldJpaInjectionServices} fills that
 * field.
 * <p>
 * The repository is not injected as an {@code R} field: an injection point whose type is a type
 * variable is a definition error (WELD-001407), even when the subclass binds it. The base injects
 * an {@link Instance} instead and selects the {@link #repositoryClass} from it, once per test.
 * <p>
 * One container per test method, the default of {@link EnableAutoWeld @EnableAutoWeld}: each test
 * has its own factory, and so, on {@code demonstration-test}, an empty schema of its own. The repository
 * is a {@code @Dependent} bean of the test instance, with its own {@link StatelessSession}, closed
 * when the container shuts down. A stateless session has no persistence context: each repository
 * call goes to the database, an entity it returns is detached, and a write needs a transaction of
 * the session's own ({@code session(getRepositoryInstance()).inTransaction(s -> ...)}).
 *
 * @param <R> the type of the repository under test.
 * @param <T> the type of the entity of the repository.
 * @param <K> the type of the entity's id.
 * @see _Repository_Test
 * @see _Repository_IT
 */
@EnableAutoWeld
abstract class __Repository_TestBase<R extends _Repository<T, K>, T, K> {

    /**
     * Returns the session the specified repository opened, through the {@code public session()}
     * accessor every generated {@code <Repository>_} class has.
     *
     * @param repository the repository, an instance of a generated {@code <Repository>_} class.
     * @return the session of the repository.
     */
    static StatelessSession session(final _Repository<?, ?> repository) {
        Objects.requireNonNull(repository, "repository is null");
        try {
            return (StatelessSession) repository.getClass().getMethod("session").invoke(repository);
        } catch (final NoSuchMethodException | IllegalAccessException
                       | InvocationTargetException e) {
            throw new RuntimeException("failed to get the session of " + repository, e);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance for the specified repository and its types.
     *
     * @param repositoryClass the repository under test.
     * @param entityClass     the entity of the repository.
     * @param keyClass        the type of the entity's id.
     */
    __Repository_TestBase(final Class<R> repositoryClass, final Class<T> entityClass,
                          final Class<K> keyClass) {
        super();
        this.repositoryClass = Objects.requireNonNull(repositoryClass, "repositoryClass is null");
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass is null");
        this.keyClass = Objects.requireNonNull(keyClass, "keyClass is null");
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Returns the repository under test, a bean the container creates on the first call of the
     * current test.
     *
     * @return the repository under test.
     */
    R getRepositoryInstance() {
        if (repositoryInstance == null) {
            repositoryInstance = instance.select(repositoryClass).get();
        }
        return repositoryInstance;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * The repository interface under test.
     */
    final Class<R> repositoryClass;

    /**
     * The entity of the repository.
     */
    final Class<T> entityClass;

    /**
     * The type of the entity's id.
     */
    final Class<K> keyClass;

    // -----------------------------------------------------------------------------------------------------------------

    @Inject
    private Instance<Object> instance;

    private R repositoryInstance;
}
