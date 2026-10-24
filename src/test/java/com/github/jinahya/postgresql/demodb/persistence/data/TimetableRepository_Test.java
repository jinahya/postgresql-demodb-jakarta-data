package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Timetable;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The unit tests of {@link TimetableRepository}.
 * <p>
 * The view is a table on the in-memory unit, and its columns are read-only in the mapping, so a test inserts the
 * rows it queries with SQL.
 */
@AddBeanClasses(TimetableRepository_.class)
class TimetableRepository_Test
        extends _Repository_Test<TimetableRepository, Timetable, Integer> {

    TimetableRepository_Test() {
        super(TimetableRepository.class, Timetable.class, Integer.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    private static OffsetDateTime utc(final int year, final int month, final int day, final int hour) {
        return OffsetDateTime.of(year, month, day, hour, 0, 0, 0, ZoneOffset.UTC);
    }

    /**
     * Inserts a row from the specified airport, departing at the specified instant, which is the instant plus the
     * specified hours in local time, into the view's table, and returns the row read back.
     */
    private Timetable persistRow(final EntityManager em, final String departureAirport,
                                 final OffsetDateTime departure, final int localOffsetHours) {
        final var flightId = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        em.createNativeQuery("""
                                     insert into timetable (flight_id, route_no, departure_airport, arrival_airport, status,
                                                            airplane_code, scheduled_departure, scheduled_departure_local,
                                                            scheduled_arrival, scheduled_arrival_local)
                                     values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10)""")
                .setParameter(1, flightId)
                .setParameter(2, "pg0001")
                .setParameter(3, departureAirport)
                .setParameter(4, "led")
                .setParameter(5, "Scheduled")
                .setParameter(6, "32n")
                .setParameter(7, departure)
                .setParameter(8, departure.plusHours(localOffsetHours).toLocalDateTime())
                .setParameter(9, departure.plusHours(2))
                .setParameter(10, departure.plusHours(2 + localOffsetHours).toLocalDateTime())
                .executeUpdate();
        return em.find(Timetable.class, flightId);
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_TIMETABLE_B_01: listByDepartureAirportAndScheduledDepartureBetween(departureAirport, from, to)")
    @Nested
    class ListByDepartureAirportAndScheduledDepartureBetweenTest {

        private static final OffsetDateTime NOW = utc(2031, 12, 1, 0);

        // two from svo within the day, one from svo after it, one from elsewhere within it
        private Integer[] persist() {
            return entityManagerFactory.callInTransaction(em -> new Integer[] {
                    persistRow(em, "svo", NOW.plusHours(20), 3).getFlightId(),
                    persistRow(em, "svo", NOW, 3).getFlightId(),
                    persistRow(em, "svo", NOW.plusHours(24), 3).getFlightId(),
                    persistRow(em, "led", NOW.plusHours(1), 3).getFlightId()
            });
        }

        @Test
        void __returnsFlightsFromTheAirportInThePeriod() {
            final var ids = persist();
            final var list = getRepositoryInstance().listByDepartureAirportAndScheduledDepartureBetween(
                    "svo", NOW, NOW.plusHours(24));
            assertThat(list).extracting(Timetable::getFlightId).containsExactlyInAnyOrder(ids[0], ids[1]);
        }

        @Test
        void __orderedByScheduledDeparture() {
            final var ids = persist();
            final var list = getRepositoryInstance().listByDepartureAirportAndScheduledDepartureBetween(
                    "svo", NOW, NOW.plusHours(24));
            assertThat(list).extracting(Timetable::getFlightId).containsExactly(ids[1], ids[0]);
        }

        @Test
        void __emptyWhenNoneMatches() {
            assertThat(getRepositoryInstance().listByDepartureAirportAndScheduledDepartureBetween(
                    "svo", NOW, NOW.plusHours(24))).isEmpty();
        }

        // a @Query parameter is bound as it is, unchecked; a comparison with NULL matches nothing
        @Test
        void __nullArguments_matchNothing() {
            persist();
            final var repository = getRepositoryInstance();
            final var to = NOW.plusHours(24);
            assertThat(repository.listByDepartureAirportAndScheduledDepartureBetween(null, NOW, to)).isEmpty();
            assertThat(repository.listByDepartureAirportAndScheduledDepartureBetween("svo", null, to)).isEmpty();
            assertThat(repository.listByDepartureAirportAndScheduledDepartureBetween("svo", NOW, null)).isEmpty();
        }
    }

    @DisplayName("DEMODB_TIMETABLE_I_01: countDepartingOnAnotherLocalDay()")
    @Nested
    class CountDepartingOnAnotherLocalDayTest {

        @Test
        void __countsRowsWhoseLocalDayDiffers() {
            entityManagerFactory.runInTransaction(em -> {
                persistRow(em, "svo", utc(2031, 12, 1, 22), 3);   // local 2031-12-02: counted
                persistRow(em, "svo", utc(2031, 12, 1, 2), -3);   // local 2031-11-30: counted
                persistRow(em, "svo", utc(2031, 12, 1, 12), 3);   // local 2031-12-01: not
                persistRow(em, "svo", utc(2031, 12, 1, 12), 0);   // local 2031-12-01: not
            });
            assertThat(getRepositoryInstance().countDepartingOnAnotherLocalDay()).isEqualTo(2L);
        }

        @Test
        void __zeroWhenNone() {
            assertThat(getRepositoryInstance().countDepartingOnAnotherLocalDay()).isZero();
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    @Inject
    private EntityManagerFactory entityManagerFactory;
}
