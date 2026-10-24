package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Route;
import com.github.jinahya.postgresql.demodb.persistence.RouteId;
import com.github.jinahya.postgresql.demodb.persistence.mapped.MappedRoute_;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.By;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;

/**
 * A repository for {@link Route}, the {@code routes} table of the demonstration database.
 * <p>
 * The exercise methods answer the exercises of upstream's {@code EXERCISES.asciidoc} on the
 * {@code routes} table, one method per exercise, each named in its javadoc by the exercise's id.
 */
@Repository
public interface RouteRepository
        extends _Repository<Route, RouteId>,
                CrudRepository<Route, RouteId> {

    // ----------------------------------------------------------------------------------- exercises

    /**
     * Lists the routes from the specified airport ({@code DEMODB_ROUTES_B_01}, which asks for
     * {@code SVO} and orders by {@link MappedRoute_#ROUTE_NO routeNo}, then by
     * {@link MappedRoute_#VALIDITY validity}).
     * <p>
     * The airport is matched by its code, the {@code departure_airport} column, through the
     * inherited read-only attribute.
     *
     * @param departureAirport the code of the airport of departure.
     * @return a list of the routes from {@code departureAirport}, by route number and period.
     */
    @Find
    @OrderBy(MappedRoute_.ROUTE_NO)
    @OrderBy(MappedRoute_.VALIDITY)
    List<Route> listByDepartureAirport(@By(MappedRoute_.DEPARTURE_AIRPORT) String departureAirport);

    /**
     * Lists the periods of the specified route, each a row of the table
     * ({@code DEMODB_ROUTES_I_01}, which asks for {@code PG0001} and orders by
     * {@link MappedRoute_#VALIDITY validity}).
     * <p>
     * The exercise shows the bounds of each period, which are PostgreSQL's {@code LOWER} and
     * {@code UPPER} of the range; JPQL has neither, so the route is selected whole, with its
     * {@code validity} as text. The order is in the query. A {@code @Query} parameter is bound
     * unchecked; a {@code null} route number matches nothing.
     *
     * @param routeNo     the route number.
     * @param pageRequest the page to return.
     * @return a page of the periods of the route, the earliest first.
     */
    @Query("""
            select r
            from Route r
            where r.routeNo = :routeNo
            order by r.validity""")
    Page<Route> listByRouteNo(@Param("routeNo") String routeNo, PageRequest pageRequest);
}
