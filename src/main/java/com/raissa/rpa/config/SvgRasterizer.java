package com.raissa.rpa.config;

import org.apache.batik.parser.AWTPathProducer;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;

public class SvgRasterizer {
    private static final int GRID_SIZE = 48;
    private static final int PADDING = 4;

    private SvgRasterizer() {
    }

    public static double[] fingerprint(String pathData) throws ParseException {
        if (pathData == null || pathData.isBlank()) {
            throw new IllegalArgumentException("El atributo d no puede estar vacío.");
        }
        Shape shape = parseShape(pathData);
        Rectangle2D bounds = shape.getBounds2D();
        if (bounds.isEmpty()) {
            throw new IllegalArgumentException("El path SVG no posee un contorno válido.");
        }

        double scale = (double) (GRID_SIZE - 2 * PADDING) / Math.max(bounds.getWidth(), bounds.getHeight());
        AffineTransform transform = new AffineTransform();
        transform.translate(PADDING - bounds.getX() * scale, PADDING - bounds.getY() * scale);
        transform.scale(scale, scale);

        Shape normalized = transform.createTransformedShape(shape);

        BufferedImage image = new BufferedImage(GRID_SIZE, GRID_SIZE, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = image.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setBackground(Color.BLACK);
            g2d.clearRect(0, 0, GRID_SIZE, GRID_SIZE);
            g2d.setColor(Color.WHITE);
            g2d.fill(normalized);
        } finally {
            g2d.dispose();
        }

        double[] vector = new double[GRID_SIZE * GRID_SIZE];
        int[] pixel = new int[1];
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                image.getRaster().getPixel(x, y, pixel);
                vector[y * GRID_SIZE + x] = pixel[0] / 255.0;
            }
        }
        return vector;
    }

    private static Shape parseShape(String pathData) throws ParseException {
        try {
            return AWTPathProducer.createShape(new StringReader(pathData), PathIterator.WIND_NON_ZERO);
        } catch (IOException ex) {
            throw new IllegalStateException("Error inesperado leyendo el path SVG.", ex);
        }
    }
}
