package com.example.telegram_bot;

import com.example.telegram_bot.dto.ChatRequestDTO;
import com.example.telegram_bot.dto.ChatResponseDTO;
import com.example.telegram_bot.dto.VacancyDTO;
import com.example.telegram_bot.services.VacancyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class VacanciesBot extends TelegramLongPollingBot {
    @Qualifier("openaiRestTemplate")
    @Autowired
    private RestTemplate restTemplate;

    @Value("${openai.api.key}")
    private String apiKey;
    String lastShownVacancyId = "";
    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String apiUrl;
    @Autowired
    private VacancyService vacancyService;

    private Map<Long, String> lastShownVacancyLevel = new HashMap<>();


    public VacanciesBot() {
        super("6661786621:AAECIxe3ZQFfJv5DbG1cwsEA5twnexYlrNo");
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.getMessage() != null) {
                handleStartCommand(update);
            }
            if (update.getCallbackQuery() != null) {
                String callbackData = update.getCallbackQuery().getData();
                if ("show Junior vacancies".equals(callbackData)) {
                    showJuniorVacancies(update);
                } else if ("show Middle vacancies".equals(callbackData)) {
                    showMiddleVacancies(update);
                } else if ("show Senior vacancies".equals(callbackData)) {
                    showSeniorVacancies(update);
                } else if ("backToVacancies".equals(callbackData)) {
                    handleBackToVacanciesCommand(update);
                } else if ("backToStartMenu".equals(callbackData)) {
                    handleBackToStartCommand(update);
                } else if (callbackData.startsWith("vacancyId=")) {
                    String id = callbackData.split("=")[1];
                    lastShownVacancyId = id;
                    showVacancyDescription(id, update);
                } else if ("getCoverLetter".equals(callbackData)) {
                    handleGetCoverLetterCommand(lastShownVacancyId, update);
                }
            }
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleGetCoverLetterCommand(String id, Update update) throws TelegramApiException {
        VacancyDTO vacancy = vacancyService.getVacancyById(id);
        String prompt = "Can you generate semi-formal cover letter without cap for the vacancy, taking into account that the knowledge meets the requirements."
                + "Vacancy: " + vacancy.getTitle() + vacancy.getCompany() + vacancy.getShortDescription() + vacancy.getDescription();
        prompt = prompt.replace('\n', ' ');

        SendMessage sendMessage = new SendMessage();
        try {
            URL obj = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");
            // The request body
            String body = "{\"model\": \"" + model + "\", \"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}]}";
            connection.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(body);
            writer.flush();
            writer.close();

            // Response from ChatGPT
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;

            StringBuilder response = new StringBuilder();

            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            sendMessage.setText(extractMessageFromJSONResponse(response.toString()));
            sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId());
            sendMessage.setReplyMarkup(getBackToVacanciesMenuAfterCoverLetter());
            execute(sendMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        /*
        ChatRequestDTO chatRequest = new ChatRequestDTO(model,prompt);
        ChatResponseDTO chatResponse = restTemplate.postForObject(apiUrl,chatRequest, ChatResponseDTO.class);
        if (chatResponse == null || chatResponse.getChoices() == null || chatResponse.getChoices().isEmpty()) {
            sendMessage.setText("No response");
            sendMessage.setReplyMarkup(getStartMenu());
        }
        sendMessage.setText(chatResponse.getChoices().get(0).getMessage().getContent());
        sendMessage.setReplyMarkup(getStartMenu());*/

    }

    private void handleBackToStartCommand(Update update) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Please, choose your title:");
        sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId());
        sendMessage.setReplyMarkup(getStartMenu());
        execute(sendMessage);

    }

    private void handleBackToVacanciesCommand(Update update) throws TelegramApiException {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String level = lastShownVacancyLevel.get(chatId);

        if ("junior".equals(level)) {
            showJuniorVacancies(update);
        } else if ("middle".equals(level)) {
            showMiddleVacancies(update);
        } else if ("senior".equals(level)) {
            showSeniorVacancies(update);
        }
    }

    private void showSeniorVacancies(Update update) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Please choose vacancy:");
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        sendMessage.setChatId(chatId);
        sendMessage.setReplyMarkup(getSeniorVacanciesMenu());
        execute(sendMessage);
        lastShownVacancyLevel.put(chatId, "senior");
    }

    private ReplyKeyboard getSeniorVacanciesMenu() {
        List<VacancyDTO> seniorVacancies = vacancyService.getSeniorVacancies();
        return getVacanciesMenu(seniorVacancies);
    }

    private void showMiddleVacancies(Update update) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Please choose vacancy:");
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        sendMessage.setChatId(chatId);
        sendMessage.setReplyMarkup(getMiddleVacanciesMenu());
        execute(sendMessage);
        lastShownVacancyLevel.put(chatId, "middle");
    }

    private ReplyKeyboard getMiddleVacanciesMenu() {

        List<VacancyDTO> middleVacancies = vacancyService.getMiddleVacancies();
        return getVacanciesMenu(middleVacancies);
    }

    private void showJuniorVacancies(Update update) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Please choose vacancy:");
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        sendMessage.setChatId(chatId);
        sendMessage.setReplyMarkup(getJuniorVacanciesMenu());
        execute(sendMessage);

        lastShownVacancyLevel.put(chatId, "junior");

    }

    private void showVacancyDescription(String id, Update update) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId());
        VacancyDTO vacancy = vacancyService.getVacancyById(id);
        String vacancyInfo = """
                *Title:* %s
                *Company:* %s
                *Short description:* %s
                *Description:* %s
                *Salary:* %s
                *Link:* [%s](%s)
                """.formatted(
                escapeMarkdownReservedChars(vacancy.getTitle()),
                escapeMarkdownReservedChars(vacancy.getCompany()),
                escapeMarkdownReservedChars(vacancy.getShortDescription()),
                escapeMarkdownReservedChars(vacancy.getDescription()),
                vacancy.getSalary().isBlank() ? "Not specified" : escapeMarkdownReservedChars(vacancy.getSalary()),
                "Click here for more details",
                escapeMarkdownReservedChars(vacancy.getLink())

        );
        sendMessage.setText(vacancyInfo);
        sendMessage.setParseMode(ParseMode.MARKDOWNV2);
        sendMessage.setReplyMarkup(getBackToVacanciesMenu());
        execute(sendMessage);
    }

    private String escapeMarkdownReservedChars(String text) {
        return text.replace("-", "\\-")
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }

    private ReplyKeyboard getBackToVacanciesMenu() {
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton backToVacanciesButton = new InlineKeyboardButton();
        backToVacanciesButton.setText("Back to vacancies");
        backToVacanciesButton.setCallbackData("backToVacancies");
        row.add(backToVacanciesButton);

        InlineKeyboardButton backToStartMenu = new InlineKeyboardButton();
        backToStartMenu.setText("Back to start menu");
        backToStartMenu.setCallbackData("backToStartMenu");
        row.add(backToStartMenu);

        InlineKeyboardButton chatGptButton = new InlineKeyboardButton();
        chatGptButton.setText("Get cover letter");
        //chatGptButton.setUrl("https://chat.openai.com/");
        chatGptButton.setCallbackData("getCoverLetter");
        row.add(chatGptButton);

        return new InlineKeyboardMarkup(List.of(row));
    }

    private ReplyKeyboard getBackToVacanciesMenuAfterCoverLetter() {
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton backToVacanciesButton = new InlineKeyboardButton();
        backToVacanciesButton.setText("Back to vacancies");
        backToVacanciesButton.setCallbackData("backToVacancies");
        row.add(backToVacanciesButton);

        InlineKeyboardButton backToStartMenu = new InlineKeyboardButton();
        backToStartMenu.setText("Back to start menu");
        backToStartMenu.setCallbackData("backToStartMenu");
        row.add(backToStartMenu);
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(List.of(row));
        return keyboardMarkup;
    }

    private ReplyKeyboard getJuniorVacanciesMenu() {
        List<VacancyDTO> juniorVacancies = vacancyService.getJuniorVacancies();
        return getVacanciesMenu(juniorVacancies);
    }

    public ReplyKeyboard getVacanciesMenu(List<VacancyDTO> vacancies) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (VacancyDTO vacancy : vacancies) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(vacancy.getTitle());
            button.setCallbackData("vacancyId=" + vacancy.getId());
            row.add(button);
        }

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(List.of(row));
        return keyboardMarkup;
    }

    private void handleStartCommand(Update update) {
        String text = update.getMessage().getText();
        System.out.println("Received text:" + text);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId());
        sendMessage.setText("Welcome to Ishchenko java vacancies bot! Please choose your title:");
        sendMessage.setReplyMarkup(getStartMenu());
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private ReplyKeyboard getStartMenu() {
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton junior = new InlineKeyboardButton();
        junior.setText("Junior");
        junior.setCallbackData("show Junior vacancies");
        row.add(junior);

        InlineKeyboardButton middle = new InlineKeyboardButton();
        middle.setText("Middle");
        middle.setCallbackData("show Middle vacancies");
        row.add(middle);

        InlineKeyboardButton senior = new InlineKeyboardButton();
        senior.setText("Senior");
        senior.setCallbackData("show Senior vacancies");
        row.add(senior);

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(List.of(row));
        return keyboardMarkup;
    }

    public static String extractMessageFromJSONResponse(String response) {
        int start = response.indexOf("content") + 11;

        int end = response.indexOf("\"", start);
        response = response.replace("\\n", "\n");

        return response.substring(start, end);

    }

    @Override
    public String getBotUsername() {
        return "ishchenko vacancies bot";
    }
}
