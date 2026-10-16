package io.github.justhurky.groundsegment.spacecraft;

import io.github.justhurky.groundsegment.spacecraft.dto.MissionPhaseChangeRequest;
import io.github.justhurky.groundsegment.spacecraft.dto.ParameterDefinitionRequest;
import io.github.justhurky.groundsegment.spacecraft.dto.ParameterDefinitionResponse;
import io.github.justhurky.groundsegment.spacecraft.dto.RegisterSpacecraftRequest;
import io.github.justhurky.groundsegment.spacecraft.dto.SpacecraftRegistrationResponse;
import io.github.justhurky.groundsegment.spacecraft.dto.SpacecraftResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/spacecraft")
public class SpacecraftController {

    private final SpacecraftService spacecraftService;

    public SpacecraftController(SpacecraftService spacecraftService) {
        this.spacecraftService = spacecraftService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SpacecraftRegistrationResponse register(@Valid @RequestBody RegisterSpacecraftRequest request) {
        return spacecraftService.register(request);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<SpacecraftResponse> list() {
        return spacecraftService.findAll();
    }

    @GetMapping("/{spacecraftId}")
    @PreAuthorize("isAuthenticated()")
    public SpacecraftResponse byId(@PathVariable UUID spacecraftId) {
        return spacecraftService.findById(spacecraftId);
    }

    @PutMapping("/{spacecraftId}/mission-phase")
    @PreAuthorize("hasAnyRole('FLIGHT_DIRECTOR','ADMIN')")
    public SpacecraftResponse changeMissionPhase(@PathVariable UUID spacecraftId,
                                                 @Valid @RequestBody MissionPhaseChangeRequest request) {
        return spacecraftService.changeMissionPhase(spacecraftId, request.targetPhase(), request.reason());
    }

    @PostMapping("/{spacecraftId}/parameters")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','FLIGHT_DIRECTOR')")
    public ParameterDefinitionResponse defineParameter(@PathVariable UUID spacecraftId,
                                                       @Valid @RequestBody ParameterDefinitionRequest request) {
        return spacecraftService.defineParameter(spacecraftId, request);
    }

    @GetMapping("/{spacecraftId}/parameters")
    @PreAuthorize("isAuthenticated()")
    public List<ParameterDefinitionResponse> parameters(@PathVariable UUID spacecraftId) {
        return spacecraftService.parametersOf(spacecraftId);
    }
}
