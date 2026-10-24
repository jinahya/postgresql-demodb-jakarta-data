package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Airport;
import jakarta.data.page.PageRequest;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The integration tests of {@link AirportRepository}.
 * <p>
 * The expected rows are the ones upstream's {@code airports_exercises_0N_*.sql} give.
 */
@AddBeanClasses(AirportRepository_.class)
class AirportRepository_IT
        extends _Repository_IT<AirportRepository, Airport, String> {

    AirportRepository_IT() {
        super(AirportRepository.class, Airport.class, String.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_AIRPORTS_B_01: listByCountry(country)")
    @Nested
    class ListByCountryTest {

        @Test
        void __returnsTheFiftyFiveAirportsOfNorway() {
            final var list = getRepositoryInstance().listByCountry("Norway");
            assertThat(list).hasSize(55).allSatisfy(a -> assertThat(a.getCountry()).isEqualTo("Norway"));
            assertThat(list).extracting(Airport::getAirportCode, Airport::getAirportName, Airport::getCity)
                    .startsWith(tuple("AES", "Vigra", "Ålesund"), tuple("ALF", "Alta", "Alta"),
                                tuple("ANX", "Andøya", "Andenes"), tuple("BDU", "Bardufoss", "Bardufoss"),
                                tuple("BGO", "Flesland", "Bergen"));
        }

        @Test
        void __orderedByAirportCode() {
            assertThat(getRepositoryInstance().listByCountry("Norway")).extracting(Airport::getAirportCode)
                    .isSorted();
        }

        @Test
        void __nullCountry_matchesNothing() {
            assertThat(getRepositoryInstance().listByCountry(null)).isEmpty();
        }
    }

    @DisplayName("DEMODB_AIRPORTS_I_01: selectAirportCountsByCountry(pageRequest)")
    @Nested
    class SelectAirportCountsByCountryTest {

        @Test
        void __returnsTheTenCountriesWithTheMostAirports() {
            final var page = getRepositoryInstance().selectAirportCountsByCountry(PageRequest.ofSize(10));
            assertThat(page.content())
                    .extracting(AirportRepository.CountryAirportCount::country,
                                AirportRepository.CountryAirportCount::airports)
                    .containsExactly(
                            tuple("United States", 1129L),
                            tuple("Canada", 335L),
                            tuple("Australia", 218L),
                            tuple("China", 187L),
                            tuple("Brazil", 180L),
                            tuple("Russia", 158L),
                            tuple("France", 122L),
                            tuple("India", 110L),
                            tuple("Indonesia", 105L),
                            tuple("United Kingdom", 105L));
        }

        @Test
        void __paged() {
            final var first = getRepositoryInstance().selectAirportCountsByCountry(PageRequest.ofSize(5));
            assertThat(first.content()).hasSize(5);
            assertThat(first.totalElements()).isGreaterThan(10L);
            final var second = getRepositoryInstance().selectAirportCountsByCountry(first.nextPageRequest());
            assertThat(second.content()).extracting(AirportRepository.CountryAirportCount::country)
                    .containsExactly("Russia", "France", "India", "Indonesia", "United Kingdom");
        }

        @Test
        void __nullPageRequest_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().selectAirportCountsByCountry(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
