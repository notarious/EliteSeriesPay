package com.eliteseriespay.service;

import com.eliteseriespay.repository.ApplicationSettingsRepository;
import com.eliteseriespay.repository.ParticipantRepository;
import com.eliteseriespay.repository.PaymentRepository;
import com.eliteseriespay.repository.ProjectMembershipRepository;
import com.eliteseriespay.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationResetService {

    private final PaymentRepository paymentRepository;
    private final ProjectMembershipRepository projectMembershipRepository;
    private final ProjectRepository projectRepository;
    private final ParticipantRepository participantRepository;
    private final ApplicationSettingsRepository applicationSettingsRepository;

    public ApplicationResetService(PaymentRepository paymentRepository,
                                     ProjectMembershipRepository projectMembershipRepository,
                                     ProjectRepository projectRepository,
                                     ParticipantRepository participantRepository,
                                     ApplicationSettingsRepository applicationSettingsRepository) {
        this.paymentRepository = paymentRepository;
        this.projectMembershipRepository = projectMembershipRepository;
        this.projectRepository = projectRepository;
        this.participantRepository = participantRepository;
        this.applicationSettingsRepository = applicationSettingsRepository;
    }

    @Transactional
    public void resetAllData() {
        paymentRepository.deleteAllInBatch();
        projectMembershipRepository.deleteAllInBatch();
        projectRepository.deleteAllInBatch();
        participantRepository.deleteAllInBatch();
        applicationSettingsRepository.deleteAllInBatch();
    }
}
