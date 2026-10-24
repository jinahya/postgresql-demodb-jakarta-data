package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.AirportData;
import jakarta.data.page.PageRequest;
import com.github.jinahya.object.randomizer.ObjectRandomizerUtils;
import com.github.jinahya.persistence.test.util.EntityPersisterUtils;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The unit tests of {@link AirportDataRepository}.
 */
@AddBeanClasses(AirportDataRepository_.class)
class AirportDataRepository_Test
        extends _Repository_Test<AirportDataRepository, AirportData, String> {

    AirportDataRepository_Test() {
        super(AirportDataRepository.class, AirportData.class, String.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    private AirportData persistAirport(final jakarta.persistence.EntityManager em, final String timezone) {
        final var airport = ObjectRandomizerUtils.newRandomizedInstanceOf(AirportData.class).orElseThrow();
        airport.setTimezone(timezone);
        em.persist(airport);
        return airport;
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_AIRPORTS_DATA_B_01: listByTimezone(timezone)")
    @Nested
    class ListByTimezoneTest {

        @Test
        void __returnsAirportsOfTheTimezone() {
            final var codes = entityManagerFactory.callInTransaction(em -> new String[] {
                    persistAirport(em, "Europe/Moscow").getAirportCode(),
                    persistAirport(em, "Asia/Seoul").getAirportCode(),
                    persistAirport(em, "Europe/Moscow").getAirportCode()
            });
            final var list = getRepositoryInstance().listByTimezone("Europe/Moscow");
            assertThat(list).extracting(AirportData::getAirportCode).containsExactlyInAnyOrder(codes[0], codes[2]);
        }

        @Test
        void __orderedByAirportCode() {
            entityManagerFactory.runInTransaction(em -> {
                for (int i = 0; i < 5; i++) {
                    persistAirport(em, "Europe/Moscow");
                }
            });
            final var list = getRepositoryInstance().listByTimezone("Europe/Moscow");
            assertThat(list).hasSize(5).extracting(AirportData::getAirportCode).isSorted();
        }

        @Test
        void __emptyWhenNoneMatches() {
            assertThat(getRepositoryInstance().listByTimezone("Europe/Moscow")).isEmpty();
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

        // three in Moscow, two in Seoul, one in Tahiti
        private void persist() {
            entityManagerFactory.runInTransaction(em -> {
                persistAirport(em, "Europe/Moscow");
                persistAirport(em, "Europe/Moscow");
                persistAirport(em, "Europe/Moscow");
                persistAirport(em, "Asia/Seoul");
                persistAirport(em, "Asia/Seoul");
                persistAirport(em, "Pacific/Tahiti");
            });
        }

        @Test
        void __returnsTimezonesWithCounts() {
            persist();
            final var page = getRepositoryInstance().selectAirportCountsByTimezone(PageRequest.ofSize(10));
            assertThat(page.content())
                    .extracting(AirportDataRepository.TimezoneAirportCount::timezone,
                                AirportDataRepository.TimezoneAirportCount::airports)
                    .containsExactlyInAnyOrder(tuple("Europe/Moscow", 3L), tuple("Asia/Seoul", 2L),
                                               tuple("Pacific/Tahiti", 1L));
        }

        @Test
        void __orderedByAirportsDescendingThenTimezone() {
            persist();
            final var page = getRepositoryInstance().selectAirportCountsByTimezone(PageRequest.ofSize(10));
            assertThat(page.content()).extracting(AirportDataRepository.TimezoneAirportCount::timezone)
                    .containsExactly("Europe/Moscow", "Asia/Seoul", "Pacific/Tahiti");
        }

        @Test
        void __paged() {
            persist();
            final var first = getRepositoryInstance().selectAirportCountsByTimezone(PageRequest.ofSize(2));
            assertThat(first.content()).hasSize(2);
            assertThat(first.totalElements()).isEqualTo(3L);
            final var second = getRepositoryInstance().selectAirportCountsByTimezone(first.nextPageRequest());
            assertThat(second.content()).extracting(AirportDataRepository.TimezoneAirportCount::timezone)
                    .containsExactly("Pacific/Tahiti");
        }

        @Test
        void __nullPageRequest_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().selectAirportCountsByTimezone(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    @Inject
    private EntityManagerFactory entityManagerFactory;
}
