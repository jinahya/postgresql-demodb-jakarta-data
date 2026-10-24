package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Airplane;
import jakarta.data.page.PageRequest;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The integration tests of {@link AirplaneRepository}.
 * <p>
 * The expected rows are the ones upstream's {@code airplanes_exercises_0N_*.sql} give.
 */
@AddBeanClasses(AirplaneRepository_.class)
class AirplaneRepository_IT
        extends _Repository_IT<AirplaneRepository, Airplane, String> {

    AirplaneRepository_IT() {
        super(AirplaneRepository.class, Airplane.class, String.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_AIRPLANES_B_01: listAll()")
    @Nested
    class ListAllTest {

        @Test
        void __returnsTheTenAirplanes() {
            final var list = getRepositoryInstance().listAll();
            assertThat(list).extracting(Airplane::getAirplaneCode, Airplane::getModel, Airplane::getRange)
                    .containsExactly(
                            tuple("32N", "Airbus A320neo", 6500),
                            tuple("339", "Airbus A330-900neo", 13300),
                            tuple("351", "Airbus A350-1000", 16700),
                            tuple("35X", "Airbus A350F", 8700),
                            tuple("76F", "Boeing 767-300F", 6000),
                            tuple("77W", "Boeing 777-300ER", 14600),
                            tuple("789", "Boeing 787-9 Dreamliner", 14000),
                            tuple("7M7", "Boeing 737 MAX 7", 7000),
                            tuple("CR7", "Bombardier CRJ700", 3100),
                            tuple("E70", "Embraer E170", 4000));
        }
    }

    @DisplayName("DEMODB_AIRPLANES_I_01: selectAirplaneCountsByMaker(pageRequest)")
    @Nested
    class SelectAirplaneCountsByMakerTest {

        @Test
        void __returnsTheFourMakers() {
            final var page = getRepositoryInstance().selectAirplaneCountsByMaker(PageRequest.ofSize(10));
            assertThat(page.content())
                    .extracting(AirplaneRepository.MakerAirplaneCount::maker,
                                AirplaneRepository.MakerAirplaneCount::airplanes)
                    .containsExactly(tuple("Airbus", 4L), tuple("Boeing", 4L), tuple("Bombardier", 1L),
                                     tuple("Embraer", 1L));
        }

        @Test
        void __paged() {
            final var first = getRepositoryInstance().selectAirplaneCountsByMaker(PageRequest.ofSize(3));
            assertThat(first.content()).hasSize(3);
            assertThat(first.totalElements()).isEqualTo(4L);
            final var second = getRepositoryInstance().selectAirplaneCountsByMaker(first.nextPageRequest());
            assertThat(second.content()).extracting(AirplaneRepository.MakerAirplaneCount::maker)
                    .containsExactly("Embraer");
        }

        @Test
        void __nullPageRequest_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().selectAirplaneCountsByMaker(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
