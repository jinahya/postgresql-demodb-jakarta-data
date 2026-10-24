package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Flight;
import com.github.jinahya.postgresql.demodb.persistence.Segment;
import com.github.jinahya.postgresql.demodb.persistence.SegmentId;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

/**
 * The unit tests of {@link SegmentRepository}.
 */
@AddBeanClasses(SegmentRepository_.class)
class SegmentRepository_Test
        extends _Repository_Test<SegmentRepository, Segment, SegmentId> {

    SegmentRepository_Test() {
        super(SegmentRepository.class, Segment.class, SegmentId.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    private Segment persistSegment(final EntityManager em, final Ticket ticket, final String fareConditions,
                                   final String price) {
        final var flight = EntityPersisterUtils.newPersistedInstanceOf(em, Flight.class);
        em.flush();
        final var segment = Segment.of(ticket, flight);
        segment.setFareConditions(fareConditions);
        segment.setPrice(new BigDecimal(price));
        em.persist(segment);
        return segment;
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_SEGMENTS_B_01: listByTicketNo(ticketNo)")
    @Nested
    class ListByTicketNoTest {

        @Test
        void __returnsSegmentsOfTheTicket() {
            final var result = entityManagerFactory.callInTransaction(em -> {
                final var ticket = EntityPersisterUtils.newPersistedInstanceOf(em, Ticket.class);
                final var other = EntityPersisterUtils.newPersistedInstanceOf(em, Ticket.class);
                final var s1 = persistSegment(em, ticket, "Economy", "3250.00");
                final var s2 = persistSegment(em, ticket, "Business", "9000.00");
                persistSegment(em, other, "Economy", "3250.00");
                return new Object[] {ticket.getTicketNo(), s1.getFlightId(), s2.getFlightId()};
            });
            final var list = getRepositoryInstance().listByTicketNo((String) result[0]);
            assertThat(list).extracting(Segment::getFlightId)
                    .containsExactlyInAnyOrder((Integer) result[1], (Integer) result[2]);
        }

        @Test
        void __orderedByFlightId() {
            final var ticketNo = entityManagerFactory.callInTransaction(em -> {
                final var ticket = EntityPersisterUtils.newPersistedInstanceOf(em, Ticket.class);
                for (int i = 0; i < 4; i++) {
                    persistSegment(em, ticket, "Economy", "3250.00");
                }
                return ticket.getTicketNo();
            });
            assertThat(getRepositoryInstance().listByTicketNo(ticketNo)).hasSize(4).extracting(Segment::getFlightId)
                    .isSorted();
        }

        @Test
        void __emptyWhenNoneMatches() {
            assertThat(getRepositoryInstance().listByTicketNo("0005432000000")).isEmpty();
        }

        @Test
        void __nullTicketNo_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().listByTicketNo(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @DisplayName("DEMODB_SEGMENTS_I_01: selectPricesByFareConditions(pageRequest)")
    @Nested
    class SelectPricesByFareConditionsTest {

        // Economy 1000, 2000, 6000; Business 9000
        private void persist() {
            entityManagerFactory.runInTransaction(em -> {
                final var ticket = EntityPersisterUtils.newPersistedInstanceOf(em, Ticket.class);
                persistSegment(em, ticket, "Economy", "2000.00");
                persistSegment(em, ticket, "Business", "9000.00");
                persistSegment(em, ticket, "Economy", "6000.00");
                persistSegment(em, ticket, "Economy", "1000.00");
            });
        }

        @Test
        void __returnsFareConditionsWithPrices() {
            persist();
            final var page = getRepositoryInstance().selectPricesByFareConditions(PageRequest.ofSize(10));
            assertThat(page.content())
                    .extracting(SegmentRepository.FareConditionsPrices::fareConditions,
                                p -> p.cheapest().stripTrailingZeros().toPlainString(),
                                p -> p.dearest().stripTrailingZeros().toPlainString())
                    .containsExactly(tuple("Business", "9000", "9000"), tuple("Economy", "1000", "6000"));
            assertThat(page.content().get(0).average()).isCloseTo(9000.0, within(0.01));
            assertThat(page.content().get(1).average()).isCloseTo(3000.0, within(0.01));
        }

        @Test
        void __paged() {
            persist();
            final var first = getRepositoryInstance().selectPricesByFareConditions(PageRequest.ofSize(1));
            assertThat(first.content()).extracting(SegmentRepository.FareConditionsPrices::fareConditions)
                    .containsExactly("Business");
            assertThat(first.totalElements()).isEqualTo(2L);
            final var second = getRepositoryInstance().selectPricesByFareConditions(first.nextPageRequest());
            assertThat(second.content()).extracting(SegmentRepository.FareConditionsPrices::fareConditions)
                    .containsExactly("Economy");
        }

        @Test
        void __nullPageRequest_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().selectPricesByFareConditions(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    @Inject
    private EntityManagerFactory entityManagerFactory;
}
