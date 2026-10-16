package io.github.justhurky.groundsegment.command;

import io.github.justhurky.groundsegment.command.dto.CancelCommandRequest;
import io.github.justhurky.groundsegment.command.dto.SubmitCommandRequest;
import io.github.justhurky.groundsegment.command.dto.TelecommandResponse;
import io.github.justhurky.groundsegment.common.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Validated
public class TelecommandController {

    private final TelecommandService telecommandService;
    private final CommandCatalogue commandCatalogue;

    public TelecommandController(TelecommandService telecommandService, CommandCatalogue commandCatalogue) {
        this.telecommandService = telecommandService;
        this.commandCatalogue = commandCatalogue;
    }

    @PostMapping("/spacecraft/{spacecraftId}/commands")
    @PreAuthorize("hasAnyRole('OPERATOR','FLIGHT_DIRECTOR','ADMIN')")
    public ResponseEntity<TelecommandResponse> submit(@PathVariable UUID spacecraftId,
                                                      @Valid @RequestBody SubmitCommandRequest request) {
        TelecommandResponse command = telecommandService.submit(spacecraftId, request);
        HttpStatus status = command.state() == CommandState.REJECTED
                ? HttpStatus.UNPROCESSABLE_ENTITY
                : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(command);
    }

    @GetMapping("/spacecraft/{spacecraftId}/commands")
    @PreAuthorize("isAuthenticated()")
    public PageResponse<TelecommandResponse> list(
            @PathVariable UUID spacecraftId,
            @RequestParam(required = false) CommandState state,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(500) int size) {
        return PageResponse.of(telecommandService.search(spacecraftId, state, PageRequest.of(page, size)));
    }

    @GetMapping("/commands/{commandId}")
    @PreAuthorize("isAuthenticated()")
    public TelecommandResponse byId(@PathVariable UUID commandId) {
        return telecommandService.findById(commandId);
    }

    @PostMapping("/commands/{commandId}/cancel")
    @PreAuthorize("hasAnyRole('OPERATOR','FLIGHT_DIRECTOR','ADMIN')")
    public TelecommandResponse cancel(@PathVariable UUID commandId,
                                      @Valid @RequestBody CancelCommandRequest request) {
        return telecommandService.cancel(commandId, request.reason());
    }

    @GetMapping("/commands/catalogue")
    @PreAuthorize("isAuthenticated()")
    public Set<String> catalogue() {
        return commandCatalogue.opcodes();
    }
}
