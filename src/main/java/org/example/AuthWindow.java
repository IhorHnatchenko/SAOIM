package org.example;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.*;
import java.util.Properties;

public class AuthWindow {

    private Stage stage;
    private Stage dummyOwner;
    private VBox formContainer;
    private Runnable onLoginSuccess;

    private final double WINDOW_WIDTH = 280;
    private final double WINDOW_HEIGHT = 440;

    private final String BOX_STYLE =
            "-fx-background-color: rgba(25, 25, 25, 0.95); " +
                    "-fx-background-radius: 15; " +
                    "-fx-border-color: #ffffff; " +
                    "-fx-border-width: 1.5;";

    private final String FIELD_STYLE =
            "-fx-background-color: rgba(255,255,255,0.1); " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 14px; " +
                    "-fx-border-color: gray; " +
                    "-fx-border-radius: 5; " +
                    "-fx-background-radius: 5;";

    private final String BTN_STYLE =
            "-fx-background-color: rgba(255,255,255,0.15); " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 14px; " +
                    "-fx-background-radius: 8; " +
                    "-fx-cursor: hand;";

    private final String ACCENT_BTN_STYLE =
            "-fx-background-color: #ff9900; " +
                    "-fx-text-fill: black; " +
                    "-fx-font-size: 14px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-background-radius: 8; " +
                    "-fx-cursor: hand;";

    private final String LABEL_STYLE =
            "-fx-text-fill: #b3b3b3; " +
                    "-fx-font-size: 12px;";

    private final String CHECKBOX_STYLE =
            "-fx-text-fill: #b3b3b3; " +
                    "-fx-font-size: 13px; " +
                    "-fx-cursor: hand; " +
                    "-fx-box-background: rgba(255,255,255,0.1); " +
                    "-fx-box-border: gray; " +
                    "-fx-mark-color: #ff9900;";

    private final String CONFIG_FILE = "sao_config.properties";

    public AuthWindow(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }

    public void show() {

        if (stage != null) {
            showLoginScreen();
            stage.show();
            return;
        }

        dummyOwner = new Stage(StageStyle.UTILITY);

        dummyOwner.setOpacity(0);
        dummyOwner.setWidth(0);
        dummyOwner.setHeight(0);
        dummyOwner.setX(-100);

        dummyOwner.show();

        formContainer = new VBox(15);

        formContainer.setPadding(
                new Insets(30, 25, 30, 25)
        );

        formContainer.setStyle(BOX_STYLE);
        formContainer.setPrefSize(
                WINDOW_WIDTH,
                WINDOW_HEIGHT
        );

        formContainer.setAlignment(Pos.CENTER);

        formContainer.setOnMousePressed(e -> {

            if (e.getButton() == MouseButton.SECONDARY) {
                e.consume();
            }

        });

        formContainer.setOnMouseReleased(e -> {

            if (e.getButton() == MouseButton.SECONDARY) {
                e.consume();
            }

        });

        Pane rootPane = new Pane(formContainer);

        rootPane.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.01);"
        );

        rootPane.setPickOnBounds(true);

        rootPane.setOnMousePressed(e -> {

            if (e.getButton() == MouseButton.SECONDARY) {
                e.consume();
            }

        });

        rootPane.setOnMouseReleased(e -> {

            if (e.getButton() == MouseButton.SECONDARY) {

                e.consume();
                stage.hide();
            }

        });

        rootPane.setOnContextMenuRequested(
                e -> e.consume()
        );

        Scene scene = new Scene(rootPane);

        scene.setFill(Color.TRANSPARENT);

        stage = new Stage();

        stage.initOwner(dummyOwner);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        stage.setScene(scene);

        Rectangle2D bounds =
                Screen.getPrimary().getBounds();

        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());

        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());

        formContainer.setLayoutX(50);

        formContainer.setLayoutY(
                (bounds.getHeight() - WINDOW_HEIGHT) / 2
        );

        showLoginScreen();

        stage.show();
    }

    private void showLoginScreen() {

        formContainer.getChildren().clear();

        Label title =
                new Label("SAO LOGIN");

        title.setStyle(
                "-fx-text-fill: white; " +
                        "-fx-font-size: 18px; " +
                        "-fx-font-weight: bold;"
        );

        TextField txtUser =
                new TextField();

        txtUser.setPromptText("Username");
        txtUser.setStyle(FIELD_STYLE);

        PasswordField txtPass =
                new PasswordField();

        txtPass.setPromptText("Password");
        txtPass.setStyle(FIELD_STYLE);

        txtPass.setText("");

        CheckBox chkRemember =
                new CheckBox("Remember Me");

        chkRemember.setStyle(CHECKBOX_STYLE);
        chkRemember.setMaxWidth(Double.MAX_VALUE);

        String[] savedCredentials =
                loadSavedCredentials();

        if (savedCredentials != null) {

            txtUser.setText(
                    savedCredentials[0]
            );

            txtPass.setText(
                    savedCredentials[1]
            );

            chkRemember.setSelected(true);

        } else {

            txtUser.setText(
                    System.getProperty("user.name")
            );
        }

        Button btnLogin =
                new Button("LINK START");

        btnLogin.setStyle(
                ACCENT_BTN_STYLE
        );

        btnLogin.setMaxWidth(
                Double.MAX_VALUE
        );

        btnLogin.setDefaultButton(true);

        Label lblRegister =
                new Label(
                        "New User? Create Account"
                );

        lblRegister.setStyle(
                LABEL_STYLE +
                        "-fx-underline: true; " +
                        "-fx-cursor: hand;"
        );

        lblRegister.setOnMouseClicked(
                e -> showRegisterScreen()
        );

        /*
         * ВАЖНО:
         * Вход пока всё ещё работает напрямую
         * через старый AuthService и MySQL.
         *
         * Это нормально.
         * Авторизацию перенесём на сервер
         * на следующем этапе.
         */
        btnLogin.setOnAction(e -> {

            String username =
                    txtUser.getText();

            String password =
                    txtPass.getText();

            if (AuthService.loginUser(
                    username,
                    password
            )) {

                if (chkRemember.isSelected()) {

                    saveCredentials(
                            username,
                            password
                    );

                } else {

                    clearSavedCredentials();
                }

                stage.hide();

                MainStarter.setCurrentUsername(
                        username
                );

                onLoginSuccess.run();

            } else {

                title.setText(
                        "INVALID PASS!"
                );

                title.setStyle(
                        "-fx-text-fill: #ff3333; " +
                                "-fx-font-size: 16px;"
                );

                txtPass.setText("");
            }
        });

        formContainer
                .getChildren()
                .addAll(
                        title,
                        txtUser,
                        txtPass,
                        chkRemember,
                        btnLogin,
                        lblRegister
                );
    }

    private void showRegisterScreen() {

        formContainer.getChildren().clear();

        Label title =
                new Label("REGISTRATION");

        title.setStyle(
                "-fx-text-fill: white; " +
                        "-fx-font-size: 18px; " +
                        "-fx-font-weight: bold;"
        );

        TextField txtUser =
                new TextField();

        txtUser.setPromptText(
                "Username (3-32 chars)"
        );

        txtUser.setStyle(
                FIELD_STYLE
        );

        TextField txtEmail =
                new TextField();

        txtEmail.setPromptText(
                "Enter Email"
        );

        txtEmail.setStyle(
                FIELD_STYLE
        );

        PasswordField txtPass =
                new PasswordField();

        txtPass.setPromptText(
                "Password (12-128 chars)"
        );

        txtPass.setStyle(
                FIELD_STYLE
        );

        Button btnFinish =
                new Button("CREATE & SYNC");

        btnFinish.setStyle(
                ACCENT_BTN_STYLE
        );

        btnFinish.setMaxWidth(
                Double.MAX_VALUE
        );

        btnFinish.setDefaultButton(true);

        Button btnBack =
                new Button("BACK");

        btnBack.setStyle(
                BTN_STYLE
        );

        btnBack.setMaxWidth(
                Double.MAX_VALUE
        );

        btnBack.setOnAction(
                e -> showLoginScreen()
        );

        /*
         * НОВАЯ РЕГИСТРАЦИЯ
         *
         * Теперь здесь больше нет:
         *
         * AuthService.registerUser(...)
         *
         * Вместо прямого подключения к MySQL
         * запрос отправляется в SAOIM-Server.
         */
        btnFinish.setOnAction(e -> {

            String username =
                    txtUser
                            .getText()
                            .trim();

            String email =
                    txtEmail
                            .getText()
                            .trim();

            String password =
                    txtPass.getText();

            /*
             * Проверяем пустые поля.
             */
            if (username.isEmpty()
                    || email.isEmpty()
                    || password.isEmpty()) {

                title.setText(
                        "EMPTY FIELDS!"
                );

                title.setStyle(
                        "-fx-text-fill: #ff3333; " +
                                "-fx-font-size: 16px; " +
                                "-fx-font-weight: bold;"
                );

                return;
            }

            /*
             * Проверяем username
             * твоим существующим валидатором.
             */
            if (!PasswordValidator
                    .isValidUsername(username)) {

                title.setText(
                        "INVALID USERNAME!"
                );

                title.setStyle(
                        "-fx-text-fill: #ff3333; " +
                                "-fx-font-size: 15px; " +
                                "-fx-font-weight: bold;"
                );

                return;
            }

            /*
             * Проверяем длину пароля.
             */
            if (password.length() < 12
                    || password.length() > 128) {

                title.setText(
                        "PASSWORD: 12-128 CHARS!"
                );

                title.setStyle(
                        "-fx-text-fill: #ff3333; " +
                                "-fx-font-size: 14px; " +
                                "-fx-font-weight: bold;"
                );

                txtPass.setText("");

                return;
            }

            /*
             * Проверяем сложность пароля.
             */
            if (PasswordValidator
                    .isTooSimple(password)) {

                title.setText(
                        "PASSWORD TOO SIMPLE!"
                );

                title.setStyle(
                        "-fx-text-fill: #ff3333; " +
                                "-fx-font-size: 15px; " +
                                "-fx-font-weight: bold;"
                );

                txtPass.setText("");

                return;
            }

            try {

                /*
                 * НОВОЕ:
                 *
                 * Здесь SAOIM отправляет
                 * HTTP POST запрос:
                 *
                 * POST /api/register
                 *
                 * в SAOIM-Server.
                 */
                RegisterResponse response =
                        ApiClient.register(
                                username,
                                email,
                                password,
                                "",
                                ""
                        );

                /*
                 * Сервер успешно
                 * зарегистрировал пользователя.
                 */
                if (response.success()) {

                    title.setText(
                            "SUCCESSFULLY SYNCED!"
                    );

                    title.setStyle(
                            "-fx-text-fill: #00ff66; " +
                                    "-fx-font-size: 16px; " +
                                    "-fx-font-weight: bold;"
                    );

                    System.out.println(
                            "[API] Account created. " +
                                    "SAO ID: " +
                                    response.saoId()
                    );

                    PauseTransition pause =
                            new PauseTransition(
                                    Duration.seconds(1.5)
                            );

                    pause.setOnFinished(
                            event ->
                                    showLoginScreen()
                    );

                    pause.play();

                } else {

                    /*
                     * Сервер ответил,
                     * но регистрация запрещена.
                     *
                     * Например:
                     * USERNAME TAKEN!
                     * EMAIL ALREADY EXISTS!
                     */
                    title.setText(
                            response.message()
                    );

                    title.setStyle(
                            "-fx-text-fill: #ff3333; " +
                                    "-fx-font-size: 15px; " +
                                    "-fx-font-weight: bold;"
                    );
                }

            } catch (Exception ex) {

                /*
                 * Если SAOIM-Server выключен,
                 * localhost:8080 недоступен,
                 * либо произошла другая
                 * ошибка сети.
                 */
                System.err.println(
                        "[API] Server connection failed: "
                                + ex.getMessage()
                );

                ex.printStackTrace();

                title.setText(
                        "SERVER OFFLINE!"
                );

                title.setStyle(
                        "-fx-text-fill: #ff3333; " +
                                "-fx-font-size: 15px; " +
                                "-fx-font-weight: bold;"
                );
            }
        });

        formContainer
                .getChildren()
                .addAll(
                        title,
                        txtUser,
                        txtEmail,
                        txtPass,
                        btnFinish,
                        btnBack
                );
    }

    private void saveCredentials(
            String username,
            String password
    ) {

        Properties props =
                new Properties();

        props.setProperty(
                "username",
                username
        );

        props.setProperty(
                "password",
                password
        );

        try (
                OutputStream out =
                        new FileOutputStream(
                                CONFIG_FILE
                        )
        ) {

            props.store(
                    out,
                    "SAO UI Saved Session"
            );

        } catch (IOException e) {

            System.err.println(
                    "[Config] Не удалось сохранить сессию: "
                            + e.getMessage()
            );
        }
    }

    private String[] loadSavedCredentials() {

        File file =
                new File(CONFIG_FILE);

        if (!file.exists()) {
            return null;
        }

        Properties props =
                new Properties();

        try (
                InputStream in =
                        new FileInputStream(
                                CONFIG_FILE
                        )
        ) {

            props.load(in);

            return new String[]{
                    props.getProperty("username"),
                    props.getProperty("password")
            };

        } catch (IOException e) {

            return null;
        }
    }

    public boolean hasSavedSession() {

        File file =
                new File(CONFIG_FILE);

        if (!file.exists()) {
            return false;
        }

        String[] credentials =
                loadSavedCredentials();

        if (credentials != null
                && credentials[0] != null
                && credentials[1] != null) {

            /*
             * Авторизация пока старая.
             * Перенесём её на сервер
             * в Этапе 5.
             */
            return AuthService.loginUser(
                    credentials[0],
                    credentials[1]
            );
        }

        return false;
    }

    public void clearSavedCredentials() {

        File file =
                new File(CONFIG_FILE);

        if (file.exists()) {

            boolean deleted =
                    file.delete();

            if (!deleted) {

                System.err.println(
                        "[Config] Не удалось удалить файл сессии."
                );
            }
        }
    }
}