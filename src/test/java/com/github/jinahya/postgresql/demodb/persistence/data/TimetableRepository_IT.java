package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Timetable;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The integration tests of {@link TimetableRepository}.
 * <p>
 * The expected rows are the ones upstream's {@code timetable_exercises_0N_*.sql} give.
 */
@AddBeanClasses(TimetableRepository_.class)
class TimetableRepository_IT
        extends _Repository_IT<TimetableRepository, Timetable, Integer> {

    /**
     * The database's {@code bookings.now()}.
     */
    private static final OffsetDateTime NOW = OffsetDateTime.of(2025, 12, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    TimetableRepository_IT() {
        super(TimetableRepository.class, Timetable.class, Integer.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_TIMETABLE_B_01: listByDepartureAirportAndScheduledDepartureBetween(departureAirport, from, to)")
    @Nested
    class ListByDepartureAirportAndScheduledDepartureBetweenTest {

        @Test
        void __returnsTheFiveFlightsFromSheremetyevoInTheNextDay() {
            final var list = getRepositoryInstance().listByDepartureAirportAndScheduledDepartureBetween(
                    "SVO", NOW, NOW.plusHours(24));
            assertThat(list)
                    .extracting(Timetable::getFlightId, Timetable::getRouteNo, Timetable::getArrivalAirport,
                                Timetable::getScheduledDepartureLocal, Timetable::getStatus)
                    .containsExactly(
                            tuple(11120, "PG0012", "UFA", LocalDateTime.of(2025, 12, 1, 13, 55), "On Time"),
                            tuple(11121, "PG0034", "SVX", LocalDateTime.of(2025, 12, 1, 14, 5), "On Time"),
                            tuple(11132, "PG0177", "AER", LocalDateTime.of(2025, 12, 1, 15, 5), "On Time"),
                            tuple(11211, "PG0018", "KZN", LocalDateTime.of(2025, 12, 2, 2, 5), "On Time"),
                            tuple(11216, "PG0106", "LED", LocalDateTime.of(2025, 12, 2, 2, 40), "On Time"));
        }

        @Test
        void __orderedByScheduledDeparture() {
            final var list = getRepositoryInstance().listByDepartureAirportAndScheduledDepartureBetween(
                    "SVO", NOW, NOW.plusHours(24));
            assertThat(list).extracting(Timetable::getScheduledDeparture).isSorted();
        }

        @Test
        void __nullArguments_matchNothing() {
            final var repository = getRepositoryInstance();
            final var to = NOW.plusHours(24);
            assertThat(repository.listByDepartureAirportAndScheduledDepartureBetween(null, NOW, to)).isEmpty();
            assertThat(repository.listByDepartureAirportAndScheduledDepartureBetween("SVO", null, to)).isEmpty();
            assertThat(repository.listByDepartureAirportAndScheduledDepartureBetween("SVO", NOW, null)).isEmpty();
        }
    }

    @DisplayName("DEMODB_TIMETABLE_I_01: countDepartingOnAnotherLocalDay()")
    @Nested
    class CountDepartingOnAnotherLocalDayTest {

        @Test
        void __countsFourThousandFiveHundredNinetySeven() {
            assertThat(getRepositoryInstance().countDepartingOnAnotherLocalDay()).isEqualTo(4597L);
        }
    }
}
