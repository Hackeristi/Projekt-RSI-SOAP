package pl.rsi.cinema;

import javafx.scene.control.Alert;

public class RegisterController {

    private static final UserService userService = UserService.getInstance();

    // Metoda statyczna do walidacji - wywoływana przez BookingController
    public static String validateRegister(String fullName, String email, String password, String confirmPassword) {
        if (fullName == null || fullName.trim().isEmpty())
            return "Proszę wprowadzić imię i nazwisko";
        if (email == null || !email.contains("@"))
            return "Proszę wprowadzić prawidłowy email";
        if (password == null || password.length() < 4)
            return "Hasło musi mieć min. 4 znaki";
        if (!password.equals(confirmPassword))
            return "Hasła nie są identyczne";
        return null;
    }

    // Metoda statyczna do samej operacji rejestracji
    public static boolean performRegister(String email, String password, String fullName) {
        return userService.register(email, password, fullName);
    }

    // Pomocnik do wyświetlania komunikatów (Sukces/Błąd)
    public static void showInfo(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}