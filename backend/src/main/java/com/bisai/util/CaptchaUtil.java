package com.bisai.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;

public class CaptchaUtil {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LENGTH = 4;
    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final Random RANDOM = new Random();

    public static CaptchaResult generate() {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 背景
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 干扰线
        for (int i = 0; i < 5; i++) {
            g.setColor(randomColor(150, 200));
            g.drawLine(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT),
                    RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
        }

        // 验证码字符
        StringBuilder code = new StringBuilder();
        g.setFont(new Font("Arial", Font.BOLD, 28));
        for (int i = 0; i < CODE_LENGTH; i++) {
            String ch = String.valueOf(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            code.append(ch);
            g.setColor(randomColor(50, 120));
            g.drawString(ch, 20 * i + 15, 28 + RANDOM.nextInt(5));
        }

        // 干扰点
        for (int i = 0; i < 30; i++) {
            g.setColor(randomColor(100, 200));
            g.fillOval(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), 2, 2);
        }

        g.dispose();

        // 转为 Base64
        String base64Image;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("生成验证码失败", e);
        }

        return new CaptchaResult(code.toString(), "data:image/png;base64," + base64Image);
    }

    private static Color randomColor(int min, int max) {
        int r = min + RANDOM.nextInt(max - min);
        int g = min + RANDOM.nextInt(max - min);
        int b = min + RANDOM.nextInt(max - min);
        return new Color(r, g, b);
    }

    public record CaptchaResult(String code, String imageBase64) {}
}
