package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Seat;
import com.github.jinahya.postgresql.demodb.persistence.SeatId;
import jakarta.data.page.PageRequest;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The integration tests of {@link SeatRepository}.
 * <p>
 * The expected rows are the ones upstream's {@code seats_exercises_0N_*.sql} give.
 */
@AddBeanClasses(SeatRepository_.class)
class SeatRepository_IT
        extends _Repository_IT<SeatRepository, Seat, SeatId> {

    SeatRepository_IT() {
        super(SeatRepository.class, Seat.class, SeatId.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_SEATS_B_01: listByAirplaneCodeAndFareConditions(airplaneCode, fareConditions)")
    @Nested
    class ListByAirplaneCodeAndFareConditionsTest {

        @Test
        void __returnsTheTwentyEightBusinessSeatsOfTheA320neo() {
            final var list = getRepositoryInstance().listByAirplaneCodeAndFareConditions("32N", "Business");
            assertThat(list).hasSize(28).allSatisfy(s -> {
                assertThat(s.getAirplaneCode()).isEqualTo("32N");
                assertThat(s.getFareConditions()).isEqualTo("Business");
            });
            assertThat(list).extracting(Seat::getSeatNo)
                    .startsWith("1A", "1C", "1D", "1F", "2A", "2C", "2D", "2F", "3A", "3C");
        }

        @Test
        void __orderedBySeatNo() {
            assertThat(getRepositoryInstance().listByAirplaneCodeAndFareConditions("32N", "Business"))
                    .extracting(Seat::getSeatNo).isSorted();
        }

        @Test
        void __nullArguments_npe() {
            final var repository = getRepositoryInstance();
            assertThatThrownBy(() -> repository.listByAirplaneCodeAndFareConditions(null, "Business"))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> repository.listByAirplaneCodeAndFareConditions("32N", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @DisplayName("DEMODB_SEATS_I_01: selectSeatCountsByFareConditions(pageRequest)")
    @Nested
    class SelectSeatCountsByFareConditionsTest {

        @Test
        void __returnsTheTenAirplanesWithTheirSeatCounts() {
            final var page = getRepositoryInstance().selectSeatCountsByFareConditions(PageRequest.ofSize(10));
            assertThat(page.content())
                    .extracting(c -> c.airplane().getAirplaneCode(), SeatRepository.AirplaneSeatCounts::economy,
                                SeatRepository.AirplaneSeatCounts::comfort, SeatRepository.AirplaneSeatCounts::business,
                                SeatRepository.AirplaneSeatCounts::seats)
                    .containsExactly(
                            tuple("32N", 138L, 0L, 28L, 166L),
                            tuple("339", 224L, 28L, 29L, 281L),
                            tuple("351", 281L, 0L, 44L, 325L),
                            tuple("35X", 0L, 0L, 0L, 0L),
                            tuple("76F", 0L, 0L, 0L, 0L),
                            tuple("77W", 326L, 48L, 30L, 404L),
                            tuple("789", 188L, 21L, 48L, 257L),
                            tuple("7M7", 144L, 0L, 16L, 160L),
                            tuple("CR7", 52L, 12L, 6L, 70L),
                            tuple("E70", 72L, 0L, 6L, 78L));
        }

        @Test
        void __paged() {
            final var first = getRepositoryInstance().selectSeatCountsByFareConditions(PageRequest.ofSize(4));
            assertThat(first.content()).hasSize(4);
            assertThat(first.totalElements()).isEqualTo(10L);
            final var second = getRepositoryInstance().selectSeatCountsByFareConditions(first.nextPageRequest());
            assertThat(second.content()).extracting(c -> c.airplane().getAirplaneCode())
                    .containsExactly("76F", "77W", "789", "7M7");
        }

        @Test
        void __nullPageRequest_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().selectSeatCountsByFareConditions(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
