package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Flight;
import jakarta.data.page.PageRequest;
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
 * The integration tests of {@link FlightRepository}.
 * <p>
 * The expected rows are the ones upstream's {@code flights_exercises_0N_*.sql} give.
 */
@AddBeanClasses(FlightRepository_.class)
class FlightRepository_IT
        extends _Repository_IT<FlightRepository, Flight, Integer> {

    private static final OffsetDateTime OCTOBER = OffsetDateTime.of(2025, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private static final OffsetDateTime NOVEMBER = OffsetDateTime.of(2025, 11, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    FlightRepository_IT() {
        super(FlightRepository.class, Flight.class, Integer.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_FLIGHTS_B_01: listByRouteNoAndScheduledDepartureBetween(routeNo, from, to)")
    @Nested
    class ListByRouteNoAndScheduledDepartureBetweenTest {

        @Test
        void __returnsTheTwentySixFlightsOfPg0001InOctober() {
            final var list = getRepositoryInstance().listByRouteNoAndScheduledDepartureBetween(
                    "PG0001", OCTOBER, NOVEMBER);
            assertThat(list).hasSize(26).allSatisfy(f -> {
                assertThat(f.getRouteNo()).isEqualTo("PG0001");
                assertThat(f.getScheduledDeparture()).isAfterOrEqualTo(OCTOBER).isBefore(NOVEMBER);
            });
            assertThat(list).extracting(Flight::getFlightId)
                    .startsWith(56, 420, 595, 786, 962, 1145, 1319, 1682, 1858, 2049);
        }

        @Test
        void __orderedByScheduledDeparture() {
            final var list = getRepositoryInstance().listByRouteNoAndScheduledDepartureBetween(
                    "PG0001", OCTOBER, NOVEMBER);
            assertThat(list).extracting(Flight::getScheduledDeparture).isSorted();
        }

        @Test
        void __nullArguments_matchNothing() {
            final var repository = getRepositoryInstance();
            assertThat(repository.listByRouteNoAndScheduledDepartureBetween(null, OCTOBER, NOVEMBER)).isEmpty();
            assertThat(repository.listByRouteNoAndScheduledDepartureBetween("PG0001", null, NOVEMBER)).isEmpty();
            assertThat(repository.listByRouteNoAndScheduledDepartureBetween("PG0001", OCTOBER, null)).isEmpty();
        }
    }

    @DisplayName("DEMODB_FLIGHTS_I_01: selectFlightCountsByMonth(pageRequest)")
    @Nested
    class SelectFlightCountsByMonthTest {

        @Test
        void __returnsTheFourMonths() {
            final var page = getRepositoryInstance().selectFlightCountsByMonth(PageRequest.ofSize(10));
            assertThat(page.content())
                    .extracting(FlightRepository.MonthFlightCount::year, FlightRepository.MonthFlightCount::month,
                                FlightRepository.MonthFlightCount::flights)
                    .containsExactly(tuple(2025, 10, 5580L), tuple(2025, 11, 5467L), tuple(2025, 12, 5539L),
                                     tuple(2026, 1, 5172L));
        }

        @Test
        void __paged() {
            final var first = getRepositoryInstance().selectFlightCountsByMonth(PageRequest.ofSize(3));
            assertThat(first.content()).hasSize(3);
            assertThat(first.totalElements()).isEqualTo(4L);
            final var second = getRepositoryInstance().selectFlightCountsByMonth(first.nextPageRequest());
            assertThat(second.content()).extracting(FlightRepository.MonthFlightCount::flights).containsExactly(5172L);
        }

        @Test
        void __nullPageRequest_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().selectFlightCountsByMonth(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
