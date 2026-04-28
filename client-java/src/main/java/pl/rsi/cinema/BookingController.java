package pl.rsi.cinema;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import pl.rsi.cinema.CinemaServerService.MovieDetails;
import pl.rsi.cinema.dto.MovieFromServer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;

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
    @FXML
    private TableColumn<MovieFromServer, String> titleCol;
    @FXML
    private TableColumn<MovieFromServer, String> genreCol;
    @FXML
    private TableColumn<MovieFromServer, String> dateCol;
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

    @FXML
    private Label titleLabel;

    @FXML
    private Label durationLabel;

    @FXML
    private Label genreLabel;

    @FXML
    private Label yearLabel;

    @FXML
    private Label directorLabel;

    @FXML
    private TextArea actorsArea;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private ImageView coverImage;
    private Button currentSelectedTimeButton = null;
    private String selectedTime = "";
    private final Set<String> selectedSeatKeys = new HashSet<>();
    private final SeatController seatController = new SeatController();
    private final CinemaServerService serverService = CinemaServerService.getInstance();
    private List<MovieFromServer> movies;
    @FXML
    private TableView<MovieFromServer> moviesTable;
    private final Map<String, Set<String>> occupancyMap = new HashMap<>();

    @FXML
    public void initialize() {
        // Sprawdźmy, która instancja się odpala
        System.out.println("DEBUG: Init - loginForm: " + (loginForm != null));

        System.out.println("INIT CALLED");
        System.out.println("SHOWING OVERLAY TEST");
        showAuthOverlay();
        System.out.println("authOverlay = " + authOverlay);
        System.out.println("loginForm = " + loginForm);
        System.out.println("titleLabel = " + titleLabel);
        System.out.println("seatGrid = " + seatGrid);
        loginForm.setVisible(true);
        loginForm.setManaged(true);

        registerForm.setVisible(false);
        registerForm.setManaged(false);
        if (seatGrid != null) {
            seatController.setBookingController(this);
            seatController.initSeatMap(seatGrid);

            try {
                movies = serverService.getMovies();

                if (movies != null && !movies.isEmpty()) {

                    Set<String> uniqueDates = new HashSet<>();
                    for (var movie : movies) {
                        String dateTime = movie.getShowDateTime();
                        if (dateTime != null && dateTime.length() >= 10) {
                            String date = dateTime.substring(0, 10).replace("-", ".");
                            uniqueDates.add(date);
                        }
                    }

                    MovieDate.getItems().addAll(uniqueDates);

                } else {
                    MovieDate.getItems().addAll("27.04.2024", "28.04.2024", "29.04.2024");
                }

            } catch (Exception e) {
                System.out.println("Błąd: " + e.getMessage());
                MovieDate.getItems().addAll("27.04.2024", "28.04.2024", "29.04.2024");
            }

            if (!MovieDate.getItems().isEmpty()) {
                MovieDate.getSelectionModel().selectFirst();
            }

            // 🔥 DOPIERO TERAZ:
            setupMovies();
            loadMovies();

            moviesTable.getSelectionModel().selectedItemProperty().addListener(
                    (obs, oldMovie, newMovie) -> {
                        if (newMovie != null) {
                            showMovieDetails(newMovie);
                        }
                    });
        } else {
            System.out.println("ERROR: seatGrid is null!");
        }
        System.out.println("HIDE INSTANCE = " + System.identityHashCode(this));
    }

    private void showMovieDetails(MovieFromServer movie) {

        if (titleLabel == null) {
            System.out.println("UI not ready yet");
            return;
        }
        System.out.println("REQUEST MOVIE DETAILS ID = " + movie.getShowId());
        System.out.println("CLICKED SHOW ID = " + movie.getShowId());

        MovieDetails details = serverService.getMovieDetails(movie.getShowId());

        if (details == null)
            return;

        titleLabel.setText(movie.getTitle());
        durationLabel.setText(details.getDuration() + " min");
        genreLabel.setText(movie.getGenre());
        yearLabel.setText(details.getPremiere().substring(0, 4));
        directorLabel.setText(details.getDirector());

        actorsArea.setText(details.getActors());
        descriptionArea.setText(details.getDescription());

        if (details.getPoster() != null && !details.getPoster().isEmpty()) {
            byte[] imageBytes = java.util.Base64.getDecoder().decode(details.getPoster());
            Image image = new Image(new ByteArrayInputStream(imageBytes));
            coverImage.setImage(image);
        }
    }

    private void setupMovies() {

        titleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));

        genreCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getGenre()));

        dateCol.setCellValueFactory(data -> new SimpleStringProperty(formatDate(data.getValue().getShowDateTime())));
    }

    private List<MovieFromServer> filterAndSort(List<MovieFromServer> list) {

        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> seen = new HashSet<>();
        List<MovieFromServer> result = new ArrayList<>();

        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(7);

        for (MovieFromServer m : list) {

            LocalDate date = extractDate(m);

            if (date.isBefore(today) || date.isAfter(end)) {
                continue;
            }

            String key = m.getTitle() + "_" + date;

            if (seen.add(key)) {
                result.add(m);
            }
        }

        result.sort(Comparator.comparing(this::extractDate));
        return result;
    }

    private LocalDate extractDate(MovieFromServer m) {
        return LocalDate.parse(m.getShowDateTime().split("T")[0]);
    }

    private String formatDate(String dateTime) {
        if (dateTime == null)
            return "";

        String date = dateTime.split("T")[0]; // 2025-04-28

        String[] parts = date.split("-");
        return parts[2] + "." + parts[1]; // 28.04
    }

    public void loadMovieDetails(int movieId) {
        try {
            CinemaServerService.MovieDetails movie = serverService.getMovieDetails(movieId);

            if (movie == null)
                return;

            titleLabel.setText(movie.getTitle());
            durationLabel.setText(movie.getDuration() + " min");
            directorLabel.setText("Reżyser: " + movie.getDirector());

            // PREMIERE (String → rok)
            if (movie.getPremiere() != null && movie.getPremiere().length() >= 4) {
                yearLabel.setText("Rok: " + movie.getPremiere().substring(0, 4));
            }

            actorsArea.setText(movie.getActors());
            descriptionArea.setText(movie.getDescription());

            // POSTER (Base64 String → Image)
            if (movie.getPoster() != null && !movie.getPoster().isEmpty()) {

                byte[] imageBytes = java.util.Base64.getDecoder().decode(movie.getPoster());
                Image image = new Image(new ByteArrayInputStream(imageBytes));

                coverImage.setImage(image); // ✔ poprawna nazwa
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadMovies() {

        List<MovieFromServer> filtered = filterAndSort(movies);

        moviesTable.setItems(
                FXCollections.observableArrayList(filtered));
    }

    private void showAuthOverlay() {
        authOverlay.setDisable(false);
        authOverlay.setVisible(true);
        authOverlay.setManaged(true);
        authOverlay.setMouseTransparent(false);
    }

    @FXML
    public void showRegisterForm() {
        showAuthOverlay();
        // Logika właściwa (wykona się tylko na mainInstance)
        if (loginForm != null) {
            loginForm.setVisible(false);
            loginForm.setManaged(false);
            registerForm.setVisible(true);
            registerForm.setManaged(true);
        }
    }

    @FXML
    public void showLoginForm() {
        showAuthOverlay();
        System.out.println("Przełączam na logowanie...");
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
        authOverlay.setDisable(true);
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