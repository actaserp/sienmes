package mes.domain.repository;

import mes.domain.entity.Balju;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Date;

public interface BujuRepository extends JpaRepository<Balju, Integer> {
  Balju getBujuById(Integer id);

  void deleteByJumunNumberAndJumunDateAndSpjangcd(String jumunNumber, Date jumunDate, String spjangcd);
}
