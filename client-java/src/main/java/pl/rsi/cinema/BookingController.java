package pl.rsi.cinema;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BookingController {

    @FXML
    private VBox seatsListContainer;
    @FXML
    private VBox seatsListContainer1;
    @FXML
    private HBox timeButtonsContainer;
    @FXML
    private GridPane seatGrid;
    @FXML
    private ComboBox<String> CBDate;

    private Button currentSelectedTimeButton = null;
    private String selectedTime = "";
    private final Set<String> selectedSeatKeys = new HashSet<>();
    private final SeatController seatController = new SeatController();

    // KLUCZ: "Data_Godzina" (np. "27.04.2024_14:30"), WARTOŚĆ: Zbiór zajętych
    // miejsc
    private final Map<String, Set<String>> occupancyMap = new HashMap<>();

    @FXML
    public void initialize() {
        seatController.setBookingController(this);
        seatController.initSeatMap(seatGrid);

        CBDate.getItems().addAll("27.04.2024", "28.04.2024", "29.04.2024");
        CBDate.getSelectionModel().selectFirst();

        // Każda zmiana daty odświeża zajętość sali
        CBDate.valueProperty().addListener((obs, oldVal, newVal) -> refreshOccupancy());
    }

    @FXML
    private void handleTimeSelection(ActionEvent event) {
        if (currentSelectedTimeButton != null) {
            resetButtonToDefault(currentSelectedTimeButton);
        }

        Button clickedBtn = (Button) event.getSource();
        clickedBtn.setStyle(
                "-fx-background-color: #0078D7; -fx-border-color: #005a9e; -fx-border-radius: 5; -fx-text-fill: white; -fx-font-weight: bold;");

        this.currentSelectedTimeButton = clickedBtn;
        this.selectedTime = clickedBtn.getText();

        // KLUCZOWE: Po kliknięciu godziny sprawdzamy, jakie miejsca są zajęte w tym
        // konkretnym czasie
        refreshOccupancy();
    }

    private void refreshOccupancy() {
        String currentDate = CBDate.getValue();

        // 1. Czyścimy siatkę (wszystkie na szaro)
        seatController.resetAllSeatsToFree();

        // 2. Czyścimy aktualny wybór (prawą listę)
        seatsListContainer.getChildren().clear();
        selectedSeatKeys.clear();

        // 3. Jeśli wybrano i datę, i godzinę, ładujemy zajęte miejsca
        if (currentDate != null && !selectedTime.isEmpty()) {
            String key = currentDate + "_" + selectedTime;
            Set<String> occupied = occupancyMap.getOrDefault(key, new HashSet<>());
            seatController.markOccupiedOnGrid(occupied);
        }
    }

    @FXML
    private void handleConfirmReservation(ActionEvent event) {
        String currentDate = CBDate.getValue();
        if (selectedSeatKeys.isEmpty() || selectedTime.isEmpty() || currentDate == null)
            return;

        // 1. Zapisujemy do mapy pod kluczem łączonym "Data_Godzina"
        String sessionKey = currentDate + "_" + selectedTime;
        occupancyMap.computeIfAbsent(sessionKey, k -> new HashSet<>()).addAll(selectedSeatKeys);

        // 2. Dodajemy wpis do lewego panelu (Potwierdzone)
        VBox reservationBox = new VBox(5);
        reservationBox.setStyle(
                "-fx-border-color: #313131; -fx-border-radius: 5; -fx-padding: 10; -fx-background-color: #1a1a1a;");

        Label infoLabel = new Label("Diuna | " + currentDate + " | Godz: " + selectedTime);
        infoLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        StringBuilder seatsSummary = new StringBuilder();
        for (String key : selectedSeatKeys) {
            String[] parts = key.split(",");
            char rowLetter = (char) ('A' + Integer.parseInt(parts[0]) - 1);
            if (seatsSummary.length() > 0)
                seatsSummary.append(", ");
            seatsSummary.append(rowLetter).append(parts[1]);
        }

        Label seatsLabel = new Label("Miejsca: " + seatsSummary.toString());
        seatsLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11;");

        reservationBox.getChildren().addAll(infoLabel, seatsLabel);
        seatsListContainer1.getChildren().add(reservationBox);

        // 3. Resetujemy wybór (ale zostawiamy datę zaznaczoną)
        seatController.markSelectedAsOccupied(selectedSeatKeys);
        seatsListContainer.getChildren().clear();
        selectedSeatKeys.clear();
        resetButtonToDefault(currentSelectedTimeButton);
        selectedTime = "";
        refreshOccupancy();
    }

    // Pozostałe metody pomocnicze (addSeatToList, removeSeatFromList,
    // handleSeatClick, resetButtonToDefault) pozostają bez zmian...

    @FXML
    private void handleSeatClick(ActionEvent event) {
        seatController.processSeatClick(event);
    }

    public void addSeatToList(String seatKey, int row, int col) {
        if (selectedSeatKeys.contains(seatKey))
            return;
        String rowLetter = String.valueOf((char) ('A' + row - 1));
        Label seatLabel = new Label("Rząd: " + rowLetter + " miejsce " + col);
        seatLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13;");
        Button removeButton = new Button("Usuń");
        removeButton.setStyle(
                "-fx-background-color: #ff4444; -fx-text-fill: white; -fx-font-size: 10; -fx-background-radius: 4;");
        removeButton.setOnAction(e -> {
            seatController.resetSingleSeatColor(seatKey);
            removeSeatFromList(seatKey);
        });
        HBox rowBox = new HBox(8, seatLabel, removeButton);
        rowBox.setAlignment(Pos.CENTER_LEFT);
        rowBox.setUserData(seatKey);
        seatsListContainer.getChildren().add(rowBox);
        selectedSeatKeys.add(seatKey);
    }

    public void removeSeatFromList(String seatKey) {
        selectedSeatKeys.remove(seatKey);
        seatsListContainer.getChildren().removeIf(node -> seatKey.equals(node.getUserData()));
    }

    private void resetButtonToDefault(Button btn) {
        if (btn != null)
            btn.setStyle(
                    "-fx-background-color: #121212; -fx-border-color: #313131; -fx-border-radius: 5; -fx-text-fill: white; -fx-font-weight: normal;");
    }
}