package mes.domain.repository;

import groovy.lang.Tuple2;
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


    @Modifying
    @Transactional
    @Query("DELETE FROM TB_SalesDetail d WHERE d.id.misdate = :misdate AND d.id.misnum = :misnum")
    void deleteByMisdateAndMisnum(@Param("misdate") String misdate, @Param("misnum") String misnum);


}