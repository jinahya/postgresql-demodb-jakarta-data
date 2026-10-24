package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Airport;
import jakarta.data.page.PageRequest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The unit tests of {@link AirportRepository}.
 * <p>
 * The view is a table on the in-memory unit, and its columns are read-only in the mapping, so a test inserts the
 * airports it queries with SQL.
 */
@AddBeanClasses(AirportRepository_.class)
class AirportRepository_Test
        extends _Repository_Test<AirportRepository, Airport, String> {

    AirportRepository_Test() {
        super(AirportRepository.class, Airport.class, String.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Returns a random code of three lower-case letters, which never collides with an installed, upper-case one.
     */
    private static String randomCode() {
        final var random = ThreadLocalRandom.current();
        final var builder = new StringBuilder(3);
        for (int i = 0; i < 3; i++) {
            builder.append((char) ('a' + random.nextInt(26)));
        }
        return builder.toString();
    }

    /**
     * Inserts an airport of the specified country into the view's table, and returns the row read back.
     */
    private Airport persistAirport(final EntityManager em, final String country) {
        final var code = randomCode();
        em.createNativeQuery("""
                                     insert into airports (airport_code, airport_name, city, country, coordinates, timezone)
                                     values (?1, ?2, ?3, ?4, ?5, ?6)""")
                .setParameter(1, code)
                .setParameter(2, "Airport " + code)
                .setParameter(3, "City " + code)
                .setParameter(4, country)
                .setParameter(5, "(0,0)")
                .setParameter(6, "Europe/Oslo")
                .executeUpdate();
        return em.find(Airport.class, code);
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_AIRPORTS_B_01: listByCountry(country)")
    @Nested
    class ListByCountryTest {

        @Test
        void __returnsAirportsOfTheCountry() {
            final var codes = entityManagerFactory.callInTransaction(em -> new String[] {
                    persistAirport(em, "Norway").getAirportCode(),
                    persistAirport(em, "Sweden").getAirportCode(),
                    persistAirport(em, "Norway").getAirportCode()
            });
            assertThat(getRepositoryInstance().listByCountry("Norway")).extracting(Airport::getAirportCode)
                    .containsExactlyInAnyOrder(codes[0], codes[2]);
        }

        @Test
        void __orderedByAirportCode() {
            entityManagerFactory.runInTransaction(em -> {
                for (int i = 0; i < 5; i++) {
                    persistAirport(em, "Norway");
                }
            });
            assertThat(getRepositoryInstance().listByCountry("Norway")).hasSize(5)
                    .extracting(Airport::getAirportCode).isSorted();
        }

        @Test
        void __emptyWhenNoneMatches() {
            assertThat(getRepositoryInstance().listByCountry("Norway")).isEmpty();
        }

        // the view's country is nullable in the mapping, so null is looked up, and no row has none
        @Test
        void __nullCountry_matchesNothing() {
            entityManagerFactory.runInTransaction(em -> persistAirport(em, "Norway"));
            assertThat(getRepositoryInstance().listByCountry(null)).isEmpty();
        }
    }

    @DisplayName("DEMODB_AIRPORTS_I_01: selectAirportCountsByCountry(pageRequest)")
    @Nested
    class SelectAirportCountsByCountryTest {

        private void persist() {
            entityManagerFactory.runInTransaction(em -> {
                persistAirport(em, "Norway");
                persistAirport(em, "Norway");
                persistAirport(em, "Norway");
                persistAirport(em, "Sweden");
                persistAirport(em, "Sweden");
                persistAirport(em, "Finland");
            });
        }

        @Test
        void __returnsCountriesWithCounts() {
            persist();
            final var page = getRepositoryInstance().selectAirportCountsByCountry(PageRequest.ofSize(10));
            assertThat(page.content())
                    .extracting(AirportRepository.CountryAirportCount::country,
                                AirportRepository.CountryAirportCount::airports)
                    .containsExactlyInAnyOrder(tuple("Norway", 3L), tuple("Sweden", 2L), tuple("Finland", 1L));
        }

        @Test
        void __orderedByAirportsDescendingThenCountry() {
            persist();
            final var page = getRepositoryInstance().selectAirportCountsByCountry(PageRequest.ofSize(10));
            assertThat(page.content()).extracting(AirportRepository.CountryAirportCount::country)
                    .containsExactly("Norway", "Sweden", "Finland");
        }

        @Test
        void __paged() {
            persist();
            final var first = getRepositoryInstance().selectAirportCountsByCountry(PageRequest.ofSize(2));
            assertThat(first.content()).hasSize(2);
            assertThat(first.totalElements()).isEqualTo(3L);
            final var second = getRepositoryInstance().selectAirportCountsByCountry(first.nextPageRequest());
            assertThat(second.content()).extracting(AirportRepository.CountryAirportCount::country)
                    .containsExactly("Finland");
        }

        @Test
        void __nullPageRequest_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().selectAirportCountsByCountry(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    @Inject
    private EntityManagerFactory entityManagerFactory;
}
