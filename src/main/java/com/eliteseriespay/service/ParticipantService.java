package com.eliteseriespay.service;

import com.eliteseriespay.domain.MembershipStatus;
import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.exception.NotFoundException;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.repository.ParticipantRepository;
import com.eliteseriespay.util.Texts;
import com.eliteseriespay.validation.ParticipantValidator;
import com.eliteseriespay.validation.ValidationError;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParticipantService {

    private final ParticipantRepository participantRepository;

    public ParticipantService(ParticipantRepository participantRepository) {
        this.participantRepository = participantRepository;
    }

    @Transactional(readOnly = true)
    public Participant findById(Long id) {
        return participantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Participant", id));
    }

    @Transactional(readOnly = true)
    public List<Participant> findAllOrderByName() {
        return participantRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Participant> findAvailableForProject(Long projectId) {
        return participantRepository.findAvailableForProject(projectId, MembershipStatus.ACTIVE);
    }

    @Transactional
    public Participant findOrCreate(String vkId, String name, String comment) {
        String normalizedVkId = Texts.trim(vkId);
        String normalizedName = Texts.trim(name);
        String normalizedComment = Texts.trimToNull(comment);
        validateParticipant(normalizedVkId, normalizedName);

        return participantRepository.findByVkId(normalizedVkId)
                .orElseGet(() -> createParticipant(normalizedVkId, normalizedName, normalizedComment));
    }

    @Transactional
    public Participant create(String vkId, String name, String comment) {
        String normalizedVkId = Texts.trim(vkId);
        String normalizedName = Texts.trim(name);
        String normalizedComment = Texts.trimToNull(comment);
        validateParticipant(normalizedVkId, normalizedName);
        ensureVkIdAvailable(normalizedVkId, null);
        return createParticipant(normalizedVkId, normalizedName, normalizedComment);
    }

    @Transactional
    public Participant update(Long id, String vkId, String name, String comment) {
        String normalizedVkId = Texts.trim(vkId);
        String normalizedName = Texts.trim(name);
        String normalizedComment = Texts.trimToNull(comment);
        validateParticipant(normalizedVkId, normalizedName);
        ensureVkIdAvailable(normalizedVkId, id);

        Participant participant = findById(id);
        participant.updateDetails(normalizedVkId, normalizedName, normalizedComment);
        return participant;
    }

    private void validateParticipant(String vkId, String name) {
        ParticipantValidator.validateVkId(vkId);
        ParticipantValidator.validateName(name);
    }

    private void ensureVkIdAvailable(String vkId, Long participantId) {
        participantRepository.findByVkId(vkId)
                .filter(existing -> participantId == null || !existing.getId().equals(participantId))
                .ifPresent(existing -> {
                    throw new ValidationException(ValidationError.VK_ID_ALREADY_EXISTS);
                });
    }

    private Participant createParticipant(String vkId, String name, String comment) {
        Participant participant = new Participant(vkId, name, comment);
        return participantRepository.save(participant);
    }
}
