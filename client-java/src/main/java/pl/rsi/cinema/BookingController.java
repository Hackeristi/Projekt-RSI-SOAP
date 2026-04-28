package pl.rsi.cinema;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BookingController {

    // Auth overlay fields
    @FXML
    private VBox authOverlay;
    @FXML
    private VBox authModal;
    @FXML
    private VBox loginForm;
    @FXML
    private VBox registerForm;
    @FXML
    private TextField loginEmail;
    @FXML
    private PasswordField loginPassword;
    @FXML
    private TextField registerName;
    @FXML
    private TextField registerEmail;
    @FXML
    private PasswordField registerPassword;
    @FXML
    private PasswordField registerConfirmPassword;

    // Booking fields
    @FXML
    private VBox seatsListContainer;
    @FXML
    private VBox seatsListContainer1;
    @FXML
    private HBox timeButtonsContainer;
    @FXML
    private GridPane seatGrid;
    @FXML
    private ComboBox<String> MovieDate;
    private static BookingController mainInstance;
    private Button currentSelectedTimeButton = null;
    private String selectedTime = "";
    private final Set<String> selectedSeatKeys = new HashSet<>();
    private final SeatController seatController = new SeatController();
    // KLUCZ: "Data_Godzina" (np. "27.04.2024_14:30"), WARTOŚĆ: Zbiór zajętych
    // miejsc
    private final Map<String, Set<String>> occupancyMap = new HashMap<>();

    @FXML
    public void initialize() {
        // Sprawdźmy, która instancja się odpala
        System.out.println("DEBUG: Init - loginForm: " + (loginForm != null));

        // Zapisujemy JAKĄKOLWIEK instancję, która ma loginForm
        if (authOverlay != null) {
            mainInstance = this;
            System.out.println("DEBUG: GŁÓWNA INSTANCJA ZAREJESTROWANA");
        } else {
            System.out.println("DEBUG: To nie jest główna instancja, loginForm jest nullem.");
        }
        System.out.println("INIT CALLED");
        System.out.println("SHOWING OVERLAY TEST");
        showAuthOverlay();
        System.out.println("authOverlay = " + authOverlay);
        System.out.println("loginForm = " + loginForm);

        // Initialize booking
        if (seatGrid != null) {
            seatController.setBookingController(this);
            seatController.initSeatMap(seatGrid);

            MovieDate.getItems().addAll("27.04.2024", "28.04.2024", "29.04.2024");
            MovieDate.getSelectionModel().selectFirst();

            // Każda zmiana daty odświeża zajętość sali
            MovieDate.valueProperty().addListener((obs, oldVal, newVal) -> refreshOccupancy());
        }
    }

private void showAuthOverlay() {
    authOverlay.setVisible(true);
    authOverlay.setManaged(true);
    authOverlay.setMouseTransparent(false);
    authOverlay.setOpacity(1);
    authOverlay.toFront();

    @FXML
    public void showRegisterForm() {
        showAuthOverlay();
        // Jeśli ta metoda odpaliła się w instancji LoginWindow (gdzie loginForm to
        // null)
        // to przekazujemy pałeczkę do głównej instancji
        if (this != mainInstance && mainInstance != null) {
            mainInstance.showRegisterForm();
            return;
        }

        // Logika właściwa (wykona się tylko na mainInstance)
        if (loginForm != null) {
            loginForm.setVisible(false);
            loginForm.setManaged(false);
            registerForm.setVisible(true);
            registerForm.setManaged(true);
            authOverlay.setOpacity(1);

            authOverlay.toFront();
        }
    }

    @FXML
    public void showLoginForm() {
        showAuthOverlay();
        System.out.println("Przełączam na logowanie...");
        if (this != mainInstance && mainInstance != null) {
            mainInstance.showLoginForm();
            return;
        }

        if (loginForm != null && registerForm != null) {
            loginForm.setVisible(true);
            loginForm.setManaged(true);
            registerForm.setVisible(false);
            registerForm.setManaged(false);
        }
    }

    @FXML
    public void handleLogin() {
        String email = loginEmail.getText();
        String password = loginPassword.getText();

        // Wywołujemy statyczną metodę z klasy pomocniczej
        String error = LoginController.validateLogin(email, password);
        if (error != null) {
            showAlert("Błąd", error);
            return;
        }

        if (LoginController.performLogin(email, password)) {
            hideAuthOverlay(); // TO sprawi, że nakładka zniknie i zobaczysz kino!
        } else {
            System.out.println("EMAIL FIELD: " + loginEmail);
            System.out.println("EMAIL TEXT: " + (loginEmail != null ? loginEmail.getText() : "NULL"));
            showAlert("Błąd", "Nieprawidłowe dane logowania");
        }
        System.out.println("VISIBLE: " + authOverlay.isVisible());
        System.out.println("MANAGED: " + authOverlay.isManaged());
        System.out.println("MOUSE: " + authOverlay.isMouseTransparent());
        System.out.println("authOverlay parent = " + authOverlay.getParent());

        System.out.println("Kliknięto logowanie!");
    }

    @FXML
    public void handleRegister() {
        String name = registerName.getText();
        String email = registerEmail.getText();
        String pass = registerPassword.getText();
        String confirm = registerConfirmPassword.getText();

        String error = RegisterController.validateRegister(name, email, pass, confirm);
        if (error != null) {
            showAlert("Błąd", error);
            return;
        }

        if (RegisterController.performRegister(email, pass, name)) {
            showAlert("Sukces", "Zarejestrowano! Możesz się zalogować.");
            showLoginForm();
        }
    }

    private void hideAuthOverlay() {
        System.out.println("HIDE INSTANCE = " + System.identityHashCode(this));
        System.out.println("OVERLAY = " + authOverlay);
        authOverlay.setVisible(false);
        authOverlay.setManaged(false);
        authOverlay.setMouseTransparent(true);
        authOverlay.setOpacity(1);
    }

    private void showAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
        String currentDate = MovieDate.getValue();

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
        String currentDate = MovieDate.getValue();
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