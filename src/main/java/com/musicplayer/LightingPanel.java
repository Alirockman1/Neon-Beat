package com.musicplayer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

public class LightingPanel extends JPanel {
    private Color baseColor = Color.WHITE;
    private float intensity = 0.1f;
    private float rotationAngle = 0.0f;

    public LightingPanel() {
        setOpaque(false);
    }

    public void setGenreColor(String genre) {
        if (genre == null || genre.isEmpty()) {
            this.baseColor = new Color(50, 50, 50);
            repaint();
            return;
        }

        String lowerGenre = genre.toLowerCase();

        if (lowerGenre.contains("pop")) {
            this.baseColor = new Color(0, 255, 127);   // Spring Green
        } 
        else if (lowerGenre.contains("rock") || lowerGenre.contains("metal")) {
            this.baseColor = new Color(255, 50, 50);   // Bright Red
        } 
        else if (lowerGenre.contains("jazz") || lowerGenre.contains("blues")) {
            this.baseColor = new Color(138, 43, 226);  // Deep Purple
        } 
        else if (lowerGenre.contains("soundtrack") || lowerGenre.contains("score") || lowerGenre.contains("film")) {
            this.baseColor = new Color(255, 215, 0);   // Golden Glow
        } 
        else if (lowerGenre.contains("hip hop") || lowerGenre.contains("rap")) {
            this.baseColor = new Color(255, 105, 180); // Hot Pink
        }
        else {
            this.baseColor = new Color(50, 50, 50);    // Default Subtle Gray
        }
        repaint();
    }

    public void setIntensity(float intensity) {
        // Clamp value between 0 and 1
        this.intensity = Math.max(0.0f, Math.min(1.0f, intensity));
        
        // Slowly increment rotation to make the star "drift"
        this.rotationAngle += 0.01f + (this.intensity * 0.02f); 
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2d = (Graphics2D) graphics;

       // System.out.println(intensity);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // --- SHARPNESS LOGIC ---
        double outerRadius = Math.min(centerX, centerY) - 10;
        double innerRadiusFactor = 0.85 - (intensity * 0.45);
        double innerRadius = outerRadius * innerRadiusFactor;

        int numPeaks = 70;
        int totalPoints = numPeaks * 2;
        Path2D star = new Path2D.Double();

        for (int i = 0; i < totalPoints; i++) {
            double angle = Math.toRadians(i * (360.0 / totalPoints) - 90) + rotationAngle;
            double currentRadius = (i % 2 == 0) ? outerRadius : innerRadius;

            double x = centerX + currentRadius * Math.cos(angle);
            double y = centerY + currentRadius * Math.sin(angle);

            if (i == 0) star.moveTo(x, y);
            else star.lineTo(x, y);
        }
        star.closePath();

        float[] hsb = Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), null);

        float hueShift = hsb[0] + (intensity * 0.05f); 
        
        float dynamicSat = 0.5f + (intensity * 0.5f); 
        float dynamicBright = 0.4f + (intensity * 0.6f);

        Color dynamicBase = Color.getHSBColor(hueShift, dynamicSat, dynamicBright);
        
        // Whiter center for that "Glow" look
        Color centerColor = Color.getHSBColor(hsb[0], dynamicSat * 0.5f, Math.min(1.0f, dynamicBright + 0.2f));
        Color edgeColor = new Color(dynamicBase.getRed(), dynamicBase.getGreen(), dynamicBase.getBlue(), 0);

        // Ensure smoothing of colour
        float gradientRadius = (float) (outerRadius * (0.6 + intensity * 0.4));
        RadialGradientPaint glow = new RadialGradientPaint(
            new Point(centerX, centerY),
            gradientRadius > 0 ? gradientRadius : 1.0f,
            new float[]{0.0f, 1.0f},
            new Color[]{
                new Color(centerColor.getRed(), centerColor.getGreen(), centerColor.getBlue(), (int)(160 + intensity * 95)), 
                edgeColor
            }
        );

        g2d.setPaint(glow);
        g2d.fill(star);
        
        // Set the color of the background player
        g2d.setColor(new Color(dynamicBase.getRed(), dynamicBase.getGreen(), dynamicBase.getBlue(), (int)(40 + intensity * 215)));
        g2d.setStroke(new BasicStroke(1.0f + intensity * 1.5f)); 
        g2d.draw(star);
    }
}
