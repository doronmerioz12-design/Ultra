# Ultra — Bletchley Park Intelligence Simulation

A Java application simulating the WW2 British "Ultra" intelligence system at Bletchley Park. Encrypted enemy messages are intercepted, decrypted using classical cipher-breaking techniques, classified by an AI model (Google Gemini), and routed to the appropriate military branch.

## Architecture

```
┌─────────────────────┐
│  Radio Simulator    │  ← Simulates intercepted encrypted messages
│  (CLI input → DB)   │
└────────┬────────────┘
         │ INSERT into INPUT table
         ▼
┌─────────────────────┐
│  Repository         │  ← Polls for PENDING messages
└────────┬────────────┘
         │ Message objects
         ▼
┌─────────────────────┐
│  Ultra Orchestrator │  ← Central pipeline
│  ┌───────────────┐  │
│  │ CipherFactory │──┼──→ CaesarDecrypt / VigenereCracker
│  └───────────────┘  │
│  ┌───────────────┐  │
│  │ LLM Classifier│──┼──→ Gemini API (determines branch + urgency)
│  └───────────────┘  │
└────────┬────────────┘
         │ Notify observers
         ▼
┌─────────────────────┐
│  Branch Observers   │  ← Army, Navy, RAF, PM Office, SHAEF
│  (write to branch   │
│   specific DB table)│
└─────────────────────┘
```

## Design Patterns

| Pattern | Usage |
|---------|-------|
| **Strategy** | `DecryptionStrategy` interface with `CeaserDecrypt` and `VigenereCracker` implementations |
| **Observer** | Branch classes observe decrypted messages and store them independently |
| **Factory** | `CipherFactory` selects the appropriate decryption strategy |

## Project Structure

```
src/
├── Ultra/
│   ├── Main.java                  # Entry point (CLI menu)
│   ├── Ultra.java                 # Orchestrator (decrypt → classify → route)
│   └── CipherFactory.java        # Strategy factory for cipher selection
├── branches/
│   ├── Observer.java              # Observer interface
│   ├── AbstractBranchObserver.java # Shared DB logic for all branches
│   ├── Army.java
│   ├── Navy.java
│   ├── RAF.java
│   ├── PMOFFICE.java
│   └── SHAEF.java
├── classify/
│   ├── MessageClassifier.java     # Classifier interface
│   └── LlmMessageClassifier.java  # Gemini-based classification with retry
├── decrypt/
│   ├── DecryptionStrategy.java    # Strategy interface
│   ├── CeaserDecrypt.java         # Caesar cipher breaker (chi-squared fallback)
│   └── VigenereCracker.java       # Vigenère cipher breaker (Kasiski method)
├── InputAndProcess/
│   ├── InputReciver.java          # Input interface
│   ├── RadioStationSimulator.java # CLI-based message insertion
│   └── Repository.java            # DB access for pending messages
└── model/
    ├── Message.java               # Intercepted message
    ├── Branch.java                # Destination enum (NAVY, RAF, ARMY, etc.)
    ├── Urgency.java               # Urgency level enum
    ├── ClassificationResult.java  # Type-safe classifier output
    └── Progress.java              # Message processing status enum
```

## Prerequisites

- **Java 17+**
- **Maven**
- **PostgreSQL** running locally on port 5432
- **Google Gemini API key**

## Setup

### 1. Database

Create a PostgreSQL database and user:

```sql
CREATE DATABASE my_database;
CREATE USER "ULTRA" WITH PASSWORD 'ULTRA123';
GRANT ALL PRIVILEGES ON DATABASE my_database TO "ULTRA";
```

The application creates its own tables on first run.

### 2. Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `GEMINI_API_KEY` | Google AI Studio API key (required for classification) | — |
| `ULTRA_DB_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/my_database` |
| `ARMY_DB_USER` / `ARMY_DB_PASSWORD` | Army branch DB credentials | `Army` / `Army123` |
| `NAVY_DB_USER` / `NAVY_DB_PASSWORD` | Navy branch DB credentials | `Navy` / `Navy123` |
| `RAF_DB_USER` / `RAF_DB_PASSWORD` | RAF branch DB credentials | `RAF` / `RAF123` |
| `PMOFFICE_DB_USER` / `PMOFFICE_DB_PASSWORD` | PM Office DB credentials | `PMOFFICE` / `PMOFFICE123` |
| `SHAEF_DB_USER` / `SHAEF_DB_PASSWORD` | SHAEF DB credentials | `SHAEF` / `SHAEF123` |

### 3. Build & Run

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="Ultra.Main"
```

Or run `Ultra.Main` directly from IntelliJ IDEA.

## Usage

```
=== ULTRA - Bletchley Park Intelligence System ===
1. Insert intercepted messages (Radio Simulator)
2. Process pending messages
3. Insert then process
4. Exit
```

1. **Option 1** — Enter encrypted messages and their cipher type (CAESAR / VIGENERE)
2. **Option 2** — Process all pending messages through the full pipeline
3. **Option 3** — Insert then immediately process

## How Decryption Works

### Caesar Cipher
Generates all 26 possible shifts and scores each against English letter frequency distribution using chi-squared analysis. If an external analysis API is available at `localhost:8080`, it uses that instead.

### Vigenère Cipher
Uses the **Kasiski examination** method to guess key length by finding repeated trigram distances, then applies frequency analysis on each key position (assuming 'E' is the most common letter) to recover the key.

## Classification

The `LlmMessageClassifier` sends the decrypted plaintext to Google Gemini, which returns a JSON response with:
- **destination**: which branch should receive the message (NAVY, RAF, ARMY, PM_OFFICE, SHAEF, or ALL)
- **urgency**: how urgent the message is (LOW, MEDIUM, HIGH, CRITICAL, ROUTINE)

Includes automatic retry with 30-second backoff for rate limiting (429 errors).

## Notes

- The free tier of Google Gemini has daily request limits. If classification falls back to `ALL | ROUTINE`, your quota may be exhausted.
- The Caesar analysis API (`localhost:8080`) is optional — the local chi-squared fallback works without it.
