package org.example.backend.repository;

import org.example.backend.entities.ParkingSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {
    List<ParkingSpot> findByZoneId(Long zoneId);
    long countBySensorIdIsNotNull();
    Optional<ParkingSpot> findBySensorId(String sensorId);


    // Compter les spots par zone et statut
    @Query("SELECT COUNT(p) FROM ParkingSpot p WHERE p.zone.id = :zoneId AND p.status = :status")
    long countByZoneIdAndStatus(@Param("zoneId") Long zoneId, @Param("status") Boolean status);


    // Trouver les spots disponibles dans une zone
    @Query("SELECT ps FROM ParkingSpot ps WHERE ps.zone.id = :zoneId AND ps.status = true")
    List<ParkingSpot> findAvailableSpotsByZoneId(@Param("zoneId") Long zoneId);

    // Compter les spots disponibles dans une zone
    @Query("SELECT COUNT(ps) FROM ParkingSpot ps WHERE ps.zone.id = :zoneId AND ps.status = true")
    long countAvailableSpotsByZoneId(@Param("zoneId") Long zoneId);

    long countByStatus(Boolean status);
}
