package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Ticket;
import jakarta.data.page.PageRequest;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The integration tests of {@link TicketRepository}.
 * <p>
 * The expected rows are the ones upstream's {@code tickets_exercises_0N_*.sql} give.
 */
@AddBeanClasses(TicketRepository_.class)
class TicketRepository_IT
        extends _Repository_IT<TicketRepository, Ticket, String> {

    TicketRepository_IT() {
        super(TicketRepository.class, Ticket.class, String.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_TICKETS_B_01: listByBookRef(bookRef)")
    @Nested
    class ListByBookRefTest {

        @Test
        void __returnsTheTwoTicketsOfTheBooking0000EG() {
            final var list = getRepositoryInstance().listByBookRef("0000EG");
            assertThat(list)
                    .extracting(Ticket::getTicketNo, t -> t.getBooking().getBookRef(), Ticket::getPassengerId,
                                Ticket::getPassengerName, Ticket::getOutbound)
                    .containsExactly(
                            tuple("0005434564147", "0000EG", "CA 1985281597520", "John Beland", true),
                            tuple("0005434564152", "0000EG", "CA 1985281597520", "John Beland", false));
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

        @Test
        void __returnsTheFivePassengersWithTheMostTickets() {
            final var page = getRepositoryInstance().selectTicketCountsByPassenger(PageRequest.ofSize(5));
            assertThat(page.content())
                    .extracting(TicketRepository.PassengerTicketCount::passengerId,
                                TicketRepository.PassengerTicketCount::passengerName,
                                TicketRepository.PassengerTicketCount::tickets)
                    .containsExactly(
                            tuple("CN 1303160118058", "Haiyun Yang", 5L),
                            tuple("CN 8458154433091", "Haiyun Zhao", 5L),
                            tuple("US 2995168528581", "William Thompson", 5L),
                            tuple("CA 0307652121299", "Pascal Lang", 4L),
                            tuple("CA 0314814108732", "Dawn Dupuis", 4L));
        }

        @Test
        void __paged() {
            final var first = getRepositoryInstance().selectTicketCountsByPassenger(PageRequest.ofSize(3));
            assertThat(first.content()).hasSize(3);
            assertThat(first.totalElements()).isGreaterThan(5L);
            final var second = getRepositoryInstance().selectTicketCountsByPassenger(first.nextPageRequest());
            assertThat(second.content()).extracting(TicketRepository.PassengerTicketCount::passengerId)
                    .startsWith("CA 0307652121299", "CA 0314814108732");
        }

        @Test
        void __nullPageRequest_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().selectTicketCountsByPassenger(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
