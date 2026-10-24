package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.AirplaneData;
import com.github.jinahya.postgresql.demodb.persistence.Seat;
import com.github.jinahya.postgresql.demodb.persistence.SeatId;
import com.github.jinahya.postgresql.demodb.persistence.mapped.MappedSeat_;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.By;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;

/**
 * A repository for {@link Seat}, the {@code seats} table of the demonstration database.
 * <p>
 * The exercise methods answer the exercises of upstream's {@code EXERCISES.asciidoc} on the
 * {@code seats} table, one method per exercise, each named in its javadoc by the exercise's id.
 */
@Repository
public interface SeatRepository
        extends _Repository<Seat, SeatId>,
                CrudRepository<Seat, SeatId> {

    // ----------------------------------------------------------------------------------- exercises

    /**
     * An airplane and how many seats it has of each fare conditions; a row of
     * {@link #selectSeatCountsByFareConditions(PageRequest)}.
     *
     * @param airplane the airplane.
     * @param economy  the number of Economy seats.
     * @param comfort  the number of Comfort seats.
     * @param business the number of Business seats.
     * @param seats    the number of seats in all.
     */
    record AirplaneSeatCounts(AirplaneData airplane, long economy, long comfort, long business,
                              long seats) {
    }

    /**
     * Lists the seats of the specified airplane of the specified fare conditions
     * ({@code DEMODB_SEATS_B_01}, which asks for the Business seats of {@code 32N} and orders by
     * {@link MappedSeat_#SEAT_NO seatNo}).
     *
     * @param airplaneCode   the code of the airplane.
     * @param fareConditions the fare conditions.
     * @return a list of the seats, by seat number.
     */
    @Find
    @OrderBy(MappedSeat_.SEAT_NO)
    List<Seat> listByAirplaneCodeAndFareConditions(@By(MappedSeat_.AIRPLANE_CODE) String airplaneCode,
                                                   @By(MappedSeat_.FARE_CONDITIONS) String fareConditions);

    /**
     * Selects every airplane with how many seats it has of each fare conditions, and in all
     * ({@code DEMODB_SEATS_I_01}, which orders by the airplane's code).
     * <p>
     * The airplanes are left joined to their seats, so an airplane with none is a row of zeros;
     * the exercise's {@code FILTER} is a {@code CASE} summed, and the seats are counted by their
     * numbers, as the exercise counts them, since a count of the entity itself would count the
     * unmatched row of the join. The order is in the query.
     *
     * @param pageRequest the page to return.
     * @return a page of the airplanes with their seat counts, by code.
     */
    @Query("""
            select a,
                   sum(case when s.fareConditions = 'Economy' then 1 else 0 end),
                   sum(case when s.fareConditions = 'Comfort' then 1 else 0 end),
                   sum(case when s.fareConditions = 'Business' then 1 else 0 end),
                   count(s.seatNo)
            from AirplaneData a
                     left join Seat s on s.airplaneData = a
            group by a
            order by a.airplaneCode""")
    Page<AirplaneSeatCounts> selectSeatCountsByFareConditions(PageRequest pageRequest);
}
