package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.AirportData;
import com.github.jinahya.postgresql.demodb.persistence.mapped.MappedAirportData_;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.By;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;

/**
 * A repository for {@link AirportData}, the {@code airports_data} table of the demonstration
 * database.
 * <p>
 * The exercise methods answer the exercises of upstream's {@code EXERCISES.asciidoc} on the
 * {@code airports_data} table, one method per exercise, each named in its javadoc by the exercise's
 * id.
 */
@Repository
public interface AirportDataRepository
        extends _Repository<AirportData, String>,
                CrudRepository<AirportData, String> {

    // ----------------------------------------------------------------------------------- exercises

    /**
     * A time zone and how many airports are in it; a row of
     * {@link #selectAirportCountsByTimezone(PageRequest)}.
     *
     * @param timezone the time zone.
     * @param airports the number of airports in the time zone.
     */
    record TimezoneAirportCount(String timezone, long airports) {
    }

    /**
     * Lists the airports in the specified time zone ({@code DEMODB_AIRPORTS_DATA_B_01}, which asks
     * for {@code Europe/Moscow} and orders by {@link MappedAirportData_#AIRPORT_CODE airportCode}).
     *
     * @param timezone the time zone to match.
     * @return a list of the airports in {@code timezone}, by code.
     */
    @Find
    @OrderBy(MappedAirportData_.AIRPORT_CODE)
    List<AirportData> listByTimezone(@By(MappedAirportData_.TIMEZONE) String timezone);

    /**
     * Selects the time zones with how many airports each has
     * ({@code DEMODB_AIRPORTS_DATA_I_02}, which orders by {@code airports} descending, then by
     * {@code timezone}; the exercise's {@code LIMIT 10} is the first page of ten).
     *
     * @param pageRequest the page to return.
     * @return a page of the time zones with their numbers of airports, the most first.
     */
    @Query("""
            select a.timezone, count(a)
            from AirportData a
            group by a.timezone
            order by count(a) desc, a.timezone""")
    Page<TimezoneAirportCount> selectAirportCountsByTimezone(PageRequest pageRequest);
}
