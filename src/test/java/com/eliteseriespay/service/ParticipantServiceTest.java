package com.eliteseriespay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.exception.NotFoundException;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.repository.ParticipantRepository;
import com.eliteseriespay.support.TestEntities;
import com.eliteseriespay.validation.ValidationError;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParticipantServiceTest {

    @Mock
    private ParticipantRepository participantRepository;

    @InjectMocks
    private ParticipantService participantService;

    @Test
    void create_trimsFieldsAndSavesParticipant() {
        when(participantRepository.findByVkId("12345")).thenReturn(Optional.empty());
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Participant created = participantService.create("  12345  ", "  Ivan  ", "  note  ");

        ArgumentCaptor<Participant> participantCaptor = ArgumentCaptor.forClass(Participant.class);
        verify(participantRepository).save(participantCaptor.capture());

        assertThat(participantCaptor.getValue().getVkId()).isEqualTo("12345");
        assertThat(participantCaptor.getValue().getName()).isEqualTo("Ivan");
        assertThat(participantCaptor.getValue().getComment()).isEqualTo("note");
        assertThat(created).isEqualTo(participantCaptor.getValue());
    }

    @Test
    void create_normalizesBlankCommentToNull() {
        when(participantRepository.findByVkId("12345")).thenReturn(Optional.empty());
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Participant created = participantService.create("12345", "Ivan", "   ");

        assertThat(created.getComment()).isNull();
    }

    @Test
    void create_rejectsBlankVkId() {
        assertThatThrownBy(() -> participantService.create("  ", "Ivan", null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.VK_ID_REQUIRED));
    }

    @Test
    void create_rejectsBlankName() {
        assertThatThrownBy(() -> participantService.create("12345", "  ", null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.PARTICIPANT_NAME_REQUIRED));
    }

    @Test
    void create_rejectsDuplicateVkId() {
        Participant existing = TestEntities.participant(2L, "12345", "Other", null);
        when(participantRepository.findByVkId("12345")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> participantService.create("12345", "Ivan", null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.VK_ID_ALREADY_EXISTS));
    }

    @Test
    void findOrCreate_createsParticipantWhenMissing() {
        when(participantRepository.findByVkId("12345")).thenReturn(Optional.empty());
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> {
            Participant saved = invocation.getArgument(0);
            TestEntities.setId(saved, 10L);
            return saved;
        });

        Participant participant = participantService.findOrCreate("  12345  ", "  Ivan  ", "  note  ");

        assertThat(participant.getId()).isEqualTo(10L);
        assertThat(participant.getVkId()).isEqualTo("12345");
        assertThat(participant.getName()).isEqualTo("Ivan");
        assertThat(participant.getComment()).isEqualTo("note");
        verify(participantRepository).save(any(Participant.class));
    }

    @Test
    void findOrCreate_reusesExistingParticipant() {
        Participant existing = TestEntities.participant(10L, "12345", "Ivan", null);
        when(participantRepository.findByVkId("12345")).thenReturn(Optional.of(existing));

        Participant participant = participantService.findOrCreate("12345", "Ivan", null);

        assertThat(participant).isEqualTo(existing);
        verify(participantRepository, never()).save(any(Participant.class));
    }

    @Test
    void findOrCreate_rejectsBlankVkId() {
        assertThatThrownBy(() -> participantService.findOrCreate("  ", "Ivan", null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.VK_ID_REQUIRED));
    }

    @Test
    void findOrCreate_rejectsBlankName() {
        assertThatThrownBy(() -> participantService.findOrCreate("12345", "  ", null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.PARTICIPANT_NAME_REQUIRED));
    }

    @Test
    void update_trimsFieldsAndSavesChanges() {
        Participant existing = TestEntities.participant(1L, "12345", "Old name", "old");
        when(participantRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(participantRepository.findByVkId("67890")).thenReturn(Optional.empty());

        Participant updated = participantService.update(1L, "  67890  ", "  New name  ", "  new note  ");

        assertThat(updated.getVkId()).isEqualTo("67890");
        assertThat(updated.getName()).isEqualTo("New name");
        assertThat(updated.getComment()).isEqualTo("new note");
    }

    @Test
    void update_allowsSameVkId() {
        Participant existing = TestEntities.participant(1L, "12345", "Ivan", null);
        when(participantRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(participantRepository.findByVkId("12345")).thenReturn(Optional.of(existing));

        Participant updated = participantService.update(1L, "12345", "Petr", null);

        assertThat(updated.getVkId()).isEqualTo("12345");
        assertThat(updated.getName()).isEqualTo("Petr");
    }

    @Test
    void update_rejectsBlankVkId() {
        assertThatThrownBy(() -> participantService.update(1L, "  ", "Ivan", null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.VK_ID_REQUIRED));
    }

    @Test
    void update_rejectsDuplicateVkId() {
        Participant other = TestEntities.participant(2L, "67890", "Other", null);
        when(participantRepository.findByVkId("67890")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> participantService.update(1L, "67890", "Ivan", null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.VK_ID_ALREADY_EXISTS));
    }

    @Test
    void update_rejectsBlankName() {
        assertThatThrownBy(() -> participantService.update(1L, "12345", "  ", null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.PARTICIPANT_NAME_REQUIRED));
    }

    @Test
    void update_throwsWhenParticipantNotFound() {
        when(participantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> participantService.update(99L, "12345", "Name", null))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Participant not found: 99");
    }
}
