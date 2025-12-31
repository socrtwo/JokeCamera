#!/bin/bash

API_URL="https://v2.jokeapi.dev/joke/Any?blacklistFlags=nsfw,religious,political,racist,sexist,explicit&amount=10"
NUM_REQUESTS=30
OUTPUT_FILE="jokes.json"
TEMP_FILE="temp_jokes.json"

echo "============================================================"
echo "JokeAPI Fetcher"
echo "============================================================"
echo "Fetching jokes from API $NUM_REQUESTS times..."
echo

# Initialize empty array
echo "[]" > "$TEMP_FILE"

# Fetch jokes
for i in $(seq 1 $NUM_REQUESTS); do
    echo -n "Request $i/$NUM_REQUESTS: "

    response=$(curl -s -w "\n%{http_code}" "$API_URL")
    http_code=$(echo "$response" | tail -n1)
    json_data=$(echo "$response" | sed '$d')

    if [ "$http_code" -eq 200 ]; then
        # Extract jokes array and append
        jokes=$(echo "$json_data" | jq -c '.jokes[]' 2>/dev/null)
        if [ -n "$jokes" ]; then
            count=$(echo "$jokes" | wc -l)
            echo "Fetched $count jokes"
            # Append to temp file
            echo "$jokes" >> "$TEMP_FILE.tmp"
        else
            echo "No jokes in response"
        fi
    else
        echo "Error (HTTP $http_code)"
    fi

    # Small delay to distribute requests
    if [ $i -lt $NUM_REQUESTS ]; then
        sleep 2
    fi
done

echo
echo "============================================================"
echo "Processing and deduplicating jokes..."

# Combine all jokes, deduplicate by ID, and format as JSON array
if [ -f "$TEMP_FILE.tmp" ]; then
    cat "$TEMP_FILE.tmp" | \
        jq -s 'unique_by(.id)' > "$OUTPUT_FILE"

    total_jokes=$(cat "$TEMP_FILE.tmp" | wc -l)
    unique_jokes=$(cat "$OUTPUT_FILE" | jq 'length')
    duplicates=$((total_jokes - unique_jokes))

    echo "Total jokes fetched: $total_jokes"
    echo "Duplicates removed: $duplicates"
    echo "Unique jokes: $unique_jokes"

    rm "$TEMP_FILE.tmp"
else
    echo "[]" > "$OUTPUT_FILE"
    echo "No jokes were fetched!"
fi

rm "$TEMP_FILE"

echo "============================================================"
echo "Jokes saved to $OUTPUT_FILE"
echo "============================================================"
echo "Done!"
