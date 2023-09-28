package com.example.telegram_bot.services;

import com.example.telegram_bot.dto.VacancyDTO;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VacancyService {

    @Autowired
    private VacanciesReaderService vacanciesReaderService;
    private final Map<String, VacancyDTO> vacancies = new HashMap<>();

    @PostConstruct
    public void init() {
        List<VacancyDTO> vacanciesFromFile = vacanciesReaderService.getVacanciesFromFile("vacancies.csv");
        for (VacancyDTO vacancy : vacanciesFromFile) {
            vacancies.put(vacancy.getId(), vacancy);
        }
    }

    public List<VacancyDTO> getJuniorVacancies() {
        return vacancies
                .values()
                .stream()
                .filter(v -> v.getTitle().toLowerCase().contains("junior"))
                .toList();
    }
    public List<VacancyDTO> getMiddleVacancies() {
        return vacancies
                .values()
                .stream()
                .filter(v -> v.getTitle().toLowerCase().contains("middle"))
                .toList();
    }
    public List<VacancyDTO> getSeniorVacancies() {
        return vacancies
                .values()
                .stream()
                .filter(v -> v.getTitle().toLowerCase().contains("senior"))
                .toList();
    }
    public VacancyDTO getVacancyById(String id) {
        //return vacancies.values().stream().filter(v->v.getId().equals(id)).findFirst().get();
        return vacancies.get(id);
    }
}
