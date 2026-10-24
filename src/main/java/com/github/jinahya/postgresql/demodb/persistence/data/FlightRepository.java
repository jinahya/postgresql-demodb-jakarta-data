package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Flight;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * A repository for {@link Flight}, the {@code flights} table of the demonstration database.
 * <p>
 * The exercise methods answer the exercises of upstream's {@code EXERCISES.asciidoc} on the
 * {@code flights} table, one method per exercise, each named in its javadoc by the exercise's id.
 */
@Repository
public interface FlightRepository
        extends _Repository<Flight, Integer>,
                CrudRepository<Flight, Integer> {

    // ----------------------------------------------------------------------------------- exercises

    /**
     * A month and how many flights were scheduled to depart in it; a row of
     * {@link #selectFlightCountsByMonth(PageRequest)}.
     *
     * @param year    the year.
     * @param month   the month of the year, {@code 1} to {@code 12}.
     * @param flights the number of flights scheduled in the month.
     */
    record MonthFlightCount(int year, int month, long flights) {
    }

    /**
     * Lists the flights of the specified route scheduled to depart in the specified period
     * ({@code DEMODB_FLIGHTS_B_01}, which asks for {@code PG0001} in October 2025 and orders by
     * {@code scheduledDeparture}).
     * <p>
     * A {@code @Query} parameter is bound unchecked; a {@code null} one matches nothing.
     *
     * @param routeNo the route number.
     * @param from    the start of the period, inclusive.
     * @param to      the end of the period, exclusive.
     * @return a list of the flights of the route in the period, the earliest first.
     */
    @Query("""
            where routeNo = :routeNo
              and scheduledDeparture >= :from
              and scheduledDeparture < :to
            order by scheduledDeparture""")
    List<Flight> listByRouteNoAndScheduledDepartureBetween(@Param("routeNo") String routeNo,
                                                           @Param("from") OffsetDateTime from,
                                                           @Param("to") OffsetDateTime to);

    /**
     * Selects how many flights were scheduled to depart in each month
     * ({@code DEMODB_FLIGHTS_I_01}, which orders by month).
     * <p>
     * The month is taken out of the scheduled departure in the session's time zone, which the
     * test units set to UTC, as the exercise's {@code TO_CHAR} does in the container. The order
     * is in the query.
     *
     * @param pageRequest the page to return.
     * @return a page of the months with their numbers of flights, the earliest first.
     */
    @Query("""
            select extract(year from f.scheduledDeparture), extract(month from f.scheduledDeparture),
                   count(f)
            from Flight f
            group by extract(year from f.scheduledDeparture), extract(month from f.scheduledDeparture)
            order by extract(year from f.scheduledDeparture), extract(month from f.scheduledDeparture)""")
    Page<MonthFlightCount> selectFlightCountsByMonth(PageRequest pageRequest);
}
