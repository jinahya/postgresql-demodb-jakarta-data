package com.github.jinahya.postgresql.demodb.persistence.data;

import jakarta.data.repository.CrudRepository;

/**
 * The base of every repository of an {@code @Entity} of this project, for whatever all of them
 * should offer.
 * <p>
 * Jakarta Data needs no counterpart of Spring Data's {@code @NoRepositoryBean}: only an interface
 * annotated {@link jakarta.data.repository.Repository @Repository} gets an implementation, and this
 * one is not annotated. A method declared here, such as a {@code @Find} or a {@code default}
 * method, is implemented in each generated {@code <Entity>Repository_} with {@code T} and {@code K}
 * bound to that repository's types.
 * <p>
 * A subinterface must <em>also</em> name {@link CrudRepository} directly, with its concrete types:
 * <pre>{@code
 * @Repository
 * public interface FlightRepository
 *         extends _Repository<Flight, Integer>,
 *                 CrudRepository<Flight, Integer> {
 * }
 * }</pre>
 * Without that second supertype, {@code hibernate-processor} cannot resolve the entity type of an
 * inherited lifecycle method through a type variable, and fails with "repository does not have a
 * well-defined primary entity type for inherited member 'deleteById'".
 * <p>
 * Once a query method is declared here, {@code hibernate-processor} also generates an empty static
 * metamodel class, {@code _Repository_}, for this interface. It is harmless.
 *
 * @param <T> the type of the entity.
 * @param <K> the type of the entity's id.
 */
public interface _Repository<T, K>
        extends CrudRepository<T, K> {

}
