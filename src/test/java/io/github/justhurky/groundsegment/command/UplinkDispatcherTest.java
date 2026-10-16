package io.github.justhurky.groundsegment.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UplinkDispatcherTest {

    private static final UUID FIRST = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID SECOND = UUID.fromString("22222222-2222-4222-8222-222222222222");

    @Mock
    private TelecommandRepository telecommandRepository;

    @Mock
    private UplinkPassService uplinkPassService;

    @InjectMocks
    private UplinkDispatcher dispatcher;

    @Test
    void runsOnePassForEverySpacecraftThatHasValidatedCommands() {
        given(telecommandRepository.findSpacecraftIdsWithState(CommandState.VALIDATED))
                .willReturn(List.of(FIRST, SECOND));
        given(uplinkPassService.executePass(any())).willReturn(UplinkPassService.PassReport.noContact(FIRST));

        dispatcher.dispatchPendingCommands();

        then(uplinkPassService).should().executePass(FIRST);
        then(uplinkPassService).should().executePass(SECOND);
    }

    @Test
    void keepsDispatchingWhenOnePassFails() {
        given(telecommandRepository.findSpacecraftIdsWithState(CommandState.VALIDATED))
                .willReturn(List.of(FIRST, SECOND));
        willThrow(new IllegalStateException("database unavailable"))
                .given(uplinkPassService).executePass(FIRST);
        given(uplinkPassService.executePass(SECOND))
                .willReturn(new UplinkPassService.PassReport(SECOND, UUID.randomUUID(), 1, 0, 0));

        dispatcher.dispatchPendingCommands();

        then(uplinkPassService).should().executePass(SECOND);
    }

    @Test
    void doesNothingWhenNoCommandIsWaiting() {
        given(telecommandRepository.findSpacecraftIdsWithState(CommandState.VALIDATED))
                .willReturn(List.of());

        dispatcher.dispatchPendingCommands();

        verifyNoInteractions(uplinkPassService);
    }
}
