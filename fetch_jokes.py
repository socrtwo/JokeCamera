#!/usr/bin/env python3
import json
import urllib.request
import urllib.error
import time
from typing import List, Dict, Any

API_URL = "https://v2.jokeapi.dev/joke/Any?blacklistFlags=nsfw,religious,political,racist,sexist,explicit&amount=10"
NUM_REQUESTS = 30
OUTPUT_FILE = "jokes.json"

def fetch_jokes() -> List[Dict[str, Any]]:
    """Fetch jokes from the API 30 times in a minute."""
    all_jokes = []
    start_time = time.time()

    print(f"Fetching jokes from API {NUM_REQUESTS} times...")

    for i in range(NUM_REQUESTS):
        try:
            with urllib.request.urlopen(API_URL, timeout=10) as response:
                data = json.loads(response.read().decode())

                if data.get("error"):
                    print(f"Request {i+1}/{NUM_REQUESTS}: API returned an error")
                    continue

                jokes = data.get("jokes", [])
                all_jokes.extend(jokes)
                print(f"Request {i+1}/{NUM_REQUESTS}: Fetched {len(jokes)} jokes")

        except urllib.error.URLError as e:
            print(f"Request {i+1}/{NUM_REQUESTS}: Network error - {e}")
        except Exception as e:
            print(f"Request {i+1}/{NUM_REQUESTS}: Error - {e}")

        # Small delay to distribute requests over the minute
        if i < NUM_REQUESTS - 1:
            time.sleep(2)  # 60 seconds / 30 requests = 2 seconds

    elapsed_time = time.time() - start_time
    print(f"\nCompleted in {elapsed_time:.2f} seconds")
    print(f"Total jokes fetched: {len(all_jokes)}")

    return all_jokes

def deduplicate_jokes(jokes: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """Remove duplicate jokes based on ID."""
    seen_ids = set()
    unique_jokes = []

    for joke in jokes:
        joke_id = joke.get("id")
        if joke_id and joke_id not in seen_ids:
            seen_ids.add(joke_id)
            unique_jokes.append(joke)

    print(f"Duplicates removed: {len(jokes) - len(unique_jokes)}")
    print(f"Unique jokes: {len(unique_jokes)}")

    return unique_jokes

def save_jokes(jokes: List[Dict[str, Any]], filename: str):
    """Save jokes to a JSON file."""
    with open(filename, 'w', encoding='utf-8') as f:
        json.dump(jokes, f, indent=2, ensure_ascii=False)
    print(f"\nJokes saved to {filename}")

def main():
    print("=" * 60)
    print("JokeAPI Fetcher")
    print("=" * 60)

    # Fetch jokes
    all_jokes = fetch_jokes()

    if not all_jokes:
        print("No jokes were fetched!")
        return

    # Deduplicate
    print("\n" + "=" * 60)
    unique_jokes = deduplicate_jokes(all_jokes)

    # Save to file
    print("=" * 60)
    save_jokes(unique_jokes, OUTPUT_FILE)
    print("=" * 60)
    print("Done!")

if __name__ == "__main__":
    main()
