package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Flight;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The unit tests of {@link FlightRepository}.
 */
@AddBeanClasses(FlightRepository_.class)
class FlightRepository_Test
        extends _Repository_Test<FlightRepository, Flight, Integer> {

    FlightRepository_Test() {
        super(FlightRepository.class, Flight.class, Integer.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    private static OffsetDateTime utc(final int year, final int month, final int day, final int hour) {
        return OffsetDateTime.of(year, month, day, hour, 0, 0, 0, ZoneOffset.UTC);
    }

    private Flight persistFlight(final EntityManager em, final String routeNo, final OffsetDateTime departure) {
        final var flight = ObjectRandomizerUtils.newRandomizedInstanceOf(Flight.class).orElseThrow();
        flight.setRouteNo(routeNo);
        flight.setScheduledDeparture(departure);
        flight.setScheduledArrival(departure.plusHours(2));
        em.persist(flight);
        return flight;
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_FLIGHTS_B_01: listByRouteNoAndScheduledDepartureBetween(routeNo, from, to)")
    @Nested
    class ListByRouteNoAndScheduledDepartureBetweenTest {

        // two of PG0001 in October, one of it in November, one of PG0002 in October
        private Integer[] persist() {
            return entityManagerFactory.callInTransaction(em -> new Integer[] {
                    persistFlight(em, "PG0001", utc(2031, 10, 20, 9)).getFlightId(),
                    persistFlight(em, "PG0001", utc(2031, 10, 1, 0)).getFlightId(),
                    persistFlight(em, "PG0001", utc(2031, 11, 1, 0)).getFlightId(),
                    persistFlight(em, "PG0002", utc(2031, 10, 5, 9)).getFlightId()
            });
        }

        @Test
        void __returnsFlightsOfTheRouteInThePeriod() {
            final var ids = persist();
            final var list = getRepositoryInstance().listByRouteNoAndScheduledDepartureBetween(
                    "PG0001", utc(2031, 10, 1, 0), utc(2031, 11, 1, 0));
            assertThat(list).extracting(Flight::getFlightId).containsExactlyInAnyOrder(ids[0], ids[1]);
        }

        @Test
        void __orderedByScheduledDeparture() {
            final var ids = persist();
            final var list = getRepositoryInstance().listByRouteNoAndScheduledDepartureBetween(
                    "PG0001", utc(2031, 10, 1, 0), utc(2031, 11, 1, 0));
            assertThat(list).extracting(Flight::getFlightId).containsExactly(ids[1], ids[0]);
        }

        @Test
        void __emptyWhenNoneMatches() {
            assertThat(getRepositoryInstance().listByRouteNoAndScheduledDepartureBetween(
                    "PG0001", utc(2031, 10, 1, 0), utc(2031, 11, 1, 0))).isEmpty();
        }

        // a @Query parameter is bound as it is, unchecked; a comparison with NULL matches nothing
        @Test
        void __nullArguments_matchNothing() {
            persist();
            final var repository = getRepositoryInstance();
            final var from = utc(2031, 10, 1, 0);
            final var to = utc(2031, 11, 1, 0);
            assertThat(repository.listByRouteNoAndScheduledDepartureBetween(null, from, to)).isEmpty();
            assertThat(repository.listByRouteNoAndScheduledDepartureBetween("PG0001", null, to)).isEmpty();
            assertThat(repository.listByRouteNoAndScheduledDepartureBetween("PG0001", from, null)).isEmpty();
        }
    }

    @DisplayName("DEMODB_FLIGHTS_I_01: selectFlightCountsByMonth(pageRequest)")
    @Nested
    class SelectFlightCountsByMonthTest {

        // two in October 2031, one in November 2031, one in January 2032
        private void persist() {
            entityManagerFactory.runInTransaction(em -> {
                persistFlight(em, "PG0001", utc(2031, 11, 15, 9));
                persistFlight(em, "PG0001", utc(2031, 10, 31, 23));
                persistFlight(em, "PG0001", utc(2032, 1, 1, 0));
                persistFlight(em, "PG0001", utc(2031, 10, 1, 0));
            });
        }

        @Test
        void __returnsMonthsWithCounts() {
            persist();
            final var page = getRepositoryInstance().selectFlightCountsByMonth(PageRequest.ofSize(10));
            assertThat(page.content())
                    .extracting(FlightRepository.MonthFlightCount::year, FlightRepository.MonthFlightCount::month,
                                FlightRepository.MonthFlightCount::flights)
                    .containsExactly(tuple(2031, 10, 2L), tuple(2031, 11, 1L), tuple(2032, 1, 1L));
        }

        @Test
        void __paged() {
            persist();
            final var first = getRepositoryInstance().selectFlightCountsByMonth(PageRequest.ofSize(2));
            assertThat(first.content()).hasSize(2);
            assertThat(first.totalElements()).isEqualTo(3L);
            final var second = getRepositoryInstance().selectFlightCountsByMonth(first.nextPageRequest());
            assertThat(second.content())
                    .extracting(FlightRepository.MonthFlightCount::year, FlightRepository.MonthFlightCount::month)
                    .containsExactly(tuple(2032, 1));
        }

        @Test
        void __nullPageRequest_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().selectFlightCountsByMonth(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    @Inject
    private EntityManagerFactory entityManagerFactory;
}
