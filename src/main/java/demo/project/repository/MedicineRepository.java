package demo.project.repository;

import demo.project.model.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    @Query("select coalesce(sum(m.stock), 0) from Medicine m")
    long sumStock();

    @Query("select count(m) > 0 from Medicine m where lower(trim(m.name)) = lower(trim(:name))")
    boolean existsByNameIgnoreCaseTrimmed(@Param("name") String name);

    @Query("select count(m) > 0 from Medicine m where lower(trim(m.name)) = lower(trim(:name)) and m.id <> :id")
    boolean existsByNameIgnoreCaseTrimmedAndIdNot(@Param("name") String name, @Param("id") Long id);
}
