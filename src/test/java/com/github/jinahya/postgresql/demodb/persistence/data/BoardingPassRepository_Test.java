package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.BoardingPass;
import com.github.jinahya.postgresql.demodb.persistence.BoardingPassId;
import com.github.jinahya.postgresql.demodb.persistence.Flight;
import com.github.jinahya.postgresql.demodb.persistence.Segment;
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
 * The unit tests of {@link BoardingPassRepository}.
 */
@AddBeanClasses(BoardingPassRepository_.class)
class BoardingPassRepository_Test
        extends _Repository_Test<BoardingPassRepository, BoardingPass, BoardingPassId> {

    BoardingPassRepository_Test() {
        super(BoardingPassRepository.class, BoardingPass.class, BoardingPassId.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Persists a boarding pass on the specified flight, on a ticket of its own, with the specified seat and boarding
     * number.
     */
    private BoardingPass persistPass(final EntityManager em, final Flight flight, final String seatNo,
                                     final Integer boardingNo) {
        final var ticket = EntityPersisterUtils.newPersistedInstanceOf(em, Ticket.class);
        em.flush();
        final var segment = Segment.of(ticket, flight);
        segment.setFareConditions("Economy");
        segment.setPrice(new java.math.BigDecimal("3250.00"));
        em.persist(segment);
        em.flush();
        final var pass = BoardingPass.of(segment);
        pass.setSeatNo(seatNo);
        pass.setBoardingNo(boardingNo);
        pass.setBoardingTime(boardingNo == null ? null : flight.getScheduledDeparture().minusMinutes(30));
        em.persist(pass);
        return pass;
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_BOARDING_PASSES_B_01: listByFlightId(flightId)")
    @Nested
    class ListByFlightIdTest {

        @Test
        void __returnsPassesOfTheFlight() {
            final var result = entityManagerFactory.callInTransaction(em -> {
                final var flight = EntityPersisterUtils.newPersistedInstanceOf(em, Flight.class);
                final var other = EntityPersisterUtils.newPersistedInstanceOf(em, Flight.class);
                em.flush();
                final var p1 = persistPass(em, flight, "1A", 1);
                final var p2 = persistPass(em, flight, "2A", 2);
                persistPass(em, other, "1A", 1);
                return new Object[] {flight.getFlightId(), p1.getTicketNo(), p2.getTicketNo()};
            });
            final var list = getRepositoryInstance().listByFlightId((Integer) result[0]);
            assertThat(list).extracting(BoardingPass::getTicketNo)
                    .containsExactlyInAnyOrder((String) result[1], (String) result[2]);
        }

        @Test
        void __orderedByBoardingNoNullsLastThenTicketNo() {
            final var result = entityManagerFactory.callInTransaction(em -> {
                final var flight = EntityPersisterUtils.newPersistedInstanceOf(em, Flight.class);
                em.flush();
                final var third = persistPass(em, flight, "3A", 3);
                final var notBoarded = persistPass(em, flight, "9A", null);
                final var first = persistPass(em, flight, "1A", 1);
                return new Object[] {flight.getFlightId(), first.getTicketNo(), third.getTicketNo(),
                        notBoarded.getTicketNo()};
            });
            final var list = getRepositoryInstance().listByFlightId((Integer) result[0]);
            assertThat(list).extracting(BoardingPass::getTicketNo)
                    .containsExactly((String) result[1], (String) result[2], (String) result[3]);
        }

        @Test
        void __emptyWhenNoneMatches() {
            assertThat(getRepositoryInstance().listByFlightId(Integer.MAX_VALUE)).isEmpty();
        }

        @Test
        void __nullFlightId_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().listByFlightId(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @DisplayName("DEMODB_BOARDING_PASSES_I_01: selectBoardedCountsBySeatNo(pageRequest)")
    @Nested
    class SelectBoardedCountsBySeatNoTest {

        // seat 20D boarded three times, 22D twice, 1A once
        private void persist() {
            entityManagerFactory.runInTransaction(em -> {
                final var flight = EntityPersisterUtils.newPersistedInstanceOf(em, Flight.class);
                em.flush();
                int boardingNo = 1;
                for (final var seatNo : new String[] {"22D", "20D", "1A", "20D", "22D", "20D"}) {
                    persistPass(em, flight, seatNo, boardingNo++);
                }
            });
        }

        @Test
        void __returnsSeatNosWithCounts() {
            persist();
            final var page = getRepositoryInstance().selectBoardedCountsBySeatNo(PageRequest.ofSize(10));
            assertThat(page.content())
                    .extracting(BoardingPassRepository.SeatBoardedCount::seatNo,
                                BoardingPassRepository.SeatBoardedCount::boarded)
                    .containsExactlyInAnyOrder(tuple("20D", 3L), tuple("22D", 2L), tuple("1A", 1L));
        }

        @Test
        void __orderedByBoardedDescendingThenSeatNo() {
            persist();
            final var page = getRepositoryInstance().selectBoardedCountsBySeatNo(PageRequest.ofSize(10));
            assertThat(page.content()).extracting(BoardingPassRepository.SeatBoardedCount::seatNo)
                    .containsExactly("20D", "22D", "1A");
        }

        @Test
        void __paged() {
            persist();
            final var first = getRepositoryInstance().selectBoardedCountsBySeatNo(PageRequest.ofSize(2));
            assertThat(first.content()).hasSize(2);
            assertThat(first.totalElements()).isEqualTo(3L);
            final var second = getRepositoryInstance().selectBoardedCountsBySeatNo(first.nextPageRequest());
            assertThat(second.content()).extracting(BoardingPassRepository.SeatBoardedCount::seatNo)
                    .containsExactly("1A");
        }

        @Test
        void __nullPageRequest_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().selectBoardedCountsBySeatNo(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    @Inject
    private EntityManagerFactory entityManagerFactory;
}
