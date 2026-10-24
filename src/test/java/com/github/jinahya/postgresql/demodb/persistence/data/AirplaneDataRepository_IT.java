package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.AirplaneData;
import jakarta.data.page.PageRequest;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

/**
 * The integration tests of {@link AirplaneDataRepository}.
 * <p>
 * The expected rows are the ones upstream's {@code airplanes_data_exercises_0N_*.sql} give.
 */
@AddBeanClasses(AirplaneDataRepository_.class)
class AirplaneDataRepository_IT
        extends _Repository_IT<AirplaneDataRepository, AirplaneData, String> {

    AirplaneDataRepository_IT() {
        super(AirplaneDataRepository.class, AirplaneData.class, String.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_AIRPLANES_DATA_B_01: listAll()")
    @Nested
    class ListAllTest {

        @Test
        void __returnsTheTenAirplanes() {
            final var list = getRepositoryInstance().listAll();
            assertThat(list).extracting(AirplaneData::getAirplaneCode, AirplaneData::getRange, AirplaneData::getSpeed)
                    .containsExactly(
                            tuple("32N", 6500, 830),
                            tuple("339", 13300, 870),
                            tuple("351", 16700, 913),
                            tuple("35X", 8700, 903),
                            tuple("76F", 6000, 850),
                            tuple("77W", 14600, 905),
                            tuple("789", 14000, 913),
                            tuple("7M7", 7000, 840),
                            tuple("CR7", 3100, 829),
                            tuple("E70", 4000, 800));
        }

        @Test
        void __orderedByAirplaneCode() {
            assertThat(getRepositoryInstance().listAll()).extracting(AirplaneData::getAirplaneCode).isSorted();
        }
    }

    @DisplayName("DEMODB_AIRPLANES_DATA_I_02: selectHoursAtFullRange(pageRequest)")
    @Nested
    class SelectHoursAtFullRangeTest {

        @Test
        void __returnsTheAirplanesWithTheirHours() {
            final var page = getRepositoryInstance().selectHoursAtFullRange(PageRequest.ofSize(10));
            assertThat(page.content()).extracting(h -> h.airplane().getAirplaneCode())
                    .containsExactly("351", "77W", "789", "339", "35X", "7M7", "32N", "76F", "E70", "CR7");
            final double[] hours = {18.29, 16.13, 15.33, 15.29, 9.63, 8.33, 7.83, 7.06, 5.00, 3.74};
            for (int i = 0; i < hours.length; i++) {
                assertThat(page.content().get(i).hours()).isCloseTo(hours[i], within(0.01));
            }
        }

        @Test
        void __paged() {
            final var first = getRepositoryInstance().selectHoursAtFullRange(PageRequest.ofSize(4));
            assertThat(first.content()).hasSize(4);
            assertThat(first.totalElements()).isEqualTo(10L);
            final var second = getRepositoryInstance().selectHoursAtFullRange(first.nextPageRequest());
            assertThat(second.content()).extracting(h -> h.airplane().getAirplaneCode())
                    .containsExactly("35X", "7M7", "32N", "76F");
        }

        @Test
        void __nullPageRequest_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().selectHoursAtFullRange(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
