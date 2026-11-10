# Публикация CasinoCraft на Modrinth и CurseForge

Готовые поля и тексты. Всё, что нужно, уже лежит в репозитории:
`build/libs/casinocraft-1.7.0.jar`, `docs/screenshots/*.png`, `src/main/resources/assets/casinocraft/icon.png`, `README.md`, `README.ru.md`.

---

## Modrinth (сначала — там публикация мгновенная)

1. Войди на https://modrinth.com и нажми **+** → **Create a project**.
2. Заполни:

| Поле | Значение |
|---|---|
| Name | `CasinoCraft` |
| Slug | `casinocraft` |
| Project type | `Mod` |
| Visibility | `Public` |
| Summary (EN, ≤256) | `A full casino for Minecraft 1.20.1 (Fabric): cashier, slots, roulette, a shop with 1100+ items and physical chips. All currency is virtual.` |
| Summary (RU, ≤256) | `Казино для Minecraft 1.20.1 (Fabric): касса со скупкой предметов, слоты, рулетка, магазин на 1100+ предметов и физические фишки. Вся валюта виртуальная.` |
| Categories | `Economy`, `Minigame`, `Social`, `Game Mechanics` |
| Environment | `Client and server` (мод обязателен на обеих сторонах) |
| License | `MIT` |
| Source | `https://github.com/ImFold/CasinoCraft` |
| Issues | `https://github.com/ImFold/CasinoCraft/issues` |

3. **Description** — вставь содержимое `README.md` (Markdown поддерживается). Русскую версию можно добавить в конец или отдельным блоком со ссылкой на `README.ru.md`.
4. **Gallery** — загрузи 4 скриншота из `docs/screenshots/` (main, roulette, shop, withdraw), иконку — из `src/main/resources/assets/casinocraft/icon.png`.
5. **Versions → Create version**:
   - Version number: `1.7.0`
   - Channel: `Release`
   - Loaders: `Fabric`
   - Game versions: `1.20.1`
   - File: `build/libs/casinocraft-1.7.0.jar`
   - Dependencies: `Fabric API` (обязательная)
   - Changelog — скопируй раздел `1.7.0` из `CHANGELOG.md`.

---

## CurseForge (после Modrinth; публикация может уйти на проверку)

1. Войди на https://www.curseforge.com и открой https://authors.curseforge.com → **Create Project**.
2. Заполни:

| Поле | Значение |
|---|---|
| Project name | `CasinoCraft` |
| Summary | `Казино для Minecraft 1.20.1 (Fabric): касса, слоты, рулетка, магазин на 1100+ предметов и фишки. Вся валюта виртуальная.` |
| Game | `Minecraft` |
| Category | `Adventure and RPG`, `Economy` (если доступна), `Minigame` |
| License | `MIT` |
| Links | GitHub: `https://github.com/ImFold/CasinoCraft` |

3. **Description** — вставь `README.md` (CurseForge понимает Markdown). Русскую версию — в конец или отдельным файлом в галерее.
4. **Files → Upload File**:
   - File: `build/libs/casinocraft-1.7.0.jar`
   - Display name: `CasinoCraft 1.7.0`
   - Release type: `Release`
   - Game version: `1.20.1`, Mod loader: `Fabric`
   - Changelog — из `CHANGELOG.md`.
5. Добавь скриншоты в галерею и иконку проекта.

> CurseForge может попросить подтвердить, что проект не нарушает правила, и иногда отправляет файл на ревью. Modrinth публикует сразу.

---

## После публикации

- В `README.md` и `README.ru.md` добавь строку со ссылками на Modrinth/CurseForge.
- В GitHub Release добавь эти же ссылки в описание.
- В темы GitHub (`Topics`) добавь `modrinth`, `curseforge`.
- Стримерам отправляй ссылку на Modrinth — там кнопка «Download» гораздо понятнее, чем GitHub Releases.

## Важно

Обе площадки требуют аккаунт с подтверждённой почтой. Пароль, 2FA и токены никому не передавай, включая меня: загрузку нужно сделать самому через браузер.

