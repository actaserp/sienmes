package mes.domain.repository;

import mes.domain.entity.TB_SalesDetail;
import mes.domain.entity.TB_SalesDetailId;
import mes.domain.entity.TB_Salesment;
import mes.domain.entity.TB_SalesmentId;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface TB_SalesDetailRepository extends JpaRepository<TB_SalesDetail, TB_SalesDetailId>  {

    List<TB_SalesDetail> findByIdMisdateAndIdMisnum(String misdate, String misnum);

    @Modifying
    @Transactional
    @Query("DELETE FROM TB_SalesDetail d WHERE CONCAT(d.id.misdate, '_', d.id.misnum) IN :keys")
    void deleteByKeyList(@Param("keys") List<String> keys);

}