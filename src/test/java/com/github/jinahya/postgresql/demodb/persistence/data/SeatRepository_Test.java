package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.AirplaneData;
import com.github.jinahya.postgresql.demodb.persistence.Seat;
import com.github.jinahya.postgresql.demodb.persistence.SeatId;
import jakarta.data.page.PageRequest;
import com.github.jinahya.object.randomizer.ObjectRandomizerUtils;
import com.github.jinahya.persistence.test.util.EntityPersisterUtils;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The unit tests of {@link SeatRepository}.
 */
@AddBeanClasses(SeatRepository_.class)
class SeatRepository_Test
        extends _Repository_Test<SeatRepository, Seat, SeatId> {

    SeatRepository_Test() {
        super(SeatRepository.class, Seat.class, SeatId.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    private Seat persistSeat(final EntityManager em, final AirplaneData airplane, final String seatNo,
                             final String fareConditions) {
        final var seat = Seat.of(airplane, seatNo);
        seat.setFareConditions(fareConditions);
        em.persist(seat);
        return seat;
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_SEATS_B_01: listByAirplaneCodeAndFareConditions(airplaneCode, fareConditions)")
    @Nested
    class ListByAirplaneCodeAndFareConditionsTest {

        @Test
        void __returnsSeatsOfTheAirplaneAndTheFareConditions() {
            final var code = entityManagerFactory.callInTransaction(em -> {
                final var airplane = EntityPersisterUtils.newPersistedInstanceOf(em, AirplaneData.class);
                final var other = EntityPersisterUtils.newPersistedInstanceOf(em, AirplaneData.class);
                persistSeat(em, airplane, "1A", "Business");
                persistSeat(em, airplane, "1C", "Business");
                persistSeat(em, airplane, "10A", "Economy");
                persistSeat(em, other, "1A", "Business");
                return airplane.getAirplaneCode();
            });
            final var list = getRepositoryInstance().listByAirplaneCodeAndFareConditions(code, "Business");
            assertThat(list).extracting(Seat::getAirplaneCode, Seat::getSeatNo)
                    .containsExactlyInAnyOrder(tuple(code, "1A"), tuple(code, "1C"));
        }

        @Test
        void __orderedBySeatNo() {
            final var code = entityManagerFactory.callInTransaction(em -> {
                final var airplane = EntityPersisterUtils.newPersistedInstanceOf(em, AirplaneData.class);
                persistSeat(em, airplane, "2A", "Business");
                persistSeat(em, airplane, "1C", "Business");
                persistSeat(em, airplane, "1A", "Business");
                return airplane.getAirplaneCode();
            });
            final var list = getRepositoryInstance().listByAirplaneCodeAndFareConditions(code, "Business");
            assertThat(list).extracting(Seat::getSeatNo).containsExactly("1A", "1C", "2A");
        }

        @Test
        void __emptyWhenNoneMatches() {
            assertThat(getRepositoryInstance().listByAirplaneCodeAndFareConditions("32n", "Business")).isEmpty();
        }

        @Test
        void __nullArguments_npe() {
            final var repository = getRepositoryInstance();
            assertThatThrownBy(() -> repository.listByAirplaneCodeAndFareConditions(null, "Business"))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> repository.listByAirplaneCodeAndFareConditions("32n", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @DisplayName("DEMODB_SEATS_I_01: selectSeatCountsByFareConditions(pageRequest)")
    @Nested
    class SelectSeatCountsByFareConditionsTest {

        // one airplane with 2 Economy, 1 Comfort, 1 Business; one with no seat
        private String[] persist() {
            return entityManagerFactory.callInTransaction(em -> {
                final var seated = EntityPersisterUtils.newPersistedInstanceOf(em, AirplaneData.class);
                final var empty = EntityPersisterUtils.newPersistedInstanceOf(em, AirplaneData.class);
                persistSeat(em, seated, "10A", "Economy");
                persistSeat(em, seated, "10B", "Economy");
                persistSeat(em, seated, "5A", "Comfort");
                persistSeat(em, seated, "1A", "Business");
                return new String[] {seated.getAirplaneCode(), empty.getAirplaneCode()};
            });
        }

        @Test
        void __returnsAirplanesWithSeatCounts() {
            final var codes = persist();
            final var page = getRepositoryInstance().selectSeatCountsByFareConditions(PageRequest.ofSize(10));
            assertThat(page.content())
                    .extracting(c -> c.airplane().getAirplaneCode(), SeatRepository.AirplaneSeatCounts::economy,
                                SeatRepository.AirplaneSeatCounts::comfort, SeatRepository.AirplaneSeatCounts::business,
                                SeatRepository.AirplaneSeatCounts::seats)
                    .containsExactlyInAnyOrder(tuple(codes[0], 2L, 1L, 1L, 4L), tuple(codes[1], 0L, 0L, 0L, 0L));
        }

        @Test
        void __orderedByAirplaneCode() {
            persist();
            final var page = getRepositoryInstance().selectSeatCountsByFareConditions(PageRequest.ofSize(10));
            assertThat(page.content()).extracting(c -> c.airplane().getAirplaneCode()).isSorted();
        }

        @Test
        void __paged() {
            persist();
            final var first = getRepositoryInstance().selectSeatCountsByFareConditions(PageRequest.ofSize(1));
            assertThat(first.content()).hasSize(1);
            assertThat(first.totalElements()).isEqualTo(2L);
            final var second = getRepositoryInstance().selectSeatCountsByFareConditions(first.nextPageRequest());
            assertThat(second.content()).hasSize(1);
            assertThat(second.hasNext()).isFalse();
        }

        @Test
        void __nullPageRequest_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().selectSeatCountsByFareConditions(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    @Inject
    private EntityManagerFactory entityManagerFactory;
}
