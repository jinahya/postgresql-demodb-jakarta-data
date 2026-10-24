/**
 * Jakarta Data repositories for the entities of the PostgreSQL Demonstration Database, which live one
 * package up: this project's own entities, each extending a mapped superclass of the upstream
 * {@code postgres-demonstration-persistence} artifact.
 * <p>
 * Every repository here is an interface annotated {@link jakarta.data.repository.Repository}, named
 * {@code <Entity>Repository}; its implementation, a {@code <Name>_} class in this package, is
 * generated at compile time by Hibernate Data Repositories and runs on a
 * {@link org.hibernate.StatelessSession}. Think of the interface as the DAO interface of a Java EE
 * 5/6 application, and of the generated class as its {@code DaoImpl}, written by the compiler
 * instead of by hand.
 * <p>
 * The generated class can be used two ways:
 * <ul>
 *   <li>under CDI, as a {@code @Dependent} bean: its {@code @Inject} constructor takes nothing,
 *   and it opens its own session from the {@code @PersistenceUnit} the container injects;</li>
 *   <li>without a container, through its {@code public} constructor taking a
 *   {@code StatelessSession}, which the caller opens from its own {@code EntityManagerFactory} and
 *   closes when done.</li>
 * </ul>
 * <p>
 * Each repository extends
 * {@link com.github.jinahya.postgresql.demodb.persistence.data._Repository}, for what all of them
 * share, and also {@link jakarta.data.repository.CrudRepository} directly, with its own entity and
 * id types; see {@code _Repository} for why both are needed.
 * <p>
 * No repository names a {@code dataStore}. The persistence unit is the caller's choice, not the
 * jar's: under CDI it is the default unit, and without a container it is whichever unit the caller
 * opened the session from.
 * <p>
 * The package is {@link NullMarked}: every parameter and return type here is non-null unless
 * marked {@code @Nullable}. A lookup that may find nothing returns an {@link java.util.Optional}
 * instead of a nullable entity.
 */
@NullMarked
package com.github.jinahya.postgresql.demodb.persistence.data;

import org.jspecify.annotations.NullMarked;
