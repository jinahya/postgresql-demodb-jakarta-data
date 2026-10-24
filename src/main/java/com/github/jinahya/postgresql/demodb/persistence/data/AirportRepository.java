package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Airport;
import com.github.jinahya.postgresql.demodb.persistence.mapped.MappedAirport_;
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
 * A repository for {@link Airport}, the {@code airports} view of the demonstration database.
 * <p>
 * The exercise methods answer the exercises of upstream's {@code EXERCISES.asciidoc} on the
 * {@code airports} view, one method per exercise, each named in its javadoc by the exercise's id.
 */
@Repository
public interface AirportRepository
        extends _Repository<Airport, String>,
                CrudRepository<Airport, String> {

    // ----------------------------------------------------------------------------------- exercises

    /**
     * A country and how many airports it has; a row of
     * {@link #selectAirportCountsByCountry(PageRequest)}.
     *
     * @param country  the country, as the view names it.
     * @param airports the number of airports in the country.
     */
    record CountryAirportCount(String country, long airports) {
    }

    /**
     * Lists the airports of the specified country ({@code DEMODB_AIRPORTS_B_01}, which asks for
     * {@code Norway} and orders by {@link MappedAirport_#AIRPORT_CODE airportCode}).
     * <p>
     * The view's columns are nullable in the mapping, so a {@code null} country is not refused but
     * looked up, and matches the airports with no country, of which the installed data has none.
     *
     * @param country the country to match, as the view names it.
     * @return a list of the airports of {@code country}, by code.
     */
    @Find
    @OrderBy(MappedAirport_.AIRPORT_CODE)
    List<Airport> listByCountry(@By(MappedAirport_.COUNTRY) String country);

    /**
     * Selects the countries with how many airports each has ({@code DEMODB_AIRPORTS_I_01}, which
     * orders by {@code airports} descending, then by {@code country}; the exercise's
     * {@code LIMIT 10} is the first page of ten).
     *
     * @param pageRequest the page to return.
     * @return a page of the countries with their numbers of airports, the most first.
     */
    @Query("""
            select a.country, count(a)
            from Airport a
            group by a.country
            order by count(a) desc, a.country""")
    Page<CountryAirportCount> selectAirportCountsByCountry(PageRequest pageRequest);
}
