package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Airplane;
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
 * The unit tests of {@link AirplaneRepository}.
 * <p>
 * The view is a table on the in-memory unit, and its columns are read-only in the mapping, so a test inserts the
 * airplanes it queries with SQL.
 */
@AddBeanClasses(AirplaneRepository_.class)
class AirplaneRepository_Test
        extends _Repository_Test<AirplaneRepository, Airplane, String> {

    AirplaneRepository_Test() {
        super(AirplaneRepository.class, Airplane.class, String.class);
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
     * Inserts an airplane of the specified model into the view's table, and returns the row read back.
     */
    private Airplane persistAirplane(final EntityManager em, final String model) {
        final var code = randomCode();
        em.createNativeQuery("insert into airplanes (airplane_code, model, range, speed) values (?1, ?2, ?3, ?4)")
                .setParameter(1, code)
                .setParameter(2, model)
                .setParameter(3, ThreadLocalRandom.current().nextInt(1000, 20000))
                .setParameter(4, ThreadLocalRandom.current().nextInt(500, 1000))
                .executeUpdate();
        return em.find(Airplane.class, code);
    }

    private Airplane persistAirplane(final EntityManager em) {
        return persistAirplane(em, "Boeing " + randomCode());
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_AIRPLANES_B_01: listAll()")
    @Nested
    class ListAllTest {

        @Test
        void __returnsEveryAirplane() {
            final var codes = entityManagerFactory.callInTransaction(em -> new String[] {
                    persistAirplane(em).getAirplaneCode(),
                    persistAirplane(em).getAirplaneCode()
            });
            assertThat(getRepositoryInstance().listAll()).extracting(Airplane::getAirplaneCode)
                    .containsExactlyInAnyOrder(codes);
        }

        @Test
        void __orderedByAirplaneCode() {
            entityManagerFactory.runInTransaction(em -> {
                for (int i = 0; i < 5; i++) {
                    persistAirplane(em);
                }
            });
            assertThat(getRepositoryInstance().listAll()).hasSize(5).extracting(Airplane::getAirplaneCode).isSorted();
        }

        @Test
        void __emptyWhenNone() {
            assertThat(getRepositoryInstance().listAll()).isEmpty();
        }
    }

    @DisplayName("DEMODB_AIRPLANES_I_01: selectAirplaneCountsByMaker(pageRequest)")
    @Nested
    class SelectAirplaneCountsByMakerTest {

        // three Boeings, two Airbuses, one Embraer
        private void persist() {
            entityManagerFactory.runInTransaction(em -> {
                persistAirplane(em, "Boeing 737 MAX 7");
                persistAirplane(em, "Boeing 767-300F");
                persistAirplane(em, "Boeing 777-300ER");
                persistAirplane(em, "Airbus A320neo");
                persistAirplane(em, "Airbus A350F");
                persistAirplane(em, "Embraer E170");
            });
        }

        @Test
        void __returnsMakersWithCounts() {
            persist();
            final var page = getRepositoryInstance().selectAirplaneCountsByMaker(PageRequest.ofSize(10));
            assertThat(page.content())
                    .extracting(AirplaneRepository.MakerAirplaneCount::maker,
                                AirplaneRepository.MakerAirplaneCount::airplanes)
                    .containsExactlyInAnyOrder(tuple("Boeing", 3L), tuple("Airbus", 2L), tuple("Embraer", 1L));
        }

        @Test
        void __orderedByAirplanesDescendingThenMaker() {
            persist();
            final var page = getRepositoryInstance().selectAirplaneCountsByMaker(PageRequest.ofSize(10));
            assertThat(page.content()).extracting(AirplaneRepository.MakerAirplaneCount::maker)
                    .containsExactly("Boeing", "Airbus", "Embraer");
        }

        @Test
        void __paged() {
            persist();
            final var first = getRepositoryInstance().selectAirplaneCountsByMaker(PageRequest.ofSize(2));
            assertThat(first.content()).extracting(AirplaneRepository.MakerAirplaneCount::maker)
                    .containsExactly("Boeing", "Airbus");
            assertThat(first.totalElements()).isEqualTo(3L);
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

    // -----------------------------------------------------------------------------------------------------------------

    @Inject
    private EntityManagerFactory entityManagerFactory;
}
