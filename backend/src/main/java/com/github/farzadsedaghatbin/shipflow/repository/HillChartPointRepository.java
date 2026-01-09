package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.HillChartPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HillChartPointRepository extends JpaRepository<HillChartPoint, Long> {
    List<HillChartPoint> findByPitchId(Long pitchId);
    List<HillChartPoint> findByPitchIdOrderByUpdatedAtDesc(Long pitchId);
    void deleteByPitchId(Long pitchId);
}
