package com.github.jinahya.postgresql.demodb.persistence.data;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.jboss.weld.injection.spi.helpers.SimpleResourceReference;

/**
 * Fills a {@link PersistenceUnit @PersistenceUnit} field, which an application server fills and
 * Weld SE does not, with the {@link EntityManagerFactory} bean the container produces.
 * <p>
 * The generated {@code <Repository>_} class takes its factory from such a field, which is resource
 * injection: a CDI producer never reaches it, and without this service the field stays
 * {@code null}. Weld loads this class, as an additional
 * {@code org.jboss.weld.bootstrap.api.Service}, from
 * {@code META-INF/services/org.jboss.weld.bootstrap.api.Service}, so every container a test starts
 * with {@code @EnableAutoWeld} has it.
 * <p>
 * The unit name of the injection point is ignored: each test container holds exactly one factory,
 * the one its base class produces.
 */
public class __WeldJpaInjectionServices
        implements JpaInjectionServices {

    @Override
    public ResourceReferenceFactory<EntityManagerFactory> registerPersistenceUnitInjectionPoint(
            final InjectionPoint injectionPoint) {
        return () -> new SimpleResourceReference<>(
                CDI.current().select(EntityManagerFactory.class).get());
    }

    @Override
    public ResourceReferenceFactory<EntityManager> registerPersistenceContextInjectionPoint(
            final InjectionPoint injectionPoint) {
        throw new UnsupportedOperationException(
                "@PersistenceContext is not supported: " + injectionPoint);
    }

    @Override
    public void cleanup() {
    }
}
