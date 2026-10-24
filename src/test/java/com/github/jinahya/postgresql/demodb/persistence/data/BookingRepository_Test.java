package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Booking;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The unit tests of {@link BookingRepository}.
 */
@AddBeanClasses(BookingRepository_.class)
class BookingRepository_Test
        extends _Repository_Test<BookingRepository, Booking, String> {

    BookingRepository_Test() {
        super(BookingRepository.class, Booking.class, String.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    private static OffsetDateTime utc(final int year, final int month, final int day, final int hour,
                                      final int minute, final int second) {
        return OffsetDateTime.of(year, month, day, hour, minute, second, 0, ZoneOffset.UTC);
    }

    private Booking persistBooking(final EntityManager em, final OffsetDateTime bookDate, final String amount) {
        final var booking = ObjectRandomizerUtils.newRandomizedInstanceOf(Booking.class).orElseThrow();
        booking.setBookDate(bookDate);
        booking.setTotalAmount(new BigDecimal(amount));
        em.persist(booking);
        return booking;
    }

    // -----------------------------------------------------------------------------------------------------------------

    @DisplayName("DEMODB_BOOKINGS_B_01: listByBookDateBetween(from, to)")
    @Nested
    class ListByBookDateBetweenTest {

        private static final OffsetDateTime FROM = utc(2031, 9, 1, 0, 0, 0);

        private static final OffsetDateTime TO = utc(2031, 9, 1, 0, 1, 0);

        // two within the minute, out of order, one at its end, one before it
        private String[] persist() {
            return entityManagerFactory.callInTransaction(em -> new String[] {
                    persistBooking(em, utc(2031, 9, 1, 0, 0, 40), "5000.00").getBookRef(),
                    persistBooking(em, utc(2031, 9, 1, 0, 0, 5), "5000.00").getBookRef(),
                    persistBooking(em, utc(2031, 9, 1, 0, 1, 0), "5000.00").getBookRef(),
                    persistBooking(em, utc(2031, 8, 31, 23, 59, 59), "5000.00").getBookRef()
            });
        }

        @Test
        void __returnsBookingsOfThePeriod() {
            final var refs = persist();
            final var list = getRepositoryInstance().listByBookDateBetween(FROM, TO);
            assertThat(list).extracting(Booking::getBookRef).containsExactlyInAnyOrder(refs[0], refs[1]);
        }

        @Test
        void __orderedByBookDate() {
            final var refs = persist();
            final var list = getRepositoryInstance().listByBookDateBetween(FROM, TO);
            assertThat(list).extracting(Booking::getBookRef).containsExactly(refs[1], refs[0]);
        }

        @Test
        void __emptyWhenNoneMatches() {
            assertThat(getRepositoryInstance().listByBookDateBetween(FROM, TO)).isEmpty();
        }

        // a @Query parameter is bound as it is, unchecked; a comparison with NULL matches nothing
        @Test
        void __nullArguments_matchNothing() {
            persist();
            final var repository = getRepositoryInstance();
            assertThat(repository.listByBookDateBetween(null, TO)).isEmpty();
            assertThat(repository.listByBookDateBetween(FROM, null)).isEmpty();
        }
    }

    @DisplayName("DEMODB_BOOKINGS_I_01: selectBookingCountsAndTotalsByMonth(pageRequest)")
    @Nested
    class SelectBookingCountsAndTotalsByMonthTest {

        // two in September 2031, one in October 2031, one in January 2032
        private void persist() {
            entityManagerFactory.runInTransaction(em -> {
                persistBooking(em, utc(2031, 10, 15, 9, 0, 0), "3000.00");
                persistBooking(em, utc(2031, 9, 30, 23, 59, 59), "1000.00");
                persistBooking(em, utc(2032, 1, 1, 0, 0, 0), "4000.00");
                persistBooking(em, utc(2031, 9, 1, 0, 0, 0), "1500.00");
            });
        }

        @Test
        void __returnsMonthsWithCountsAndTotals() {
            persist();
            final var page = getRepositoryInstance().selectBookingCountsAndTotalsByMonth(PageRequest.ofSize(10));
            assertThat(page.content())
                    .extracting(BookingRepository.MonthBookingTotal::year, BookingRepository.MonthBookingTotal::month,
                                BookingRepository.MonthBookingTotal::bookings,
                                t -> t.total().compareTo(BigDecimal.ZERO) == 0 ? "0" : t.total().stripTrailingZeros().toPlainString())
                    .containsExactly(tuple(2031, 9, 2L, "2500"), tuple(2031, 10, 1L, "3000"),
                                     tuple(2032, 1, 1L, "4000"));
        }

        @Test
        void __paged() {
            persist();
            final var first = getRepositoryInstance().selectBookingCountsAndTotalsByMonth(PageRequest.ofSize(2));
            assertThat(first.content()).hasSize(2);
            assertThat(first.totalElements()).isEqualTo(3L);
            final var second = getRepositoryInstance().selectBookingCountsAndTotalsByMonth(first.nextPageRequest());
            assertThat(second.content())
                    .extracting(BookingRepository.MonthBookingTotal::year, BookingRepository.MonthBookingTotal::month)
                    .containsExactly(tuple(2032, 1));
        }

        @Test
        void __nullPageRequest_npe() {
            assertThatThrownBy(() -> getRepositoryInstance().selectBookingCountsAndTotalsByMonth(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    @Inject
    private EntityManagerFactory entityManagerFactory;
}
