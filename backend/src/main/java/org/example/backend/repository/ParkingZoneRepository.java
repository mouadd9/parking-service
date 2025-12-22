package org.example.backend.repository;

import org.example.backend.entities.ParkingZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParkingZoneRepository extends JpaRepository<ParkingZone, Long> {
}
