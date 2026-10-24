package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.BoardingPass;
import com.github.jinahya.postgresql.demodb.persistence.BoardingPassId;
import jakarta.data.page.PageRequest;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The integration tests of {@link BoardingPassRepository}.
 * <p>
 * The expected rows are the ones upstream's {@code boarding_passes_exercises_0N_*.sql} give.
 */
@AddBeanClasses(BoardingPassRepository_.class)
class BoardingPassRepository_IT
        extends _Repository_IT<BoardingPassRepository, BoardingPass, BoardingPassId> {

    BoardingPassRepository_IT() {
        super(BoardingPassRepository.class, BoardingPass.class, BoardingPassId.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_BOARDING_PASSES_B_01: listByFlightId(flightId)")
    @Nested
    class ListByFlightIdTest {

        @Test
        void __returnsTheFortyEightPassesOfTheFlight1() {
            final var list = getRepositoryInstance().listByFlightId(1);
            assertThat(list).hasSize(48).allSatisfy(p -> assertThat(p.getFlightId()).isEqualTo(1));
            assertThat(list).extracting(BoardingPass::getTicketNo, BoardingPass::getSeatNo, BoardingPass::getBoardingNo)
                    .startsWith(tuple("0005432486526", "24A", 1), tuple("0005432803911", "13C", 2),
                                tuple("0005432498784", "14B", 3), tuple("0005432803924", "20A", 4),
                                tuple("0005432533210", "29D", 5));
        }

        @Test
        void __orderedByBoardingNo() {
            final var list = getRepositoryInstance().listByFlightId(1);
            assertThat(list).extracting(BoardingPass::getBoardingNo).doesNotContainNull().isSorted();
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

        @Test
        void __returnsTheFiveSeatsBoardedTheMost() {
            final var page = getRepositoryInstance().selectBoardedCountsBySeatNo(PageRequest.ofSize(5));
            assertThat(page.content())
                    .extracting(BoardingPassRepository.SeatBoardedCount::seatNo,
                                BoardingPassRepository.SeatBoardedCount::boarded)
                    .containsExactly(tuple("20D", 9773L), tuple("22D", 9752L), tuple("20A", 9742L),
                                     tuple("20C", 9732L), tuple("22A", 9730L));
        }

        @Test
        void __paged() {
            final var first = getRepositoryInstance().selectBoardedCountsBySeatNo(PageRequest.ofSize(3));
            assertThat(first.content()).hasSize(3);
            assertThat(first.totalElements()).isGreaterThan(5L);
            final var second = getRepositoryInstance().selectBoardedCountsBySeatNo(first.nextPageRequest());
            assertThat(second.content()).extracting(BoardingPassRepository.SeatBoardedCount::seatNo)
                    .startsWith("20C", "22A");
        }

        @Test
        void __nullPageRequest_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().selectBoardedCountsBySeatNo(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
