package com.example.telegram_bot.services;

import com.example.telegram_bot.dto.VacancyDTO;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class VacanciesReaderService {

    List<VacancyDTO> getVacanciesFromFile(String filename) {
        Resource resource = new ClassPathResource(filename);

        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            CsvToBean<VacancyDTO> csvToBean = new CsvToBeanBuilder<VacancyDTO>(reader)
                    .withType(VacancyDTO.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();
            return csvToBean.parse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
