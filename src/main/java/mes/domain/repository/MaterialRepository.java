package mes.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import mes.domain.entity.Material;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Integer>{

	Material getMaterialById(Integer matPk);
	
	Integer countByIdAndStoreHouseIdIsNull(Integer id);


	Material findByCode(String matCode);

    List<Material> findByIdIn(Collection<Integer> matIds);

}
