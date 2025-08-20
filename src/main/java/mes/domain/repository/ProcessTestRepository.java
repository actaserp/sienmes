package mes.domain.repository;

import mes.domain.entity.ProcessTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessTestRepository extends JpaRepository<ProcessTest, Integer> {
    // job_res_id 기준으로 단건 조회
    Optional<ProcessTest> findByJobResId(Integer jobResId);
}
