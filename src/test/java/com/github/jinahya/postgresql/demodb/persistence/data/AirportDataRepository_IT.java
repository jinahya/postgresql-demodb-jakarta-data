package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.AirportData;
import jakarta.data.page.PageRequest;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The integration tests of {@link AirportDataRepository}.
 * <p>
 * The expected rows are the ones upstream's {@code airports_data_exercises_0N_*.sql} give.
 */
@AddBeanClasses(AirportDataRepository_.class)
class AirportDataRepository_IT
        extends _Repository_IT<AirportDataRepository, AirportData, String> {

    AirportDataRepository_IT() {
        super(AirportDataRepository.class, AirportData.class, String.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_AIRPORTS_DATA_B_01: listByTimezone(timezone)")
    @Nested
    class ListByTimezoneTest {

        @Test
        void __returnsTheSixtyFiveAirportsOfMoscowTime() {
            final var list = getRepositoryInstance().listByTimezone("Europe/Moscow");
            assertThat(list).hasSize(65)
                    .allSatisfy(a -> assertThat(a.getTimezone()).isEqualTo("Europe/Moscow"));
            assertThat(list).extracting(AirportData::getAirportCode)
                    .startsWith("AAQ", "AER", "AMV", "ARH", "BKA", "BWO", "BZK", "CEE", "CKL", "CSH");
        }

        @Test
        void __orderedByAirportCode() {
            assertThat(getRepositoryInstance().listByTimezone("Europe/Moscow"))
                    .extracting(AirportData::getAirportCode).isSorted();
        }

        @Test
        void __nullTimezone_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().listByTimezone(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @DisplayName("DEMODB_AIRPORTS_DATA_I_02: selectAirportCountsByTimezone(pageRequest)")
    @Nested
    class SelectAirportCountsByTimezoneTest {

        @Test
        void __returnsTheTenTimezonesWithTheMostAirports() {
            final var page = getRepositoryInstance().selectAirportCountsByTimezone(PageRequest.ofSize(10));
            assertThat(page.content())
                    .extracting(AirportDataRepository.TimezoneAirportCount::timezone,
                                AirportDataRepository.TimezoneAirportCount::airports)
                    .containsExactly(
                            tuple("America/New_York", 366L),
                            tuple("America/Chicago", 296L),
                            tuple("Asia/Shanghai", 188L),
                            tuple("America/Anchorage", 168L),
                            tuple("America/Los_Angeles", 143L),
                            tuple("Europe/Paris", 123L),
                            tuple("America/Toronto", 120L),
                            tuple("America/Denver", 110L),
                            tuple("Asia/Kolkata", 110L),
                            tuple("Europe/London", 106L));
        }

        @Test
        void __paged() {
            final var first = getRepositoryInstance().selectAirportCountsByTimezone(PageRequest.ofSize(5));
            assertThat(first.content()).hasSize(5);
            assertThat(first.totalElements()).isGreaterThan(10L);
            final var second = getRepositoryInstance().selectAirportCountsByTimezone(first.nextPageRequest());
            assertThat(second.content()).extracting(AirportDataRepository.TimezoneAirportCount::timezone)
                    .containsExactly("Europe/Paris", "America/Toronto", "America/Denver", "Asia/Kolkata",
                                     "Europe/London");
        }

        @Test
        void __nullPageRequest_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().selectAirportCountsByTimezone(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
