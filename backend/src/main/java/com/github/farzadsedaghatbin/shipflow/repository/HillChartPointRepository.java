package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.HillChartPoint;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HillChartPointRepository extends JpaRepository<HillChartPoint, Long> {
  List<HillChartPoint> findByPitchId(Long pitchId);

  List<HillChartPoint> findByPitchIdOrderByUpdatedAtDesc(Long pitchId);

  void deleteByPitchId(Long pitchId);

  @Query("SELECT h FROM HillChartPoint h WHERE LOWER(h.scope) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(h.description) LIKE LOWER(CONCAT('%', :query, '%'))")
  List<HillChartPoint> searchHillChartPoints(@Param("query") String query);
}
