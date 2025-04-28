package mes.domain.repository;

import mes.domain.entity.Yearamt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface YearamtRepository extends JpaRepository<Yearamt, Integer> {
}
