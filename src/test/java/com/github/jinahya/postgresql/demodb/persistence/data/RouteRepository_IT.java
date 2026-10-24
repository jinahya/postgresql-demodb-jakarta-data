package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Route;
import com.github.jinahya.postgresql.demodb.persistence.RouteId;
import jakarta.data.page.PageRequest;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The integration tests of {@link RouteRepository}.
 * <p>
 * The expected rows are the ones upstream's {@code routes_exercises_0N_*.sql} give.
 */
@AddBeanClasses(RouteRepository_.class)
class RouteRepository_IT
        extends _Repository_IT<RouteRepository, Route, RouteId> {

    RouteRepository_IT() {
        super(RouteRepository.class, Route.class, RouteId.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_ROUTES_B_01: listByDepartureAirport(departureAirport)")
    @Nested
    class ListByDepartureAirportTest {

        @Test
        void __returnsTheThirtyFourRoutesFromSheremetyevo() {
            final var list = getRepositoryInstance().listByDepartureAirport("SVO");
            assertThat(list).hasSize(34)
                    .allSatisfy(r -> assertThat(r.getDepartureAirportData().getAirportCode()).isEqualTo("SVO"));
            assertThat(list).extracting(Route::getRouteNo, r -> r.getArrivalAirportData().getAirportCode(),
                                        r -> r.getAirplaneData().getAirplaneCode())
                    .startsWith(tuple("PG0006", "KJA", "789"), tuple("PG0006", "KJA", "E70"),
                                tuple("PG0006", "KJA", "339"), tuple("PG0006", "KJA", "32N"),
                                tuple("PG0012", "UFA", "351"));
        }

        @Test
        void __orderedByRouteNoThenValidity() {
            final var list = getRepositoryInstance().listByDepartureAirport("SVO");
            assertThat(list).extracting(Route::getRouteNo).isSorted();
            assertThat(list).extracting(Route::getValidity)
                    .startsWith("[\"2025-10-01 00:00:00+00\",\"2025-11-01 00:00:00+00\")",
                                "[\"2025-11-01 00:00:00+00\",\"2025-12-01 00:00:00+00\")",
                                "[\"2025-12-01 00:00:00+00\",\"2026-01-01 00:00:00+00\")",
                                "[\"2026-01-01 00:00:00+00\",\"2026-02-01 00:00:00+00\")");
        }

        @Test
        void __nullDepartureAirport_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().listByDepartureAirport(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @DisplayName("DEMODB_ROUTES_I_01: listByRouteNo(routeNo, pageRequest)")
    @Nested
    class ListByRouteNoTest {

        @Test
        void __returnsTheFourPeriodsOfPg0001() {
            final var page = getRepositoryInstance().listByRouteNo("PG0001", PageRequest.ofSize(10));
            assertThat(page.content()).extracting(Route::getRouteNo, r -> r.getAirplaneData().getAirplaneCode())
                    .containsExactly(tuple("PG0001", "339"), tuple("PG0001", "789"), tuple("PG0001", "789"),
                                     tuple("PG0001", "77W"));
            assertThat(page.content()).extracting(Route::getValidity)
                    .containsExactly("[\"2025-10-01 00:00:00+00\",\"2025-11-01 00:00:00+00\")",
                                     "[\"2025-11-01 00:00:00+00\",\"2025-12-01 00:00:00+00\")",
                                     "[\"2025-12-01 00:00:00+00\",\"2026-01-01 00:00:00+00\")",
                                     "[\"2026-01-01 00:00:00+00\",\"2026-02-01 00:00:00+00\")");
        }

        @Test
        void __paged() {
            final var first = getRepositoryInstance().listByRouteNo("PG0001", PageRequest.ofSize(3));
            assertThat(first.content()).hasSize(3);
            assertThat(first.totalElements()).isEqualTo(4L);
            final var second = getRepositoryInstance().listByRouteNo("PG0001", first.nextPageRequest());
            assertThat(second.content()).extracting(r -> r.getAirplaneData().getAirplaneCode()).containsExactly("77W");
        }

        @Test
        void __nullRouteNo_matchesNothing() {
            assertThat(getRepositoryInstance().listByRouteNo(null, PageRequest.ofSize(10)).content()).isEmpty();
        }

        @Test
        void __nullPageRequest_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().listByRouteNo("PG0001", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
