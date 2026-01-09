package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.entity.HillChartPoint;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.repository.HillChartPointRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final PitchRepository pitchRepository;
    private final HillChartPointRepository hillChartPointRepository;

    @PostMapping("/seed-hill-chart-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> seedHillChartData() {
        List<Pitch> pitches = pitchRepository.findAll();
        
        if (pitches.isEmpty()) {
            return ResponseEntity.badRequest().body("No pitches found. Please create pitches first.");
        }

        int count = 0;
        for (Pitch pitch : pitches) {
            // Check if pitch already has hill chart points
            List<HillChartPoint> existing = hillChartPointRepository.findByPitchId(pitch.getId());
            if (!existing.isEmpty()) {
                continue; // Skip if already has data
            }

            // Add sample hill chart points
            addSamplePoint(pitch, "Backend API Development", "Build RESTful API endpoints and business logic", 75);
            addSamplePoint(pitch, "Database Schema", "Design and implement database tables and relationships", 95);
            addSamplePoint(pitch, "Frontend Components", "Create reusable UI components and pages", 45);
            addSamplePoint(pitch, "Authentication & Security", "Implement JWT auth and role-based access control", 60);
            addSamplePoint(pitch, "Testing & Documentation", "Write unit tests and API documentation", 25);
            
            count++;
        }

        return ResponseEntity.ok("Added sample hill chart data to " + count + " pitch(es)");
    }

    private void addSamplePoint(Pitch pitch, String scope, String description, int position) {
        HillChartPoint point = new HillChartPoint();
        point.setPitch(pitch);
        point.setScope(scope);
        point.setDescription(description);
        point.setPosition(position);
        hillChartPointRepository.save(point);
    }
}
