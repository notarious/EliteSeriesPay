package com.eliteseriespay.repository;

import com.eliteseriespay.domain.Participant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    Optional<Participant> findByVkId(String vkId);
}
