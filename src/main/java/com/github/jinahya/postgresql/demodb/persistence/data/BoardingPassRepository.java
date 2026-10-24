package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.BoardingPass;
import com.github.jinahya.postgresql.demodb.persistence.BoardingPassId;
import com.github.jinahya.postgresql.demodb.persistence.mapped.MappedBoardingPass_;
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
 * A repository for {@link BoardingPass}, the {@code boarding_passes} table of the demonstration
 * database.
 * <p>
 * The exercise methods answer the exercises of upstream's {@code EXERCISES.asciidoc} on the
 * {@code boarding_passes} table, one method per exercise, each named in its javadoc by the
 * exercise's id.
 */
@Repository
public interface BoardingPassRepository
        extends _Repository<BoardingPass, BoardingPassId>,
                CrudRepository<BoardingPass, BoardingPassId> {

    // ----------------------------------------------------------------------------------- exercises

    /**
     * A seat number and how many passes were issued for it; a row of
     * {@link #selectBoardedCountsBySeatNo(PageRequest)}.
     *
     * @param seatNo  the seat number.
     * @param boarded the number of boarding passes for the seat number.
     */
    record SeatBoardedCount(String seatNo, long boarded) {
    }

    /**
     * Lists the boarding passes of the specified flight, in boarding order
     * ({@code DEMODB_BOARDING_PASSES_B_01}, which asks for the flight {@code 1} and orders by
     * {@link MappedBoardingPass_#BOARDING_NO boardingNo}, a pass not yet boarded last, then by
     * {@link MappedBoardingPass_#TICKET_NO ticketNo}).
     *
     * @param flightId the id of the flight.
     * @return a list of the boarding passes of the flight, in boarding order.
     */
    @Find
    @OrderBy(MappedBoardingPass_.BOARDING_NO)
    @OrderBy(MappedBoardingPass_.TICKET_NO)
    List<BoardingPass> listByFlightId(@By(MappedBoardingPass_.FLIGHT_ID) Integer flightId);

    /**
     * Selects the seat numbers with how many passes were issued for each
     * ({@code DEMODB_BOARDING_PASSES_I_01}, which orders by {@code boarded} descending, then by
     * {@code seatNo}; the exercise's {@code LIMIT 5} is the first page of five).
     *
     * @param pageRequest the page to return.
     * @return a page of the seat numbers with their numbers of passes, the most first.
     */
    @Query("""
            select b.seatNo, count(b)
            from BoardingPass b
            group by b.seatNo
            order by count(b) desc, b.seatNo""")
    Page<SeatBoardedCount> selectBoardedCountsBySeatNo(PageRequest pageRequest);
}
