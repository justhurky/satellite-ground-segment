package io.github.justhurky.groundsegment.groundstation;

import io.github.justhurky.groundsegment.audit.AuditAction;
import io.github.justhurky.groundsegment.audit.AuditService;
import io.github.justhurky.groundsegment.common.error.DuplicateResourceException;
import io.github.justhurky.groundsegment.common.error.ResourceNotFoundException;
import io.github.justhurky.groundsegment.groundstation.dto.ContactWindowResponse;
import io.github.justhurky.groundsegment.groundstation.dto.GroundStationResponse;
import io.github.justhurky.groundsegment.groundstation.dto.RegisterGroundStationRequest;
import io.github.justhurky.groundsegment.groundstation.dto.ScheduleContactWindowRequest;
import io.github.justhurky.groundsegment.spacecraft.Spacecraft;
import io.github.justhurky.groundsegment.spacecraft.SpacecraftService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroundStationService {

    private final GroundStationRepository groundStationRepository;
    private final ContactWindowRepository contactWindowRepository;
    private final SpacecraftService spacecraftService;
    private final AuditService auditService;

    public GroundStationService(GroundStationRepository groundStationRepository,
                                ContactWindowRepository contactWindowRepository,
                                SpacecraftService spacecraftService,
                                AuditService auditService) {
        this.groundStationRepository = groundStationRepository;
        this.contactWindowRepository = contactWindowRepository;
        this.spacecraftService = spacecraftService;
        this.auditService = auditService;
    }

    @Transactional
    public GroundStationResponse register(RegisterGroundStationRequest request) {
        if (groundStationRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Ground station " + request.code() + " is already registered");
        }
        GroundStation station = groundStationRepository.save(new GroundStation(
                request.code(), request.name(), request.latitudeDegrees(),
                request.longitudeDegrees(), request.altitudeMetres()));
        auditService.record(AuditAction.GROUND_STATION_REGISTERED, "ground-station", station.getId().toString(),
                Map.of("code", station.getCode()));
        return GroundStationResponse.from(station);
    }

    @Transactional(readOnly = true)
    public List<GroundStationResponse> findAll() {
        return groundStationRepository.findAll(Sort.by("code")).stream()
                .map(GroundStationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public GroundStation requireByCode(String code) {
        return groundStationRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("ground-station", code));
    }

    @Transactional
    public ContactWindowResponse scheduleWindow(UUID spacecraftId, ScheduleContactWindowRequest request) {
        Spacecraft spacecraft = spacecraftService.require(spacecraftId);
        GroundStation station = requireByCode(request.groundStationCode());
        if (contactWindowRepository.overlaps(spacecraftId, station.getId(), request.startsAt(), request.endsAt())) {
            throw new DuplicateResourceException("An overlapping contact window is already scheduled for "
                    + station.getCode());
        }
        ContactWindow window = contactWindowRepository.save(new ContactWindow(
                spacecraft, station, request.startsAt(), request.endsAt(), request.uplinkBudgetBytes()));
        auditService.record(AuditAction.CONTACT_WINDOW_SCHEDULED, "contact-window", window.getId().toString(),
                Map.of("spacecraftId", spacecraftId.toString(),
                        "groundStation", station.getCode(),
                        "startsAt", request.startsAt().toString(),
                        "endsAt", request.endsAt().toString()));
        return ContactWindowResponse.from(window);
    }

    @Transactional(readOnly = true)
    public List<ContactWindowResponse> windowsOf(UUID spacecraftId) {
        spacecraftService.require(spacecraftId);
        return contactWindowRepository.findAllBySpacecraftIdOrderByStartsAtAsc(spacecraftId).stream()
                .map(ContactWindowResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ContactWindow> openWindow(UUID spacecraftId, Instant instant) {
        return contactWindowRepository.findOpenWindows(spacecraftId, instant).stream()
                .filter(window -> window.remainingBudgetBytes() > 0)
                .findFirst();
    }
}
