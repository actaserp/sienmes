package mes.domain.repository;

import mes.domain.entity.TB_Pb201;
import mes.domain.entity.TB_Pb201Id;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TB_Pb201Repository extends JpaRepository<TB_Pb201, TB_Pb201Id> {

}
