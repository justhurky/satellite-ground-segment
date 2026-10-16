package io.github.justhurky.groundsegment.bootstrap;

import io.github.justhurky.groundsegment.groundstation.GroundStationService;
import io.github.justhurky.groundsegment.groundstation.dto.RegisterGroundStationRequest;
import io.github.justhurky.groundsegment.groundstation.dto.ScheduleContactWindowRequest;
import io.github.justhurky.groundsegment.identity.AppUserRepository;
import io.github.justhurky.groundsegment.identity.Role;
import io.github.justhurky.groundsegment.identity.UserService;
import io.github.justhurky.groundsegment.identity.dto.CreateUserRequest;
import io.github.justhurky.groundsegment.spacecraft.SpacecraftService;
import io.github.justhurky.groundsegment.spacecraft.dto.ParameterDefinitionRequest;
import io.github.justhurky.groundsegment.spacecraft.dto.RegisterSpacecraftRequest;
import io.github.justhurky.groundsegment.spacecraft.dto.SpacecraftRegistrationResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile(DemoDataSeeder.DEMO_PROFILE)
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_PROFILE = "demo";
    public static final String SPACECRAFT_NAME = "HUSAT-1";
    public static final int SPACECRAFT_NORAD_ID = 48215;

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserService userService;
    private final AppUserRepository userRepository;
    private final SpacecraftService spacecraftService;
    private final GroundStationService groundStationService;
    private final DemoProperties demoProperties;
    private final Clock clock;

    public DemoDataSeeder(UserService userService, AppUserRepository userRepository,
                          SpacecraftService spacecraftService, GroundStationService groundStationService,
                          DemoProperties demoProperties, Clock clock) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.spacecraftService = spacecraftService;
        this.groundStationService = groundStationService;
        this.demoProperties = demoProperties;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        seedUsers();
        seedGroundStations();
        SpacecraftRegistrationResponse registration = seedSpacecraft();
        seedParameters(registration);
        seedContactWindow(registration);
        log.info("Demo profile seeded spacecraft {} with id {}", SPACECRAFT_NAME,
                registration.spacecraft().id());
        log.info("Demo profile telemetry authentication key: {}",
                registration.telemetryAuthenticationKeyBase64());
    }

    private void seedUsers() {
        userService.create(new CreateUserRequest("admin", demoProperties.password(),
                EnumSet.of(Role.ADMIN)));
        userService.create(new CreateUserRequest("director", demoProperties.password(),
                EnumSet.of(Role.FLIGHT_DIRECTOR)));
        userService.create(new CreateUserRequest("operator", demoProperties.password(),
                EnumSet.of(Role.OPERATOR)));
        userService.create(new CreateUserRequest("analyst", demoProperties.password(),
                EnumSet.of(Role.ANALYST)));
        userService.create(new CreateUserRequest("auditor", demoProperties.password(),
                EnumSet.of(Role.AUDITOR)));
        userService.create(new CreateUserRequest("station-kir", demoProperties.password(),
                EnumSet.of(Role.GROUND_STATION)));
    }

    private void seedGroundStations() {
        groundStationService.register(new RegisterGroundStationRequest(
                "KIR", "Kiruna", 67.8558, 20.2253, 391.0));
        groundStationService.register(new RegisterGroundStationRequest(
                "SVB", "Svalbard", 78.2297, 15.3975, 458.0));
    }

    private SpacecraftRegistrationResponse seedSpacecraft() {
        return spacecraftService.register(new RegisterSpacecraftRequest(
                SPACECRAFT_NAME, SPACECRAFT_NORAD_ID, demoProperties.commandPublicKeyBase64()));
    }

    private void seedParameters(SpacecraftRegistrationResponse registration) {
        List<ParameterDefinitionRequest> parameters = List.of(
                new ParameterDefinitionRequest(1, "BATTERY_VOLTAGE", "V", 22.0, 24.0, 32.0, 34.0),
                new ParameterDefinitionRequest(2, "BATTERY_CURRENT", "A", -12.0, -8.0, 8.0, 12.0),
                new ParameterDefinitionRequest(3, "PANEL_TEMPERATURE", "C", -45.0, -30.0, 70.0, 85.0),
                new ParameterDefinitionRequest(4, "OBC_TEMPERATURE", "C", -20.0, -10.0, 55.0, 70.0),
                new ParameterDefinitionRequest(5, "REACTION_WHEEL_RPM", "rpm", -6000.0, -5000.0, 5000.0, 6000.0),
                new ParameterDefinitionRequest(6, "DOWNLINK_SNR", "dB", 3.0, 6.0, null, null));
        parameters.forEach(parameter ->
                spacecraftService.defineParameter(registration.spacecraft().id(), parameter));
    }

    private void seedContactWindow(SpacecraftRegistrationResponse registration) {
        Instant now = clock.instant();
        groundStationService.scheduleWindow(registration.spacecraft().id(),
                new ScheduleContactWindowRequest("KIR", now.minus(Duration.ofMinutes(5)),
                        now.plus(Duration.ofHours(12)), 65536));
    }
}
