package org.example.backend.service;

import org.example.backend.DTO.ParkingSpotDTO;
import org.example.backend.DTO.ParkingZoneDTO;
import org.example.backend.entities.ParkingZone;
import org.example.backend.mappers.ParkingSpotMapper;
import org.example.backend.mappers.ParkingZoneMapper;
import org.example.backend.repository.ParkingSpotRepository;
import org.example.backend.repository.ParkingZoneRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ParkingZoneService {

    private final ParkingZoneRepository repository;
    private final ParkingZoneMapper mapper;
    private final ParkingSpotRepository spotRepository;
    private final ParkingSpotMapper spotMapper;

    public ParkingZoneService (ParkingZoneRepository parkingZoneRepository, ParkingZoneMapper parkingZoneMapper, ParkingSpotRepository spotRepository, ParkingSpotMapper spotMapper) {
        this.repository = parkingZoneRepository;
        this.mapper = parkingZoneMapper;
        this.spotRepository = spotRepository;
        this.spotMapper = spotMapper;
    }

    public List<ParkingZoneDTO> getAllZones() {
        // 1. Récupérer toutes les entités de la BDD
        List<ParkingZone> zones = repository.findAll();

        // 2. Transformer chaque entité en DTO via le Mapper
        return zones.stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }
    public List<ParkingSpotDTO> getSpotsByZone(Long zoneId) {
        // 1. On vérifie si la zone existe (optionnel mais propre)
        if (!repository.existsById(zoneId)) {
            throw new RuntimeException("Zone introuvable avec l'ID : " + zoneId);
        }

        // 2. On récupère les places
        return spotRepository.findByZoneId(zoneId).stream()
                .map(spotMapper::toDTO)
                .collect(Collectors.toList());
    }

}