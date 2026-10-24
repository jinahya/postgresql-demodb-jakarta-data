package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Ticket;
import com.github.jinahya.postgresql.demodb.persistence.mapped.MappedTicket_;
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
 * A repository for {@link Ticket}, the {@code tickets} table of the demonstration database.
 * <p>
 * The exercise methods answer the exercises of upstream's {@code EXERCISES.asciidoc} on the
 * {@code tickets} table, one method per exercise, each named in its javadoc by the exercise's id.
 */
@Repository
public interface TicketRepository
        extends _Repository<Ticket, String>,
                CrudRepository<Ticket, String> {

    // ----------------------------------------------------------------------------------- exercises

    /**
     * A passenger and how many tickets the passenger holds; a row of
     * {@link #selectTicketCountsByPassenger(PageRequest)}.
     *
     * @param passengerId   the id of the passenger.
     * @param passengerName the name of the passenger.
     * @param tickets       the number of tickets of the passenger.
     */
    record PassengerTicketCount(String passengerId, String passengerName, long tickets) {
    }

    /**
     * Lists the tickets of the specified booking ({@code DEMODB_TICKETS_B_01}, which asks for
     * {@code 0000EG} and orders by {@link MappedTicket_#TICKET_NO ticketNo}).
     * <p>
     * The booking is matched by its reference, the {@code book_ref} column, through the inherited
     * read-only attribute.
     *
     * @param bookRef the reference of the booking.
     * @return a list of the tickets of the booking, by ticket number.
     */
    @Find
    @OrderBy(MappedTicket_.TICKET_NO)
    List<Ticket> listByBookRef(@By(MappedTicket_.BOOK_REF) String bookRef);

    /**
     * Selects the passengers with how many tickets each holds ({@code DEMODB_TICKETS_I_01}, which
     * orders by {@code tickets} descending, then by {@code passengerId}; the exercise's
     * {@code LIMIT 5} is the first page of five).
     *
     * @param pageRequest the page to return.
     * @return a page of the passengers with their numbers of tickets, the most first.
     */
    @Query("""
            select t.passengerId, t.passengerName, count(t)
            from Ticket t
            group by t.passengerId, t.passengerName
            order by count(t) desc, t.passengerId""")
    Page<PassengerTicketCount> selectTicketCountsByPassenger(PageRequest pageRequest);
}
