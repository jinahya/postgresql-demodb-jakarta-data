package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Segment;
import com.github.jinahya.postgresql.demodb.persistence.SegmentId;
import com.github.jinahya.postgresql.demodb.persistence.mapped.MappedSegment_;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.By;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.math.BigDecimal;
import java.util.List;

/**
 * A repository for {@link Segment}, the {@code segments} table of the demonstration database.
 * <p>
 * The exercise methods answer the exercises of upstream's {@code EXERCISES.asciidoc} on the
 * {@code segments} table, one method per exercise, each named in its javadoc by the exercise's id.
 */
@Repository
public interface SegmentRepository
        extends _Repository<Segment, SegmentId>,
                CrudRepository<Segment, SegmentId> {

    // ----------------------------------------------------------------------------------- exercises

    /**
     * Fare conditions and the cheapest, the average and the dearest price paid for them; a row of
     * {@link #selectPricesByFareConditions(PageRequest)}.
     *
     * @param fareConditions the fare conditions.
     * @param cheapest       the lowest price.
     * @param average        the average price, as the provider computes it, unrounded.
     * @param dearest        the highest price.
     */
    record FareConditionsPrices(String fareConditions, BigDecimal cheapest, Double average,
                                BigDecimal dearest) {
    }

    /**
     * Lists the segments of the specified ticket ({@code DEMODB_SEGMENTS_B_01}, which asks for
     * {@code 0005432000000} and orders by {@link MappedSegment_#FLIGHT_ID flightId}).
     *
     * @param ticketNo the number of the ticket.
     * @return a list of the segments of the ticket, by flight id.
     */
    @Find
    @OrderBy(MappedSegment_.FLIGHT_ID)
    List<Segment> listByTicketNo(@By(MappedSegment_.TICKET_NO) String ticketNo);

    /**
     * Selects, for each fare conditions, the cheapest, the average and the dearest price
     * ({@code DEMODB_SEGMENTS_I_01}, which orders by {@code fareConditions}).
     * <p>
     * The exercise rounds the average to two decimals; it comes back here as the provider
     * computes it. The order is in the query.
     *
     * @param pageRequest the page to return.
     * @return a page of the fare conditions with their prices, by name.
     */
    @Query("""
            select s.fareConditions, min(s.price), avg(s.price), max(s.price)
            from Segment s
            group by s.fareConditions
            order by s.fareConditions""")
    Page<FareConditionsPrices> selectPricesByFareConditions(PageRequest pageRequest);
}
