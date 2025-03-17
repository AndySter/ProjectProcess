package org.example;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Класс с выводом окна для обработки и взаимодействия с изображением
 */
public class MainWindow extends Application {

    //Поля для отображения изображений
    private ImageView originalImageView;
    private ImageView processedImageView;

    //Хранение в буфере изначального и обработанного изображения
    private BufferedImage originalImage;
    private BufferedImage processedImage;

    //Элементы управления
    private Slider thresholdSlider;
    private Slider zoomSlider;
    private Label timeLabel;
    private Label dragOverlayLabel;

    //Поля для прокрутки изображений (для детального сравнения при зуме)
    private ScrollPane originalScrollPane;
    private ScrollPane processedScrollPane;
    private StackPane rootPane;

    /**
     * Запуск приложения JavaFX
     * @param args
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Инициализация интерфейса приложения
     * @param primaryStage
     */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Приложение обработки изображений");

        //Панель с кнопками и ползунками
        HBox controlPanel = new HBox(10);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setAlignment(Pos.CENTER_LEFT);

        //Кнопка загрузки изображения
        Button loadButton = new Button("\uD83D\uDCC2 Загрузить");
        loadButton.setOnAction(e -> loadImage(primaryStage));
        controlPanel.getChildren().add(loadButton);

        //Кнопка обработки изображения
        Button processButton = new Button("⌛ Обработать");
        processButton.setOnAction(e -> processImage());
        controlPanel.getChildren().add(processButton);

        //Кнопка сохранения изображения
        Button saveButton = new Button("\uD83D\uDCBE Сохранить");
        saveButton.setOnAction(e -> saveImage(primaryStage));
        controlPanel.getChildren().add(saveButton);

        //Ползунок порога обработки изображения
        controlPanel.getChildren().add(new Label("Порог:"));
        thresholdSlider = new Slider(0, 255, 128);
        thresholdSlider.setShowTickLabels(true);
        thresholdSlider.setShowTickMarks(true);
        thresholdSlider.setMajorTickUnit(50);
        controlPanel.getChildren().add(thresholdSlider);

        //Ползунок для управления масштабом изображений
        controlPanel.getChildren().add(new Label("\uD83D\uDD0D Приблизить:"));
        zoomSlider = new Slider(50, 200, 100);
        zoomSlider.setShowTickLabels(true);
        zoomSlider.setShowTickMarks(true);
        zoomSlider.setMajorTickUnit(50);
        zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateZoom());
        controlPanel.getChildren().add(zoomSlider);

        //Текст с временем обработки
        timeLabel = new Label("Время обработки: - мс");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        controlPanel.getChildren().addAll(spacer, timeLabel);

        originalImageView = new ImageView();
        processedImageView = new ImageView();
        originalScrollPane = new ScrollPane(originalImageView);
        processedScrollPane = new ScrollPane(processedImageView);

        //Включение возможности прокрутки изображений при большом зуме
        enablePanning(originalImageView, originalScrollPane);
        enablePanning(processedImageView, processedScrollPane);

        //Контейнер для изображений
        VBox originalBox = new VBox(5, new Label("Исходное изображение"), originalScrollPane);
        VBox processedBox = new VBox(5, new Label("Обработанное изображение"), processedScrollPane);
        HBox imageBox = new HBox(10, originalBox, processedBox);
        imageBox.setPadding(new Insets(10));
        imageBox.setPrefHeight(600);

        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(controlPanel);
        mainLayout.setCenter(imageBox);

        rootPane = new StackPane();
        rootPane.getChildren().add(mainLayout);

        //Надпись для приложения при перетаскивании файла
        dragOverlayLabel = new Label("Перетащите изображение");
        dragOverlayLabel.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-text-fill: white; -fx-font-size: 20px; -fx-alignment: center;");
        dragOverlayLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        StackPane.setAlignment(dragOverlayLabel, Pos.CENTER);
        dragOverlayLabel.setVisible(false);
        rootPane.getChildren().add(dragOverlayLabel);

        //Создание и запуск сцены
        Scene scene = new Scene(rootPane, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();

        //Получение стилей для кнопок и текста
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        setupDragAndDrop(scene);
    }

    /**
     * Настройка захвата и вставки файла в приложение
     * @param scene
     */
    private void setupDragAndDrop(Scene scene) {
        scene.setOnDragOver(event -> {
            if (event.getGestureSource() != scene && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
                dragOverlayLabel.setVisible(true);
            }
            event.consume();
        });

        scene.setOnDragExited(event -> dragOverlayLabel.setVisible(false));

        scene.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                if (isImageFile(file)) {
                    loadImage(file);
                } else {
                    showAlert("Ошибка", "Можно загружать только изображения (PNG, JPG).");
                }
            }
            dragOverlayLabel.setVisible(false);
            event.setDropCompleted(true);
            event.consume();
        });
    }

    /**
     * Загрузка изображения из диска
     * @param stage
     */
    private void loadImage(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите изображение");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg", "*.bmp"));
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            loadImage(file);
        }
    }

    /**
     * Загрузка изображения из файла в программу, ее отображение в панели
     * @param file файл с изображением
     */
    private void loadImage(File file) {
        try {
            originalImage = ImageIO.read(file);
            if (originalImage != null) {
                originalImageView.setImage(SwingFXUtils.toFXImage(originalImage, null));
                processedImageView.setImage(null);
                timeLabel.setText("Время обработки: - мс");
            }
        } catch (IOException ex) {
            showAlert("Ошибка", "Не удалось загрузить изображение.");
        }
    }

    /**
     * Выполнение обработки изображения с захватом времени обработки, отображение обработанной картинки и
     * времени обработки
     */
    private void processImage() {
        if (originalImage == null) {
            showAlert("Внимание", "Сначала загрузите изображение!");
            return;
        }

        int threshold = (int) thresholdSlider.getValue();

        long startTime = System.nanoTime();
        ImageProcessor processor = new ImageProcessor();
        processedImage = processor.processImage(originalImage, threshold);
        long endTime = System.nanoTime();
        long processingTimeMs = (endTime - startTime) / 1_000_000;
        timeLabel.setText("Время обработки: " + processingTimeMs + " мс");

        if (processedImage != null) {
            processedImageView.setImage(SwingFXUtils.toFXImage(processedImage, null));
        }
    }

    /**
     * Сохранение изображения в виде png файла
     * @param stage
     */
    private void saveImage(Stage stage) {
        if (processedImage == null) {
            showAlert("Внимание", "Нет обработанного изображения для сохранения.");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить изображение");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                ImageIO.write(processedImage, "png", file);
            } catch (IOException ex) {
                showAlert("Ошибка", "Не удалось сохранить изображение.");
            }
        }
    }

    /**
     * Изменение разрешение изображаения с помощью ползунка
     */
    private void updateZoom() {
        double scale = zoomSlider.getValue() / 100.0;
        if (originalImage != null) {
            originalImageView.setFitWidth(originalImage.getWidth() * scale);
            originalImageView.setFitHeight(originalImage.getHeight() * scale);
        }
        if (processedImage != null) {
            processedImageView.setFitWidth(processedImage.getWidth() * scale);
            processedImageView.setFitHeight(processedImage.getHeight() * scale);
        }
    }

    /**
     * Появление окошка с предупреждением
     * @param title название окна
     * @param message текст с предупреждением
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Проверка, что файл является изображением
     * @param file файл изображения, с которого считывается название
     * @return подтверждение, если файл явялется изображением, иначе нет
     */
    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.matches(".*\\.(png|jpg|jpeg|bmp)$");
    }

    /**
     * Включается панорамирование для изображения
     * @param imageView
     * @param scrollPane
     */
    private void enablePanning(ImageView imageView, ScrollPane scrollPane) {
        imageView.setOnMousePressed(event -> scrollPane.setPannable(true));
    }
}

