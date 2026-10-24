package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Booking;
import jakarta.data.page.PageRequest;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The integration tests of {@link BookingRepository}.
 * <p>
 * The expected rows are the ones upstream's {@code bookings_exercises_0N_*.sql} give.
 */
@AddBeanClasses(BookingRepository_.class)
class BookingRepository_IT
        extends _Repository_IT<BookingRepository, Booking, String> {

    private static final OffsetDateTime FROM = OffsetDateTime.of(2025, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private static final OffsetDateTime TO = FROM.plusMinutes(1);

    BookingRepository_IT() {
        super(BookingRepository.class, Booking.class, String.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_BOOKINGS_B_01: listByBookDateBetween(from, to)")
    @Nested
    class ListByBookDateBetweenTest {

        @Test
        void __returnsTheSeventeenBookingsOfTheFirstMinute() {
            final var list = getRepositoryInstance().listByBookDateBetween(FROM, TO);
            assertThat(list).hasSize(17)
                    .allSatisfy(b -> assertThat(b.getBookDate()).isAfterOrEqualTo(FROM).isBefore(TO));
            assertThat(list).extracting(Booking::getBookRef)
                    .startsWith("6TAS39", "GGY5EU", "Y4KNLB", "MJUZ2D", "4O2NMP", "2EW1SQ", "KP3OIQ", "H40481",
                                "KOS1KJ", "NWE9DB");
            assertThat(list.getFirst().getTotalAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        }

        @Test
        void __orderedByBookDate() {
            assertThat(getRepositoryInstance().listByBookDateBetween(FROM, TO)).extracting(Booking::getBookDate)
                    .isSorted();
        }

        @Test
        void __nullArguments_matchNothing() {
            final var repository = getRepositoryInstance();
            assertThat(repository.listByBookDateBetween(null, TO)).isEmpty();
            assertThat(repository.listByBookDateBetween(FROM, null)).isEmpty();
        }
    }

    @DisplayName("DEMODB_BOOKINGS_I_01: selectBookingCountsAndTotalsByMonth(pageRequest)")
    @Nested
    class SelectBookingCountsAndTotalsByMonthTest {

        @Test
        void __returnsTheThreeMonths() {
            final var page = getRepositoryInstance().selectBookingCountsAndTotalsByMonth(PageRequest.ofSize(10));
            assertThat(page.content())
                    .extracting(BookingRepository.MonthBookingTotal::year, BookingRepository.MonthBookingTotal::month,
                                BookingRepository.MonthBookingTotal::bookings)
                    .containsExactly(tuple(2025, 9, 448064L), tuple(2025, 10, 434159L), tuple(2025, 11, 410670L));
            assertThat(page.content().get(0).total()).isEqualByComparingTo(new BigDecimal("8749891200.00"));
            assertThat(page.content().get(1).total()).isEqualByComparingTo(new BigDecimal("8223964900.00"));
            assertThat(page.content().get(2).total()).isEqualByComparingTo(new BigDecimal("7600075475.00"));
        }

        @Test
        void __paged() {
            final var first = getRepositoryInstance().selectBookingCountsAndTotalsByMonth(PageRequest.ofSize(2));
            assertThat(first.content()).hasSize(2);
            assertThat(first.totalElements()).isEqualTo(3L);
            final var second = getRepositoryInstance().selectBookingCountsAndTotalsByMonth(first.nextPageRequest());
            assertThat(second.content()).extracting(BookingRepository.MonthBookingTotal::bookings)
                    .containsExactly(410670L);
        }

        @Test
        void __nullPageRequest_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().selectBookingCountsAndTotalsByMonth(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
