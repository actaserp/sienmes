package mes.domain.repository;

import mes.domain.entity.Accmanage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface AccmanageRepository  extends JpaRepository<Accmanage, String> {

    Optional<Accmanage> findByAcccd(String id);

}
