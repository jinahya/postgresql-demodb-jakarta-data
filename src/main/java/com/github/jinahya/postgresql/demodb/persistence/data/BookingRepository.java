package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Booking;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * A repository for {@link Booking}, the {@code bookings} table of the demonstration database.
 * <p>
 * The exercise methods answer the exercises of upstream's {@code EXERCISES.asciidoc} on the
 * {@code bookings} table, one method per exercise, each named in its javadoc by the exercise's id.
 */
@Repository
public interface BookingRepository
        extends _Repository<Booking, String>,
                CrudRepository<Booking, String> {

    // ----------------------------------------------------------------------------------- exercises

    /**
     * A month, how many bookings were made in it, and how much they amount to; a row of
     * {@link #selectBookingCountsAndTotalsByMonth(PageRequest)}.
     *
     * @param year     the year.
     * @param month    the month of the year, {@code 1} to {@code 12}.
     * @param bookings the number of bookings made in the month.
     * @param total    the sum of their amounts.
     */
    record MonthBookingTotal(int year, int month, long bookings, BigDecimal total) {
    }

    /**
     * Lists the bookings made in the specified period ({@code DEMODB_BOOKINGS_B_01}, which asks
     * for the first minute of the data, {@code 2025-09-01 00:00} to {@code 00:01} UTC, and orders
     * by {@code bookDate}).
     * <p>
     * A {@code @Query} parameter is bound unchecked; a {@code null} one matches nothing.
     *
     * @param from the start of the period, inclusive.
     * @param to   the end of the period, exclusive.
     * @return a list of the bookings made in the period, the earliest first.
     */
    @Query("""
            where bookDate >= :from
              and bookDate < :to
            order by bookDate""")
    List<Booking> listByBookDateBetween(@Param("from") OffsetDateTime from,
                                        @Param("to") OffsetDateTime to);

    /**
     * Selects how many bookings were made in each month, and how much they amount to
     * ({@code DEMODB_BOOKINGS_I_01}, which orders by month).
     * <p>
     * The month is taken out of the booking date in the session's time zone, which the test units
     * set to UTC, as the exercise's {@code TO_CHAR} does in the container. The order is in the
     * query.
     *
     * @param pageRequest the page to return.
     * @return a page of the months with their numbers of bookings and their totals, the earliest
     *         first.
     */
    @Query("""
            select extract(year from b.bookDate), extract(month from b.bookDate),
                   count(b), sum(b.totalAmount)
            from Booking b
            group by extract(year from b.bookDate), extract(month from b.bookDate)
            order by extract(year from b.bookDate), extract(month from b.bookDate)""")
    Page<MonthBookingTotal> selectBookingCountsAndTotalsByMonth(PageRequest pageRequest);
}
