package com.eliteseriespay.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.eliteseriespay.domain.Participant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:./target/participant-search-test.db?busy_timeout=5000"
})
class ParticipantRepositorySearchTest {

    @Autowired
    private ParticipantRepository participantRepository;

    @BeforeEach
    void setUp() {
        participantRepository.deleteAll();
        participantRepository.save(new Participant("12345", "Anna", null));
        participantRepository.save(new Participant("67890", "Boris", "note"));
        participantRepository.save(new Participant("11111", "иван", null));
    }

    @Test
    void findAllByOrderByNameAsc_paginatesAndSortsByName() {
        PageRequest firstPage = PageRequest.of(0, 2, Sort.by("name").ascending());

        Page<Participant> page = participantRepository.findAllByOrderByNameAsc(firstPage);

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Participant::getName).containsExactly("Anna", "Boris");

        Page<Participant> secondPage = participantRepository.findAllByOrderByNameAsc(
                PageRequest.of(1, 2, Sort.by("name").ascending()));

        assertThat(secondPage.getContent()).extracting(Participant::getName).containsExactly("иван");
    }

    @Test
    void searchByNameOrVkIdIgnoreCase_matchesLatinNameCaseInsensitively() {
        Page<Participant> page = participantRepository.searchByNameOrVkIdIgnoreCase(
                "ANNA",
                PageRequest.of(0, 50, Sort.by("name").ascending()));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(Participant::getName).containsExactly("Anna");
    }

    @Test
    void searchByNameOrVkIdIgnoreCase_matchesCyrillicNameBySubstring() {
        Page<Participant> page = participantRepository.searchByNameOrVkIdIgnoreCase(
                "иван",
                PageRequest.of(0, 50, Sort.by("name").ascending()));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(Participant::getName).containsExactly("иван");
    }

    @Test
    void searchByNameOrVkIdIgnoreCase_matchesPartialVkId() {
        Page<Participant> page = participantRepository.searchByNameOrVkIdIgnoreCase(
                "789",
                PageRequest.of(0, 50, Sort.by("name").ascending()));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(Participant::getVkId).containsExactly("67890");
    }

    @Test
    void searchByNameOrVkIdIgnoreCase_matchesPartialName() {
        Page<Participant> page = participantRepository.searchByNameOrVkIdIgnoreCase(
                "ann",
                PageRequest.of(0, 50, Sort.by("name").ascending()));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(Participant::getName).containsExactly("Anna");
    }

    @Test
    void searchByNameOrVkIdIgnoreCase_returnsEmptyPageWhenNothingMatches() {
        Page<Participant> page = participantRepository.searchByNameOrVkIdIgnoreCase(
                "missing",
                PageRequest.of(0, 50, Sort.by("name").ascending()));

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void searchByNameOrVkIdIgnoreCase_sortsResultsByName() {
        participantRepository.save(new Participant("22222", "Aaron", null));

        Page<Participant> page = participantRepository.searchByNameOrVkIdIgnoreCase(
                "a",
                PageRequest.of(0, 50, Sort.by("name").ascending()));

        assertThat(page.getContent()).extracting(Participant::getName)
                .containsExactly("Aaron", "Anna");
    }
}
