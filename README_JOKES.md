# Joke Fetcher

This directory contains scripts to fetch jokes from the JokeAPI and remove duplicates.

## Available Scripts

### 1. Node.js Script (Recommended)
```bash
node fetch_jokes.js
```

### 2. Python Script
```bash
python3 fetch_jokes.py
```

### 3. Bash Script (requires `jq`)
```bash
chmod +x fetch_jokes.sh
./fetch_jokes.sh
```

## What the scripts do

1. **Fetch**: Makes 30 API calls to JokeAPI, each requesting 10 jokes (up to 300 total)
2. **Deduplicate**: Removes duplicate jokes based on their unique ID
3. **Save**: Outputs the unique jokes to `jokes.json`

## API Details

- **Endpoint**: `https://v2.jokeapi.dev/joke/Any`
- **Filters**: Excludes nsfw, religious, political, racist, sexist, and explicit jokes
- **Amount**: 10 jokes per request
- **Requests**: 30 requests with 2-second delays (completes in ~60 seconds)

## Output Format

The `jokes.json` file contains an array of joke objects with the following structure:

```json
[
  {
    "category": "Programming",
    "type": "single",
    "joke": "A SQL query goes into a bar...",
    "flags": {...},
    "id": 5,
    "safe": true,
    "lang": "en"
  },
  {
    "category": "Programming",
    "type": "twopart",
    "setup": "Why do programmers prefer dark mode?",
    "delivery": "Because light attracts bugs.",
    "flags": {...},
    "id": 10,
    "safe": true,
    "lang": "en"
  }
]
```

## Note

The current `jokes.json` file contains sample data. Run one of the scripts above with internet access to fetch real jokes from the API.
