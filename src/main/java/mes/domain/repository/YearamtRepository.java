package mes.domain.repository;

import mes.domain.entity.Yearamt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface YearamtRepository extends JpaRepository<Yearamt, Integer> {

    Optional<Yearamt> findByCltcdAndIoflagAndYyyymm(Integer cltcd, String ioflag, String yyyymm);

    @Modifying
    @Query("DELETE FROM Yearamt y WHERE y.cltcd = :cltcd AND y.ioflag = :ioflag AND y.yyyymm = :yyyymm")
    void deleteByCltcdAndIoflagAndYyyymm(@Param("cltcd") Integer cltcd, @Param("ioflag") String ioflag, @Param("yyyymm") String yyyymm);

}
