package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.AirplaneData;
import com.github.jinahya.postgresql.demodb.persistence.mapped.MappedAirplaneData_;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;

/**
 * A repository for {@link AirplaneData}, the {@code airplanes_data} table of the demonstration database.
 * <p>
 * The exercise methods answer the exercises of upstream's {@code EXERCISES.asciidoc} on the
 * {@code airplanes_data} table, one method per exercise, each named in its javadoc by the exercise's
 * id.
 */
@Repository
public interface AirplaneDataRepository
        extends _Repository<AirplaneData, String>,
                CrudRepository<AirplaneData, String> {

    // ----------------------------------------------------------------------------------- exercises

    /**
     * An airplane and the hours it flies at its full range; a row of
     * {@link #selectHoursAtFullRange(PageRequest)}.
     *
     * @param airplane the airplane.
     * @param hours    the airplane's range over its speed, in hours, unrounded.
     */
    record AirplaneHours(AirplaneData airplane, Double hours) {
    }

    /**
     * Lists every airplane ({@code DEMODB_AIRPLANES_DATA_B_01}, which orders by
     * {@link MappedAirplaneData_#AIRPLANE_CODE airplaneCode}).
     *
     * @return a list of every airplane, by code.
     */
    @Find
    @OrderBy(MappedAirplaneData_.AIRPLANE_CODE)
    List<AirplaneData> listAll();

    /**
     * Selects every airplane with the hours it flies at its full range, the range over the speed
     * ({@code DEMODB_AIRPLANES_DATA_I_02}, which orders by {@code hours} descending, then by
     * {@link MappedAirplaneData_#AIRPLANE_CODE airplaneCode}).
     * <p>
     * The order is in the query; the exercise's {@code ROUND(..., 2)} is left to the caller, the
     * hours coming back as the provider computes them.
     *
     * @param pageRequest the page to return.
     * @return a page of the airplanes with their hours, the longest first.
     */
    @Query("""
            select a, a.range * 1.0 / a.speed
            from AirplaneData a
            order by a.range * 1.0 / a.speed desc, a.airplaneCode""")
    Page<AirplaneHours> selectHoursAtFullRange(PageRequest pageRequest);
}
