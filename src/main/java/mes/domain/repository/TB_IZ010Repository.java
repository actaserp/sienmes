package mes.domain.repository;

import mes.domain.entity.TB_IZ010;
import mes.domain.entity.TB_IZ010Id;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TB_IZ010Repository extends JpaRepository<TB_IZ010, TB_IZ010Id> {
}
