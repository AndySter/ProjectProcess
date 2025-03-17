package org.example;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.stream.IntStream;

/**
 * Обработка изображений, выделяющая пиксели по яркости
 */
public class ImageProcessor {
    /**
     * Метод с помощью пороговой обработку к изображению, выделяя яркие области
     *
     * @param inputImage Исходное изображение
     * @param threshold  Порог яркости (0-255)
     * @return Обработанное изображение
     */
    public BufferedImage processImage(BufferedImage inputImage, int threshold) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();

        // Перевод в формат ARGB для точной обработки пикселей (тк jpg не поддерживает прозрачность)
        BufferedImage formattedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = formattedImage.createGraphics();
        g.drawImage(inputImage, 0, 0, null);
        g.dispose();

        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Массивы пикселей для последуюшего доступа к ним (двумерный массив преобразуем в одномерный)
        int[] inputPixels = ((DataBufferInt) formattedImage.getRaster().getDataBuffer()).getData();
        int[] outputPixels = ((DataBufferInt) outputImage.getRaster().getDataBuffer()).getData();

        // Обработка изображения в многопотоке
        IntStream.range(0, width * height).parallel().forEach(i -> {
            int pixel = inputPixels[i];

            int alpha = (pixel >> 24) & 0xFF;
            int red = (pixel >> 16) & 0xFF;
            int green = (pixel >> 8) & 0xFF;
            int blue = pixel & 0xFF;

            // Яркость пикселя
            int brightness = (red + green + blue) / 3;

            // Замена цвета на желтый
            if (brightness > threshold) {
                outputPixels[i] = (alpha << 24) | (255 << 16) | (255 << 8) | 0;
            } else {
                outputPixels[i] = pixel;
            }
        });

        return outputImage;
    }
}