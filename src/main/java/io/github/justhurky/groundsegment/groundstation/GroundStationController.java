package io.github.justhurky.groundsegment.groundstation;

import io.github.justhurky.groundsegment.groundstation.dto.ContactWindowResponse;
import io.github.justhurky.groundsegment.groundstation.dto.GroundStationResponse;
import io.github.justhurky.groundsegment.groundstation.dto.RegisterGroundStationRequest;
import io.github.justhurky.groundsegment.groundstation.dto.ScheduleContactWindowRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class GroundStationController {

    private final GroundStationService groundStationService;

    public GroundStationController(GroundStationService groundStationService) {
        this.groundStationService = groundStationService;
    }

    @PostMapping("/ground-stations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public GroundStationResponse register(@Valid @RequestBody RegisterGroundStationRequest request) {
        return groundStationService.register(request);
    }

    @GetMapping("/ground-stations")
    @PreAuthorize("isAuthenticated()")
    public List<GroundStationResponse> list() {
        return groundStationService.findAll();
    }

    @PostMapping("/spacecraft/{spacecraftId}/contact-windows")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OPERATOR','FLIGHT_DIRECTOR','ADMIN')")
    public ContactWindowResponse schedule(@PathVariable UUID spacecraftId,
                                          @Valid @RequestBody ScheduleContactWindowRequest request) {
        return groundStationService.scheduleWindow(spacecraftId, request);
    }

    @GetMapping("/spacecraft/{spacecraftId}/contact-windows")
    @PreAuthorize("isAuthenticated()")
    public List<ContactWindowResponse> windows(@PathVariable UUID spacecraftId) {
        return groundStationService.windowsOf(spacecraftId);
    }
}
