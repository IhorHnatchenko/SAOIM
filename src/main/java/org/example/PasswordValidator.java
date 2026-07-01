package org.example;

public class PasswordValidator {

    // Метод для валидации логина по твоим критериям
    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        // \p{L} — любые буквы (кириллица, латиница и др.), 0-9 — цифры, _ — подчёркивание, \- — дефис, \. — точка. Длина от 3 до 32.
        String regex = "^[\\p{L}0-9_\\-\\\\.]{3,32}$";
        return username.matches(regex);
    }

    // Комплексный анализ пароля на простоту
    public static boolean isTooSimple(String password) {
        if (password == null || password.length() < 12) return true;

        // 1. Проверка на абсолютно одинаковые символы (например: aaaaaaaaaaaa)
        if (password.chars().distinct().count() == 1) {
            System.out.println("[Security] Пароль отклонен: состоит из одного символа.");
            return true;
        }

        // 2. Проверка на непрерывную последовательность (например: 12345678910... или abcdefg...)
        if (isSequential(password)) {
            System.out.println("[Security] Пароль отклонен: обнаружена простая последовательность.");
            return true;
        }

        // 3. Проверка на циклические повторения коротких блоков (от 2 до 6 символов)
        // Покрывает: 121212121212, 123123123123, 123456123456 и т.д.
        if (hasRepeatingPatterns(password)) {
            System.out.println("[Security] Пароль отклонен: обнаружен циклический паттерн.");
            return true;
        }

        return false;
    }

    // Алгоритм проверки последовательности символов (вперед и назад)
    private static boolean isSequential(String str) {
        boolean forward = true;
        boolean backward = true;

        for (int i = 0; i < str.length() - 1; i++) {
            if (str.charAt(i + 1) != str.charAt(i) + 1) forward = false;
            if (str.charAt(i + 1) != str.charAt(i) - 1) backward = false;
        }
        return forward || backward;
    }

    // Алгоритм поиска повторяющихся блоков (длиной от 2 до 6 символов)
    private static boolean hasRepeatingPatterns(String str) {
        int len = str.length();
        for (int blockSize = 2; blockSize <= 6; blockSize++) {
            if (len % blockSize == 0) {
                String block = str.substring(0, blockSize);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < len / blockSize; i++) {
                    sb.append(block);
                }
                if (sb.toString().equals(str)) {
                    return true;
                }
            }
        }
        return false;
    }
}