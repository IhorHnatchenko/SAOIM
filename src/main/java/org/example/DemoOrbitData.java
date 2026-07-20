package org.example;

import java.util.List;

/**
 * Temporary hierarchy used to verify paging and nested navigation in step 4.
 * It is intentionally isolated so database-backed data can replace it later.
 */
public final class DemoOrbitData {

    private DemoOrbitData() {
    }

    public static List<OrbitEntry> createRootEntries() {
        OrbitEntry development = OrbitEntry.category(
                "work-development",
                "Разработка",
                "⌘",
                "Инструменты и ресурсы для разработки",
                OrbitEntry.item("dev-java", "Java", "J", "Java-проекты и материалы"),
                OrbitEntry.item("dev-idea", "IntelliJ IDEA", "IJ", "Среда разработки"),
                OrbitEntry.item("dev-github", "GitHub", "GH", "Репозитории и задачи"),
                OrbitEntry.item("dev-maven", "Maven", "M", "Сборка Java-проектов"),
                OrbitEntry.item("dev-docker", "Docker", "D", "Контейнеры и окружения"),
                OrbitEntry.item("dev-db", "Базы данных", "DB", "Инструменты работы с данными"),
                OrbitEntry.item("dev-docs", "Документация", "≡", "Техническая документация")
        );

        OrbitEntry work = OrbitEntry.category(
                "work",
                "Работа",
                "▣",
                "Рабочие категории и приложения",
                development,
                OrbitEntry.item("work-documents", "Документы", "▤", "Рабочие документы"),
                OrbitEntry.item("work-mail", "Почта", "✉", "Рабочая почта"),
                OrbitEntry.item("work-calendar", "Календарь", "▦", "Встречи и события"),
                OrbitEntry.item("work-projects", "Проекты", "▥", "Текущие проекты"),
                OrbitEntry.item("work-analytics", "Аналитика", "⌁", "Отчёты и аналитика")
        );

        OrbitEntry media = OrbitEntry.category(
                "media",
                "Медиа",
                "▷",
                "Видео, музыка, изображения и трансляции",
                OrbitEntry.item("media-music", "Музыка", "♫", "Музыкальные приложения"),
                OrbitEntry.item("media-video", "Видео", "▶", "Видео и кино"),
                OrbitEntry.item("media-photo", "Фото", "▧", "Фотографии и графика"),
                OrbitEntry.item("media-podcasts", "Подкасты", "◉", "Подкасты"),
                OrbitEntry.item("media-stream", "Стриминг", "⌁", "Стриминговые сервисы"),
                OrbitEntry.item("media-edit", "Монтаж", "✂", "Видео- и аудиомонтаж")
        );

        OrbitEntry system = OrbitEntry.category(
                "system",
                "Система",
                "⬡",
                "Системные инструменты Windows",
                OrbitEntry.item("system-files", "Проводник", "▱", "Файлы и папки"),
                OrbitEntry.item("system-settings", "Настройки", "⚙", "Настройки системы"),
                OrbitEntry.item("system-security", "Безопасность", "◇", "Защита устройства"),
                OrbitEntry.item("system-updates", "Обновления", "↻", "Обновления системы")
        );

        OrbitEntry learning = OrbitEntry.category(
                "learning",
                "Обучение",
                "◇",
                "Учебные материалы и практика",
                OrbitEntry.item("learn-courses", "Курсы", "▤", "Онлайн-курсы"),
                OrbitEntry.item("learn-books", "Книги", "▥", "Электронные книги"),
                OrbitEntry.item("learn-notes", "Заметки", "✎", "Учебные заметки"),
                OrbitEntry.item("learn-practice", "Практика", "⌘", "Практические задания")
        );

        OrbitEntry games = OrbitEntry.category(
                "games",
                "Игры",
                "✦",
                "Игры и игровые сервисы",
                OrbitEntry.item("games-library", "Библиотека", "▦", "Библиотека игр"),
                OrbitEntry.item("games-launchers", "Лаунчеры", "▶", "Игровые лаунчеры"),
                OrbitEntry.item("games-achievements", "Достижения", "★", "Игровые достижения"),
                OrbitEntry.item("games-captures", "Записи", "●", "Скриншоты и записи")
        );

        return List.of(
                work,
                media,
                system,
                learning,
                games,
                OrbitEntry.category(
                        "communication",
                        "Связь",
                        "✉",
                        "Мессенджеры и звонки",
                        OrbitEntry.item("communication-chat", "Мессенджеры", "✉", "Текстовое общение"),
                        OrbitEntry.item("communication-calls", "Звонки", "◉", "Голосовая связь")
                ),
                OrbitEntry.category(
                        "finance",
                        "Финансы",
                        "⌁",
                        "Финансовые приложения",
                        OrbitEntry.item("finance-bank", "Банкинг", "▥", "Банковские приложения"),
                        OrbitEntry.item("finance-budget", "Бюджет", "▤", "Учёт бюджета")
                ),
                OrbitEntry.category(
                        "archive",
                        "Архив",
                        "▱",
                        "Редко используемые ресурсы",
                        OrbitEntry.item("archive-old", "Старые проекты", "▱", "Архив проектов")
                )
        );
    }
}
