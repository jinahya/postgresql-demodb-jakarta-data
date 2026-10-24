package com.github.jinahya.postgresql.demodb.persistence.data;

import com.github.jinahya.postgresql.demodb.persistence.Airplane;
import com.github.jinahya.postgresql.demodb.persistence.mapped.MappedAirplane_;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;

/**
 * A repository for {@link Airplane}, the {@code airplanes} view of the demonstration database.
 * <p>
 * The exercise methods answer the exercises of upstream's {@code EXERCISES.asciidoc} on the
 * {@code airplanes} view, one method per exercise, each named in its javadoc by the exercise's id.
 */
@Repository
public interface AirplaneRepository
        extends _Repository<Airplane, String>,
                CrudRepository<Airplane, String> {

    // ----------------------------------------------------------------------------------- exercises

    /**
     * A maker and how many airplanes of the view are its; a row of
     * {@link #selectAirplaneCountsByMaker(PageRequest)}.
     *
     * @param maker     the first word of the model.
     * @param airplanes the number of airplanes of the maker.
     */
    record MakerAirplaneCount(String maker, long airplanes) {
    }

    /**
     * Lists every airplane of the view ({@code DEMODB_AIRPLANES_B_01}, which orders by
     * {@link MappedAirplane_#AIRPLANE_CODE airplaneCode}).
     *
     * @return a list of every airplane, by code.
     */
    @Find
    @OrderBy(MappedAirplane_.AIRPLANE_CODE)
    List<Airplane> listAll();

    /**
     * Selects how many airplanes each maker has, the maker being the first word of the model
     * ({@code DEMODB_AIRPLANES_I_01}, which orders by {@code airplanes} descending, then by
     * {@code maker}).
     * <p>
     * JPQL has no {@code SPLIT_PART}; the maker is the model up to its first space. The order is
     * in the query.
     *
     * @param pageRequest the page to return.
     * @return a page of the makers with their numbers of airplanes, the most first.
     */
    @Query("""
            select substring(a.model, 1, locate(' ', a.model) - 1), count(a)
            from Airplane a
            group by substring(a.model, 1, locate(' ', a.model) - 1)
            order by count(a) desc, substring(a.model, 1, locate(' ', a.model) - 1)""")
    Page<MakerAirplaneCount> selectAirplaneCountsByMaker(PageRequest pageRequest);
}
