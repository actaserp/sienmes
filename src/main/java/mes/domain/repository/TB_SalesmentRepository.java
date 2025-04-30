package mes.domain.repository;

import mes.domain.entity.TB_ACCOUNT;
import mes.domain.entity.TB_Salesment;
import mes.domain.entity.TB_SalesmentId;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface TB_SalesmentRepository extends JpaRepository<TB_Salesment, TB_SalesmentId>  {
    @Query("SELECT MAX(s.id.misnum) FROM TB_Salesment s WHERE s.id.misdate = :misdate")
    Optional<String> findMaxMisnumByMisdate(@Param("misdate") String misdate);

    @Modifying
    @Transactional
    @Query("DELETE FROM TB_Salesment m WHERE CONCAT(m.id.misdate, '_', m.id.misnum) IN :keys")
    void deleteByKeyList(@Param("keys") List<String> keys);


}