package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.AirplaneData;
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
import static org.assertj.core.api.Assertions.within;

/**
 * The unit tests of {@link AirplaneDataRepository}.
 */
@AddBeanClasses(AirplaneDataRepository_.class)
class AirplaneDataRepository_Test
        extends _Repository_Test<AirplaneDataRepository, AirplaneData, String> {

    AirplaneDataRepository_Test() {
        super(AirplaneDataRepository.class, AirplaneData.class, String.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Persists a randomized airplane with the specified range and speed.
     */
    private AirplaneData persistAirplane(final jakarta.persistence.EntityManager em, final int range,
                                         final int speed) {
        final var airplane = ObjectRandomizerUtils.newRandomizedInstanceOf(AirplaneData.class).orElseThrow();
        airplane.setRange(range);
        airplane.setSpeed(speed);
        em.persist(airplane);
        return airplane;
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_AIRPLANES_DATA_B_01: listAll()")
    @Nested
    class ListAllTest {

        @Test
        void __returnsEveryAirplane() {
            final var codes = entityManagerFactory.callInTransaction(em -> new String[] {
                    EntityPersisterUtils.newPersistedInstanceOf(em, AirplaneData.class).getAirplaneCode(),
                    EntityPersisterUtils.newPersistedInstanceOf(em, AirplaneData.class).getAirplaneCode(),
                    EntityPersisterUtils.newPersistedInstanceOf(em, AirplaneData.class).getAirplaneCode()
            });
            final var list = getRepositoryInstance().listAll();
            assertThat(list).extracting(AirplaneData::getAirplaneCode).containsExactlyInAnyOrder(codes);
        }

        @Test
        void __orderedByAirplaneCode() {
            entityManagerFactory.runInTransaction(em -> {
                for (int i = 0; i < 5; i++) {
                    EntityPersisterUtils.newPersistedInstanceOf(em, AirplaneData.class);
                }
            });
            final var list = getRepositoryInstance().listAll();
            assertThat(list).hasSize(5).extracting(AirplaneData::getAirplaneCode).isSorted();
        }

        @Test
        void __emptyWhenNone() {
            assertThat(getRepositoryInstance().listAll()).isEmpty();
        }
    }

    @DisplayName("DEMODB_AIRPLANES_DATA_I_02: selectHoursAtFullRange(pageRequest)")
    @Nested
    class SelectHoursAtFullRangeTest {

        // 10 hours, 5 hours, 2 hours
        private String[] persist() {
            return entityManagerFactory.callInTransaction(em -> new String[] {
                    persistAirplane(em, 8000, 800).getAirplaneCode(),
                    persistAirplane(em, 4000, 800).getAirplaneCode(),
                    persistAirplane(em, 1600, 800).getAirplaneCode()
            });
        }

        @Test
        void __returnsAirplanesWithHours() {
            final var codes = persist();
            final var page = getRepositoryInstance().selectHoursAtFullRange(PageRequest.ofSize(10));
            assertThat(page.content()).hasSize(3).allSatisfy(h -> {
                assertThat(h.airplane()).isNotNull();
                assertThat(h.hours()).isCloseTo(
                        h.airplane().getRange() * 1.0 / h.airplane().getSpeed(), within(0.001));
            });
            assertThat(page.content()).extracting(h -> h.airplane().getAirplaneCode())
                    .containsExactlyInAnyOrder(codes);
        }

        @Test
        void __orderedByHoursDescending() {
            final var codes = persist();
            final var page = getRepositoryInstance().selectHoursAtFullRange(PageRequest.ofSize(10));
            assertThat(page.content()).extracting(h -> h.airplane().getAirplaneCode())
                    .containsExactly(codes[0], codes[1], codes[2]);
        }

        @Test
        void __paged() {
            final var codes = persist();
            final var first = getRepositoryInstance().selectHoursAtFullRange(PageRequest.ofSize(2));
            assertThat(first.content()).extracting(h -> h.airplane().getAirplaneCode())
                    .containsExactly(codes[0], codes[1]);
            assertThat(first.totalElements()).isEqualTo(3L);
            final var second = getRepositoryInstance().selectHoursAtFullRange(first.nextPageRequest());
            assertThat(second.content()).extracting(h -> h.airplane().getAirplaneCode())
                    .containsExactly(codes[2]);
            assertThat(second.hasNext()).isFalse();
        }

        @Test
        void __nullPageRequest_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().selectHoursAtFullRange(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    @Inject
    private EntityManagerFactory entityManagerFactory;
}
