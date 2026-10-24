package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.AirplaneData;
import com.github.jinahya.postgresql.demodb.persistence.AirportData;
import com.github.jinahya.postgresql.demodb.persistence.Route;
import com.github.jinahya.postgresql.demodb.persistence.RouteId;
import jakarta.data.page.PageRequest;
import com.github.jinahya.object.randomizer.ObjectRandomizerUtils;
import com.github.jinahya.persistence.test.util.EntityPersisterUtils;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The unit tests of {@link RouteRepository}.
 */
@AddBeanClasses(RouteRepository_.class)
class RouteRepository_Test
        extends _Repository_Test<RouteRepository, Route, RouteId> {

    RouteRepository_Test() {
        super(RouteRepository.class, Route.class, RouteId.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Persists a randomized route of the specified number, from the specified airport, valid for the specified
     * month.
     */
    private Route persistRoute(final EntityManager em, final String routeNo, final AirportData departure,
                               final YearMonth month) {
        final var route = ObjectRandomizerUtils.newRandomizedInstanceOf(Route.class).orElseThrow();
        final var validity = "[" + month.atDay(1) + " 00:00:00+00," + month.plusMonths(1).atDay(1) + " 00:00:00+00)";
        route.setRouteNo(routeNo);
        route.setValidity(validity);
        route.setId(RouteId.of(routeNo, validity));
        route.setDepartureAirportData(departure);
        route.setArrivalAirportData(EntityPersisterUtils.newPersistedInstanceOf(em, AirportData.class));
        route.setAirplaneData(EntityPersisterUtils.newPersistedInstanceOf(em, AirplaneData.class));
        em.persist(route);
        return route;
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_ROUTES_B_01: listByDepartureAirport(departureAirport)")
    @Nested
    class ListByDepartureAirportTest {

        @Test
        void __returnsRoutesFromTheAirport() {
            final var departure = entityManagerFactory.callInTransaction(em -> {
                final var svo = EntityPersisterUtils.newPersistedInstanceOf(em, AirportData.class);
                final var other = EntityPersisterUtils.newPersistedInstanceOf(em, AirportData.class);
                persistRoute(em, "PG0001", svo, YearMonth.of(2031, 1));
                persistRoute(em, "PG0002", other, YearMonth.of(2031, 1));
                persistRoute(em, "PG0003", svo, YearMonth.of(2031, 1));
                return svo.getAirportCode();
            });
            final var list = getRepositoryInstance().listByDepartureAirport(departure);
            assertThat(list).extracting(Route::getRouteNo).containsExactlyInAnyOrder("PG0001", "PG0003");
            assertThat(list).allSatisfy(r -> assertThat(r.getDepartureAirportData().getAirportCode()).isEqualTo(departure));
        }

        @Test
        void __orderedByRouteNoThenValidity() {
            final var departure = entityManagerFactory.callInTransaction(em -> {
                final var svo = EntityPersisterUtils.newPersistedInstanceOf(em, AirportData.class);
                persistRoute(em, "PG0002", svo, YearMonth.of(2031, 2));
                persistRoute(em, "PG0001", svo, YearMonth.of(2031, 3));
                persistRoute(em, "PG0001", svo, YearMonth.of(2031, 1));
                return svo.getAirportCode();
            });
            final var list = getRepositoryInstance().listByDepartureAirport(departure);
            assertThat(list).extracting(Route::getRouteNo).containsExactly("PG0001", "PG0001", "PG0002");
            assertThat(list.get(0).getValidity()).isLessThan(list.get(1).getValidity());
        }

        @Test
        void __emptyWhenNoneMatches() {
            assertThat(getRepositoryInstance().listByDepartureAirport("svo")).isEmpty();
        }

        @Test
        void __nullDepartureAirport_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().listByDepartureAirport(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @DisplayName("DEMODB_ROUTES_I_01: listByRouteNo(routeNo, pageRequest)")
    @Nested
    class ListByRouteNoTest {

        // three periods of PG0001, out of order, and one of PG0002
        private void persist() {
            entityManagerFactory.runInTransaction(em -> {
                final var svo = EntityPersisterUtils.newPersistedInstanceOf(em, AirportData.class);
                persistRoute(em, "PG0001", svo, YearMonth.of(2031, 3));
                persistRoute(em, "PG0001", svo, YearMonth.of(2031, 1));
                persistRoute(em, "PG0002", svo, YearMonth.of(2031, 1));
                persistRoute(em, "PG0001", svo, YearMonth.of(2031, 2));
            });
        }

        @Test
        void __returnsPeriodsOfTheRoute() {
            persist();
            final var page = getRepositoryInstance().listByRouteNo("PG0001", PageRequest.ofSize(10));
            assertThat(page.content()).hasSize(3).allSatisfy(r -> assertThat(r.getRouteNo()).isEqualTo("PG0001"));
        }

        @Test
        void __orderedByValidity() {
            persist();
            final var page = getRepositoryInstance().listByRouteNo("PG0001", PageRequest.ofSize(10));
            assertThat(page.content()).extracting(Route::getValidity)
                    .containsExactly("[2031-01-01 00:00:00+00,2031-02-01 00:00:00+00)",
                                     "[2031-02-01 00:00:00+00,2031-03-01 00:00:00+00)",
                                     "[2031-03-01 00:00:00+00,2031-04-01 00:00:00+00)");
        }

        @Test
        void __paged() {
            persist();
            final var first = getRepositoryInstance().listByRouteNo("PG0001", PageRequest.ofSize(2));
            assertThat(first.content()).hasSize(2);
            assertThat(first.totalElements()).isEqualTo(3L);
            final var second = getRepositoryInstance().listByRouteNo("PG0001", first.nextPageRequest());
            assertThat(second.content()).extracting(Route::getValidity)
                    .containsExactly("[2031-03-01 00:00:00+00,2031-04-01 00:00:00+00)");
        }

        // a @Query parameter is bound as it is, unchecked; a comparison with NULL matches nothing
        @Test
        void __nullRouteNo_matchesNothing() {
            persist();
            assertThat(getRepositoryInstance().listByRouteNo(null, PageRequest.ofSize(10)).content()).isEmpty();
        }

        @Test
        void __nullPageRequest_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().listByRouteNo("PG0001", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    @Inject
    private EntityManagerFactory entityManagerFactory;
}
