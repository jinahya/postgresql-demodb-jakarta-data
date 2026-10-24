package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Booking;
import com.github.jinahya.postgresql.demodb.persistence.Ticket;
import jakarta.data.page.PageRequest;
import com.github.jinahya.object.randomizer.ObjectRandomizerUtils;
import com.github.jinahya.persistence.test.util.EntityPersisterUtils;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The unit tests of {@link TicketRepository}.
 */
@AddBeanClasses(TicketRepository_.class)
class TicketRepository_Test
        extends _Repository_Test<TicketRepository, Ticket, String> {

    TicketRepository_Test() {
        super(TicketRepository.class, Ticket.class, String.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    private Ticket persistTicket(final EntityManager em, final Booking booking, final String passengerId,
                                 final String passengerName) {
        final var ticket = ObjectRandomizerUtils.newRandomizedInstanceOf(Ticket.class).orElseThrow();
        ticket.setBooking(booking);
        ticket.setPassengerId(passengerId);
        ticket.setPassengerName(passengerName);
        em.persist(ticket);
        return ticket;
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_TICKETS_B_01: listByBookRef(bookRef)")
    @Nested
    class ListByBookRefTest {

        @Test
        void __returnsTicketsOfTheBooking() {
            final var result = entityManagerFactory.callInTransaction(em -> {
                final var booking = EntityPersisterUtils.newPersistedInstanceOf(em, Booking.class);
                final var other = EntityPersisterUtils.newPersistedInstanceOf(em, Booking.class);
                final var t1 = persistTicket(em, booking, "KR 1", "A");
                final var t2 = persistTicket(em, booking, "KR 2", "B");
                persistTicket(em, other, "KR 3", "C");
                return new String[] {booking.getBookRef(), t1.getTicketNo(), t2.getTicketNo()};
            });
            final var list = getRepositoryInstance().listByBookRef(result[0]);
            assertThat(list).extracting(Ticket::getTicketNo).containsExactlyInAnyOrder(result[1], result[2]);
            assertThat(list).allSatisfy(t -> assertThat(t.getBooking().getBookRef()).isEqualTo(result[0]));
        }

        @Test
        void __orderedByTicketNo() {
            final var bookRef = entityManagerFactory.callInTransaction(em -> {
                final var booking = EntityPersisterUtils.newPersistedInstanceOf(em, Booking.class);
                for (int i = 0; i < 5; i++) {
                    persistTicket(em, booking, "KR " + i, "P" + i);
                }
                return booking.getBookRef();
            });
            assertThat(getRepositoryInstance().listByBookRef(bookRef)).hasSize(5).extracting(Ticket::getTicketNo)
                    .isSorted();
        }

        @Test
        void __emptyWhenNoneMatches() {
            assertThat(getRepositoryInstance().listByBookRef("0000eg")).isEmpty();
        }

        @Test
        void __nullBookRef_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().listByBookRef(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @DisplayName("DEMODB_TICKETS_I_01: selectTicketCountsByPassenger(pageRequest)")
    @Nested
    class SelectTicketCountsByPassengerTest {

        // three tickets of one passenger, two of another, one of a third
        private void persist() {
            entityManagerFactory.runInTransaction(em -> {
                final var booking = EntityPersisterUtils.newPersistedInstanceOf(em, Booking.class);
                persistTicket(em, booking, "KR 2", "Two");
                persistTicket(em, booking, "KR 1", "One");
                persistTicket(em, booking, "KR 3", "Three");
                persistTicket(em, booking, "KR 3", "Three");
                persistTicket(em, booking, "KR 2", "Two");
                persistTicket(em, booking, "KR 3", "Three");
            });
        }

        @Test
        void __returnsPassengersWithCounts() {
            persist();
            final var page = getRepositoryInstance().selectTicketCountsByPassenger(PageRequest.ofSize(10));
            assertThat(page.content())
                    .extracting(TicketRepository.PassengerTicketCount::passengerId,
                                TicketRepository.PassengerTicketCount::passengerName,
                                TicketRepository.PassengerTicketCount::tickets)
                    .containsExactlyInAnyOrder(tuple("KR 3", "Three", 3L), tuple("KR 2", "Two", 2L),
                                               tuple("KR 1", "One", 1L));
        }

        @Test
        void __orderedByTicketsDescendingThenPassengerId() {
            persist();
            final var page = getRepositoryInstance().selectTicketCountsByPassenger(PageRequest.ofSize(10));
            assertThat(page.content()).extracting(TicketRepository.PassengerTicketCount::passengerId)
                    .containsExactly("KR 3", "KR 2", "KR 1");
        }

        @Test
        void __paged() {
            persist();
            final var first = getRepositoryInstance().selectTicketCountsByPassenger(PageRequest.ofSize(2));
            assertThat(first.content()).hasSize(2);
            assertThat(first.totalElements()).isEqualTo(3L);
            final var second = getRepositoryInstance().selectTicketCountsByPassenger(first.nextPageRequest());
            assertThat(second.content()).extracting(TicketRepository.PassengerTicketCount::passengerId)
                    .containsExactly("KR 1");
        }

        @Test
        void __nullPageRequest_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().selectTicketCountsByPassenger(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    @Inject
    private EntityManagerFactory entityManagerFactory;
}
