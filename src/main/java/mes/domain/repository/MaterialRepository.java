package mes.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

	List<Material> findBySpjangcd(String spjangcd);

    Integer findIdByName(String materialName);

	Material findByName(String matName);

	@Query(value = "SELECT MAX(CAST(\"Code\" AS INTEGER)) FROM material WHERE \"Code\" LIKE '4000%'", nativeQuery = true)
	String findMaxCodeBy4000Prefix();

	boolean existsByCode(String s);
}
