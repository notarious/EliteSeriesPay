package com.eliteseriespay.service;

import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.exception.NotFoundException;
import com.eliteseriespay.repository.ParticipantRepository;
import com.eliteseriespay.util.Texts;
import com.eliteseriespay.validation.ParticipantValidator;
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
        return createParticipant(normalizedVkId, normalizedName, normalizedComment);
    }

    @Transactional
    public Participant update(Long id, String name, String comment) {
        String normalizedName = Texts.trim(name);
        String normalizedComment = Texts.trimToNull(comment);
        ParticipantValidator.validateName(normalizedName);

        Participant participant = findById(id);
        participant.updateDetails(normalizedName, normalizedComment);
        return participant;
    }

    private void validateParticipant(String vkId, String name) {
        ParticipantValidator.validateVkId(vkId);
        ParticipantValidator.validateName(name);
    }

    private Participant createParticipant(String vkId, String name, String comment) {
        Participant participant = new Participant(vkId, name, comment);
        return participantRepository.save(participant);
    }
}
