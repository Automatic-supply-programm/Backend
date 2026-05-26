# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Сборка (без тестов)
mvn package -DskipTests

# Запуск локально
mvn spring-boot:run

# Запуск тестов
mvn test

# Запуск конкретного теста
mvn test -Dtest=ClassName#methodName
```

Приложение стартует на `http://localhost:8080/api` (все эндпоинты имеют префикс `/api`).

## Переменные окружения

Скопировать `.env.example` в `.env`. Локальные дефолты прописаны в `application.properties`:
- `MONGODB_URI` — строка подключения к MongoDB (по умолчанию `mongodb://localhost:27017/warehouse_project_server`)
- `JWT_SECRET` — секрет для подписи JWT (минимум 32 символа)
- `CORS_ALLOWED_ORIGINS` — URL фронтенда через запятую (по умолчанию `http://localhost:3000`)
- `PORT` — порт сервера (по умолчанию `8080`)

## Архитектура

### Стек
- Spring Boot 2.5.4, Java 17, Maven
- MongoDB (Spring Data), без транзакций на уровне репозитория (MongoDB не поддерживает транзакции без replica set)
- JWT аутентификация через `jjwt` 0.11.5, Lombok

### Структура пакетов
```
com.nester
├── configuration/   — SecurityConfiguration, CorsConfig
├── controllers/     — REST-контроллеры
├── dto/             — LoginRequest, UserCreateRequest, ResponseResult<T>
├── model/           — MongoDB-документы
├── repository/      — Spring Data MongoDB репозитории
├── security/jwt/    — JwtTokenProvider, JwtTokenFilter, JwtUser, JwtConfigurer
└── service/         — интерфейсы + Impl-реализации
```

### Паттерн ответов
Все эндпоинты пишут в `HttpServletResponse` напрямую (не `ResponseEntity`). Формат:
```json
{ "message": "...", "data": ... }
```
Поля `null` исключаются (`@JsonInclude(NON_NULL)`). При успехе — `message: null`, при ошибке — `data: null`.

### JWT
Токен передаётся в заголовке: `Authorization: Bearer_<token>` (разделитель — нижнее подчёркивание, не пробел). Токен живёт 1 час (`jwt.token.expired=3600000`).

### Роли и доступ
| Роль | Назначение |
|------|-----------|
| ADMIN | управление пользователями, все заявки, журнал событий |
| WORKER | работник склада: материалы, входящие заявки ISSUE/RECEIPT/RETURN |
| EMPLOYEE | сотрудник производственного участка: создаёт заявки ISSUE/REPLENISHMENT |
| MANAGER | менеджер: входящие заявки REPLENISHMENT, управляет несколькими складами |

Роль хранится в `User.role` как строка без префикса (`"ADMIN"`), в Spring Security — с префиксом (`ROLE_ADMIN`).

### Модели данных

**Material** — хранит `batches: List<MaterialBatch>` (партии поступлений). Поле `status` вычисляется на лету из `currentStock` / `criticalStock`. Списание при выдаче — FIFO по `receiptDate`.

**Request** — заявка. Типы: `ISSUE` (выдача), `REPLENISHMENT` (закупка), `RECEIPT` (поступление от поставщика), `RETURN` (возврат с участка).

Статусные машины по типам:
- **ISSUE**: `UNDER_CONSIDERATION → WAITING_CONFIRMATION → CONFIRMED` (или `CANCELLED`, `REJECTED`)
- **REPLENISHMENT / RECEIPT**: `UNDER_CONSIDERATION → APPROVED / REJECTED / SENT_FOR_REVISION → (правка) → UNDER_CONSIDERATION`
- **RETURN**: `UNDER_CONSIDERATION → ACCEPTED / CANCELLED`

При `RECEIPT` + `APPROVED` или `RETURN` + `ACCEPTED` автоматически создаётся партия и увеличивается `currentStock`. При `ISSUE` + `CONFIRMED` — FIFO-списание из партий. Отменённая (`CANCELLED`) заявка автоматически архивируется.

**User** — `deleted: boolean` (мягкое удаление). Пароль хэшируется BCrypt один раз в `UserServiceImpl.create/update`, в контроллер приходит plain text.

**EventLog** — записывается после каждой значимой операции (создание/изменение заявки, смена пользователя и т.д.).

### Разделение ответственности
- Бизнес-логика (проверки прав владельца/адресата, переходы статусов) — в `ServiceImpl`.
- Логирование событий — в контроллерах после успешного вызова сервиса.
- CORS — через `CorsConfig` (бин `CorsConfigurationSource`), origins из env-переменной.

## Деплой
Приложение деплоится на Railway (`railway.toml`). Dockerfile — двухэтапная сборка: Maven-образ → JRE-alpine.
