package pl.rsi.cinema;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public class SeatController implements Initializable {

    private static final String COLOR_FREE = "#808080";   // Dostępne
    private static final String COLOR_OCCUPIED = "#393939"; // Zajęte / niedostępne
    private static final String COLOR_RESERVED = "#FF0000"; // Wybrane przez użytkownika

    @FXML
    private GridPane seatGrid;
    @FXML
    private VBox seatsListContainer;
    @FXML
    private VBox seatsListContainer1;
    @FXML
    private Button confirmReservationButton;

    private final Map<String, Button> seatButtons = new HashMap<>();
    private final Set<String> selectedSeatKeys = new HashSet<>();
    private final Map<String, ReservationInfo> confirmedReservations = new HashMap<>();

    private static class ReservationInfo {
        String title;
        String time;
        Set<String> seats;

        ReservationInfo(String title, String time) {
            this.title = title;
            this.time = time;
            this.seats = new HashSet<>();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        collectSeatButtons();
        initializeReservedSeats();
    }

    private void initializeReservedSeats() {
        for (int r = 3; r <= 4; r++) {
            for (int s = 5; s <= 7; s++) {
                updateButtonColor(r, s, COLOR_OCCUPIED);
            }
        }
    }

    private void collectSeatButtons() {
        seatButtons.clear();
        if (seatGrid == null) {
            return;
        }

        for (Node node : seatGrid.getChildren()) {
            if (node instanceof Button button) {
                Integer rowIndex = GridPane.getRowIndex(button);
                Integer colIndex = GridPane.getColumnIndex(button);
                int row = rowIndex == null ? 0 : rowIndex;
                int col = colIndex == null ? 0 : colIndex;
                seatButtons.put(row + "," + col, button);
            }
        }
    }

    @FXML
    private void handleSeatClick(ActionEvent event) {
        if (!(event.getSource() instanceof Button clickedButton)) {
            return;
        }

        Integer rowIndex = GridPane.getRowIndex(clickedButton);
        Integer colIndex = GridPane.getColumnIndex(clickedButton);
        int row = (rowIndex == null ? 0 : rowIndex) + 1;
        int col = (colIndex == null ? 0 : colIndex) + 1;
        String seatKey = formatSeatKey(row, col);

        if (clickedButton.getStyle().contains(COLOR_OCCUPIED)) {
            return;
        }

        if (clickedButton.getStyle().contains(COLOR_RESERVED)) {
            deselectSeat(seatKey, row, col);
        } else {
            selectSeat(seatKey, row, col);
        }
    }

    @FXML
    private void handleConfirmReservation(ActionEvent event) {
        if (selectedSeatKeys.isEmpty()) {
            return;
        }

        // Tworzy rezerwację dla wybranego filmu (tutaj hardcoded "Diuna" i "14:30")
        String title = "Diuna";
        String time = "14:30";
        String reservationId = title + "_" + time;

        ReservationInfo reservation = new ReservationInfo(title, time);
        reservation.seats.addAll(selectedSeatKeys);

        // Wyświetl rezerwację w lewym panelu
        displayConfirmedReservation(reservationId, title, time);

        // Wyczyść prawe panele
        seatsListContainer.getChildren().clear();
        selectedSeatKeys.clear();

        confirmedReservations.put(reservationId, reservation);
    }

    private void displayConfirmedReservation(String reservationId, String title, String time) {
        VBox reservationBox = new VBox(8);
        reservationBox.setStyle("-fx-border-color: #313131; -fx-border-radius: 5; -fx-border-width: 1; -fx-padding: 10; -fx-background-color: #1a1a1a;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold;");

        Label timeLabel = new Label("Godzina: " + time);
        timeLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12;");

        HBox buttonsBox = new HBox(8);
        buttonsBox.setAlignment(Pos.CENTER_RIGHT);

        Button editButton = new Button("Edytuj");
        editButton.setStyle("-fx-background-color: #4444ff; -fx-text-fill: white; -fx-background-radius: 4;");
        editButton.setOnAction(e -> editReservation(reservationId));

        Button deleteButton = new Button("Usuń");
        deleteButton.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white; -fx-background-radius: 4;");
        deleteButton.setOnAction(e -> deleteReservation(reservationId));

        buttonsBox.getChildren().addAll(editButton, deleteButton);

        reservationBox.getChildren().addAll(titleLabel, timeLabel, buttonsBox);
        seatsListContainer1.getChildren().add(reservationBox);
    }

    private void editReservation(String reservationId) {
        // Implementacja edycji rezerwacji - przywróć miejsca do wybioru
        ReservationInfo reservation = confirmedReservations.get(reservationId);
        if (reservation != null) {
            selectedSeatKeys.addAll(reservation.seats);
            for (String seatKey : reservation.seats) {
                String[] parts = seatKey.split(",");
                int row = Integer.parseInt(parts[0]);
                int col = Integer.parseInt(parts[1]);
                addSeatToList(seatKey, row, col);
            }
            // Usuń rezerwację z lewego panelu
            deleteReservation(reservationId);
        }
    }

    private void deleteReservation(String reservationId) {
        ReservationInfo reservation = confirmedReservations.remove(reservationId);
        if (reservation != null) {
            // Zwolnij miejsca w sieci
            for (String seatKey : reservation.seats) {
                String[] parts = seatKey.split(",");
                int row = Integer.parseInt(parts[0]);
                int col = Integer.parseInt(parts[1]);
                updateButtonColor(row, col, COLOR_FREE);
                selectedSeatKeys.remove(seatKey);
            }
        }

        // Usuń z widoku lewego panelu
        seatsListContainer1.getChildren().removeIf(node -> {
            if (node instanceof VBox vbox && vbox.getChildren().size() > 0) {
                Node firstChild = vbox.getChildren().get(0);
                return firstChild instanceof Label && ((Label) firstChild).getText().equals(reservationId);
            }
            return false;
        });
    }

    private void selectSeat(String seatKey, int row, int col) {
        updateButtonColor(row, col, COLOR_RESERVED);
        addSeatToList(seatKey, row, col);
    }

    private void deselectSeat(String seatKey, int row, int col) {
        updateButtonColor(row, col, COLOR_FREE);
        removeSeatFromList(seatKey);
    }

    private void addSeatToList(String seatKey, int row, int col) {
        if (selectedSeatKeys.contains(seatKey)) {
            return;
        }

        String rowLetter = getRowLetter(row);
        Label seatLabel = new Label("Rząd " + rowLetter + ", Miejsce " + col);
        seatLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14;");

        Button removeButton = new Button("Usuń");
        removeButton.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white; -fx-background-radius: 4;");
        removeButton.setOnAction(event -> deselectSeat(seatKey, row, col));

        HBox rowBox = new HBox(10, seatLabel, removeButton);
        rowBox.setAlignment(Pos.CENTER_LEFT);
        rowBox.setStyle("-fx-padding: 6; -fx-background-color: transparent;");

        seatsListContainer.getChildren().add(rowBox);
        selectedSeatKeys.add(seatKey);
    }

    private void removeSeatFromList(String seatKey) {
        seatsListContainer.getChildren().removeIf(node -> {
            if (node instanceof HBox rowBox && rowBox.getChildren().get(0) instanceof Label label) {
                return label.getText().equals(seatKeyToLabel(seatKey));
            }
            return false;
        });
        selectedSeatKeys.remove(seatKey);
    }

    private String seatKeyToLabel(String seatKey) {
        String[] parts = seatKey.split(",");
        int row = Integer.parseInt(parts[0]);
        int col = Integer.parseInt(parts[1]);
        return "Rząd " + getRowLetter(row) + ", Miejsce " + col;
    }

    private String formatSeatKey(int row, int col) {
        return row + "," + col;
    }

    private String getRowLetter(int row) {
        return String.valueOf((char) ('A' + row - 1));
    }

    private int getRowFromLetter(String letter) {
        if (letter == null || letter.isEmpty()) {
            return 1;
        }
        return letter.charAt(0) - 'A' + 1;
    }

    private boolean isSeatAvailable(int row, int col) {
        Button btn = seatButtons.get((row - 1) + "," + (col - 1));
        if (btn == null) {
            return false;
        }
        String style = btn.getStyle();
        if (style == null || style.isEmpty()) {
            return true;
        }
        return !style.contains(COLOR_OCCUPIED) && !style.contains(COLOR_RESERVED);
    }

    private void updateButtonColor(int row, int col, String colorHex) {
        Button btn = seatButtons.get((row - 1) + "," + (col - 1));
        if (btn == null) {
            return;
        }

        String style = btn.getStyle();
        if (style == null) {
            style = "";
        }

        if (style.contains(COLOR_FREE)) {
            btn.setStyle(style.replace(COLOR_FREE, colorHex));
        } else if (style.contains(COLOR_RESERVED)) {
            btn.setStyle(style.replace(COLOR_RESERVED, colorHex));
        } else if (style.contains(COLOR_OCCUPIED)) {
            btn.setStyle(style.replace(COLOR_OCCUPIED, colorHex));
        } else {
            btn.setStyle(style + " -fx-background-color: " + colorHex + ";");
        }
    }
}
