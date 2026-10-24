package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Segment;
import com.github.jinahya.postgresql.demodb.persistence.SegmentId;
import jakarta.data.page.PageRequest;
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
 * The integration tests of {@link SegmentRepository}.
 * <p>
 * The expected rows are the ones upstream's {@code segments_exercises_0N_*.sql} give.
 */
@AddBeanClasses(SegmentRepository_.class)
class SegmentRepository_IT
        extends _Repository_IT<SegmentRepository, Segment, SegmentId> {

    SegmentRepository_IT() {
        super(SegmentRepository.class, Segment.class, SegmentId.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_SEGMENTS_B_01: listByTicketNo(ticketNo)")
    @Nested
    class ListByTicketNoTest {

        @Test
        void __returnsTheOneSegmentOfTheTicket() {
            final var list = getRepositoryInstance().listByTicketNo("0005432000000");
            assertThat(list).extracting(Segment::getTicketNo, Segment::getFlightId, Segment::getFareConditions)
                    .containsExactly(tuple("0005432000000", 108, "Economy"));
            assertThat(list.getFirst().getPrice()).isEqualByComparingTo(new BigDecimal("3250.00"));
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

        @Test
        void __returnsTheThreeFareConditionsWithTheirPrices() {
            final var page = getRepositoryInstance().selectPricesByFareConditions(PageRequest.ofSize(10));
            assertThat(page.content())
                    .extracting(SegmentRepository.FareConditionsPrices::fareConditions,
                                p -> p.cheapest().stripTrailingZeros().toPlainString(),
                                p -> p.dearest().stripTrailingZeros().toPlainString())
                    .containsExactly(tuple("Business", "3500", "119500"), tuple("Comfort", "2275", "65325"),
                                     tuple("Economy", "1750", "59750"));
            assertThat(page.content().get(0).average()).isCloseTo(11826.86, within(0.01));
            assertThat(page.content().get(1).average()).isCloseTo(6422.43, within(0.01));
            assertThat(page.content().get(2).average()).isCloseTo(5492.63, within(0.01));
        }

        @Test
        void __paged() {
            final var first = getRepositoryInstance().selectPricesByFareConditions(PageRequest.ofSize(2));
            assertThat(first.content()).hasSize(2);
            assertThat(first.totalElements()).isEqualTo(3L);
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
}
