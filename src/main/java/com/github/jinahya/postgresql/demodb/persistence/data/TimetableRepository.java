package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Timetable;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * A repository for {@link Timetable}, the {@code timetable} view of the demonstration database.
 * <p>
 * The exercise methods answer the exercises of upstream's {@code EXERCISES.asciidoc} on the
 * {@code timetable} view, one method per exercise, each named in its javadoc by the exercise's id.
 */
@Repository
public interface TimetableRepository
        extends _Repository<Timetable, Integer>,
                CrudRepository<Timetable, Integer> {

    // ----------------------------------------------------------------------------------- exercises

    /**
     * Lists the flights from the specified airport scheduled to depart in the specified period
     * ({@code DEMODB_TIMETABLE_B_01}, which asks for {@code SVO} in the 24 hours from
     * {@code bookings.now()}, {@code 2025-12-01 00:00 UTC}, and orders by
     * {@code scheduledDeparture}).
     * <p>
     * The exercise's {@code now} is the database's; the caller passes it, and the end of the day
     * after it. A {@code @Query} parameter is bound unchecked; a {@code null} one matches nothing.
     *
     * @param departureAirport the code of the airport of departure.
     * @param from             the start of the period, inclusive.
     * @param to               the end of the period, exclusive.
     * @return a list of the flights from the airport in the period, the earliest first.
     */
    @Query("""
            where departureAirport = :departureAirport
              and scheduledDeparture >= :from
              and scheduledDeparture < :to
            order by scheduledDeparture""")
    List<Timetable> listByDepartureAirportAndScheduledDepartureBetween(
            @Param("departureAirport") String departureAirport,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    /**
     * Counts the flights which depart on another local day than the UTC one
     * ({@code DEMODB_TIMETABLE_I_01}).
     * <p>
     * The exercise compares the dates; two dates at most a day apart differ exactly when their
     * days of the month do, so the days are compared, {@code EXTRACT} being what JPQL has. The UTC
     * day of the departure is the session's, which the test units set to UTC.
     *
     * @return the number of flights departing on another local day than the UTC one.
     */
    @Query("""
            select count(t)
            from Timetable t
            where extract(day from t.scheduledDepartureLocal) <> extract(day from t.scheduledDeparture)""")
    long countDepartingOnAnotherLocalDay();
}
