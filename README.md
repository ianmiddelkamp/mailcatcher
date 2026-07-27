# mailcatcher

A local stand-in for the [Front](https://front.com) API used by `monsterplow/shared`'s
`FrontApp`/`FrontRequestHandler`. It implements just enough of Front's REST surface (message
send endpoints) to capture outbound mail during development and show it on a simple web UI,
instead of hitting the real Front API.

## Running

```
docker build -t mailcatcher .
docker run -p 8091:8091 -v mailcatcher_data:/data mailcatcher
```

Then open http://localhost:8091/.

## Wiring it up to a Front-based app

Point the app at this service instead of the real Front API. In `monsterplow/shared`'s
`FrontApp`, set the `FRONT_API_ENDPOINT_OVERRIDE` environment variable to this service's base
URL (e.g. `http://mailcatcher:8091` inside a docker network) — when set, `FrontApp` sends all
`channels/*/messages` and `inboxes/*/imported_messages` requests here instead of
`api2.frontapp.com`. Leaving it unset is a no-op (real Front API, unchanged behavior).

## What it captures

- `POST /channels/{id}/messages` and `POST /inboxes/{id}/imported_messages` — both JSON and
  Front's multipart format (`to`, `cc`, `bcc`, `subject`, `body`, `sender`/`sender_name`,
  `options[tags]`, `attachments`) are parsed and stored.
- Everything else (contacts CRUD, channel/teammate/inbox listing) gets a harmless stub response
  so calls made during dev don't throw.

## Local development

```
./mvnw spring-boot:run
```

By default the app writes to `/data` (matched to the Docker volume mount). For a local, non-
Docker run, override the paths:

```
MAILCATCHER_DB_PATH=./data/mailcatcher MAILCATCHER_ATTACHMENTS_DIR=./data/attachments ./mvnw spring-boot:run
```
