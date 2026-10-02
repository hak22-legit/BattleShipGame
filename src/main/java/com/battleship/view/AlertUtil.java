package com.battleship.view;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;

import java.util.Optional;

/**
 * centralized utility for presenting dark naval-themed, owner-attached alert dialogs.
 *
 * <p>ensures:
 * <ul>
 *   <li>dialogs are attached to the owning {@link window} so they center properly and
 *       never get obscured behind the main window on linux/x11.</li>
 *   <li>the dark naval stylesheet ({@code battleship.css}) is applied to {@link dialogpane}.</li>
 *   <li>boilerplate alert construction is unified and dry.</li>
 * </ul>
 * </p>
 */
public final class AlertUtil {

    private AlertUtil() { }

    public static void showError(Window owner, String title, String message) {
        show(owner, Alert.AlertType.ERROR, title, message);
    }

    public static void showWarning(Window owner, String title, String message) {
        show(owner, Alert.AlertType.WARNING, title, message);
    }

    public static void showInfo(Window owner, String title, String message) {
        show(owner, Alert.AlertType.INFORMATION, title, message);
    }

    public static boolean showConfirmation(Window owner, String title, String message) {
        Alert alert = createAlert(owner, Alert.AlertType.CONFIRMATION, title, message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static void show(Window owner, Alert.AlertType type, String title, String message) {
        Alert alert = createAlert(owner, type, title, message);
        alert.showAndWait();
    }

    private static Alert createAlert(Window owner, Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane pane = alert.getDialogPane();
        pane.getStyleClass().add("battleship-alert");
        try {
            var css = AlertUtil.class.getResource("/styles/battleship.css");
            if (css != null) {
                pane.getStylesheets().add(css.toExternalForm());
            }
        } catch (Exception ignored) { }

        return alert;
    }
}
