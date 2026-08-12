# Part 2 — Manual Deployment on OCI

This document covers the Part 2 deliverable: taking the Spring Boot app from
Part 1 off `localhost` and running it as a real, persistent service on our
shared OCI Compute Instance, backed by a real Oracle database.

## Reference Links

| Purpose | Link / Value |
|---|---|
| App (upload page) | `http://129.213.126.140.nip.io:8093/test.html` |
| List endpoint | `http://129.213.126.140.nip.io:8093/api/leave-requests/files` |
| Download endpoint | `http://129.213.126.140.nip.io:8093/api/leave-requests/download/{id}` |
| Original repo | `github.com/CodelineAtyab/AgileOraclesOCIEvalutaionArea` |
| My fork | `github.com/MaryamOfiiciallyGitGit/AgileOraclesOCIEvalutaionArea` |
| Google Cloud Console (OAuth clients) | `console.cloud.google.com/auth/clients` |
| Google Cloud Console (branding / test users) | `console.cloud.google.com/auth/branding` |
| Gemini API key | `aistudio.google.com/apikey` |
| Server (SSH/SFTP) | `129.213.126.140` |
| Database | `129.213.126.140:1527`, service `FREEPDB1` |
| OCI bucket (Part 2, current) | `leave-bucket-pair-04` |
| OCI namespace | `idqag2xakgns` |

## Port Allocation (all pairs, for reference)

| Pair | App ports | DB port | Bucket |
|---|---|---|---|
| Alpha | 8081 / 8082 | 1521 | leave-bucket-pair-01 |
| Beta | 8083 / 8084 | 1522 | leave-bucket-pair-01 |
| Gamma | 8085 / 8086 | 1523 | leave-bucket-pair-02 |
| Delta | 8087 / 8088 | 1524 | leave-bucket-pair-02 |
| Epsilon | 8089 / 8090 | 1525 | leave-bucket-pair-03 |
| Zeta | 8091 / 8092 | 1526 | leave-bucket-pair-03 |
| **Eta (us)** | **8093 / 8094** | **1527** | **leave-bucket-pair-04** |
| Theta | 8095 / 8096 | 1528 | leave-bucket-pair-04 |
| Iota | 8097 / 8098 | 1529 | leave-bucket-pair-05 |
| Kappa | 8099 / 8100 | 1530 | leave-bucket-pair-05 |

## Live Deployment

| | |
|---|---|
| **App URL** | `http://129.213.126.140.nip.io:8093/test.html` |
| **Pair** | Eta (Maryam + Safa) |
| **App port** | 8093 |
| **DB port** | 1527 (Service name: `FREEPDB1`) |
| **OCI bucket** | `leave-bucket-pair-04` |
| **Object prefix** | `eta-maryam/` (keeps my uploads distinguishable from my peer's in the shared bucket) |

> We use `129.213.126.140.nip.io` instead of the raw IP because Google OAuth
> rejects bare IP addresses as redirect URIs. `nip.io` is a wildcard DNS
> service that resolves `<ip>.nip.io` straight back to `<ip>` — same server,
> just wrapped in a domain name Google will accept.

## What changed from Part 1

- **Database persistence.** Uploads are no longer categorized synchronously.
  Each upload is saved immediately to Oracle DB (`LEAVE_REQUESTS` table) with
  `leave_category = NULL`.
- **Background categorization worker.** A standalone Java program
  (`CategorizationWorker`) queries for records with a null category, asks
  Gemini for a classification, and updates the row. It runs on its own —
  no user action required.
- **List + Download endpoints**, backed by DB metadata (category, uploader,
  timestamp) rather than raw object names.
- **Minimal UI** in `test.html`: a name-entry step, an upload box, and a
  "Your Files" list with a category filter and a Download button per file.
- **Deployed as a systemd service** so the app survives closing the
  terminal and survives instance reboots.

## Server-side setup

### 1. Oracle Database (Docker)

Runs via `docker-compose.yml` (from the `oracle-free-db-23-slim-container-setup`
zip), mapped to port **1527** on the host. Two isolated schemas are created
on first boot: `eta_maryam` and `eta_safa`, each with their own tablespace.

```bash
cd ~/maryam-oci/oracle-free-db-23-slim-container-setup
docker compose up -d
```

A cron job re-runs this every 5 minutes (harmless if already running) so the
container comes back on the correct port automatically if it's ever removed:

```
*/5 * * * * /home/opc/maryam-oci/ensure-db.sh
```

### 2. The Spring Boot app (systemd)

Built as a fat jar (`gradlew bootJar`) and run via a systemd unit at
`/etc/systemd/system/maryam-leave-portal-app.service`, so it starts on boot
and restarts automatically on failure. All secrets are passed as
`Environment=` variables in that file — nothing sensitive lives in the
repo.

```bash
sudo systemctl restart maryam-leave-portal-app
sudo systemctl status maryam-leave-portal-app
```

### 3. Background categorization worker (cron)

`CategorizationWorker` is a plain Java class (no Spring context) that
connects to the DB directly, finds null-category rows, calls Gemini, and
updates them. It's triggered every 5 minutes via cron so newly uploaded
files get categorized automatically without any manual step:

```
*/5 * * * * /home/opc/maryam-leave-portal-app/run-worker.sh
```

### 4. Networking

- OS firewall (`firewalld`) opened for the app port (8093) and DB port (1527).
- OCI Security List already allowed the full Spring Boot port range
  (8080–8100) and DB port range (1521–1531) for all pairs, so no change was
  needed there once the OS firewall was open.

## API endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/leave-requests/upload` | Upload a `.txt` or `.pdf` leave request |
| GET | `/api/leave-requests/files` | List the authenticated user's requests |
| GET | `/api/leave-requests/download/{id}` | Download a specific file |

## Environment variables

See `leave-portal-app/application.properties.example` for the full list.
Real values are injected via environment variables at runtime (systemd
`Environment=` entries) — they are never committed to the repo.

## Known gotchas (for future reference)

- If the Oracle container is ever recreated from `docker-compose.yml`
  without editing the port mapping first, it reverts to the default port
  (1521) instead of our assigned 1527. Always check `docker ps` after any
  `docker compose up`/`down` before assuming the port is unchanged.
- Google OAuth will not accept a bare IP as a redirect URI — use a
  `nip.io`-style hostname instead.
