package mes.domain.repository;

//import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import mes.domain.entity.Suju;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Repository 
public interface SujuRepository extends JpaRepository<Suju, Integer>{

	
	Suju getSujuById(Integer id);

	Suju findByIdAndState(Integer sujuPk, String string);

	@Transactional(readOnly = true)
    List<Suju> findByIdIn(List<Integer> ids);
}
