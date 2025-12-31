const https = require('https');
const fs = require('fs');

const API_URL = 'https://v2.jokeapi.dev/joke/Any?blacklistFlags=nsfw,religious,political,racist,sexist,explicit&amount=10';
const NUM_REQUESTS = 30;
const OUTPUT_FILE = 'jokes.json';

async function fetchJokes() {
    const allJokes = [];
    console.log('============================================================');
    console.log('JokeAPI Fetcher');
    console.log('============================================================');
    console.log(`Fetching jokes from API ${NUM_REQUESTS} times...\n`);

    const startTime = Date.now();

    for (let i = 0; i < NUM_REQUESTS; i++) {
        try {
            const jokes = await makeRequest(i + 1);
            allJokes.push(...jokes);
            await sleep(2000); // 2 second delay between requests
        } catch (error) {
            console.log(`Request ${i + 1}/${NUM_REQUESTS}: Error - ${error.message}`);
        }
    }

    const elapsedTime = ((Date.now() - startTime) / 1000).toFixed(2);
    console.log(`\nCompleted in ${elapsedTime} seconds`);
    console.log(`Total jokes fetched: ${allJokes.length}`);

    return allJokes;
}

function makeRequest(requestNum) {
    return new Promise((resolve, reject) => {
        https.get(API_URL, (res) => {
            let data = '';

            res.on('data', (chunk) => {
                data += chunk;
            });

            res.on('end', () => {
                try {
                    const response = JSON.parse(data);

                    if (response.error) {
                        console.log(`Request ${requestNum}/${NUM_REQUESTS}: API returned an error`);
                        resolve([]);
                        return;
                    }

                    const jokes = response.jokes || [];
                    console.log(`Request ${requestNum}/${NUM_REQUESTS}: Fetched ${jokes.length} jokes`);
                    resolve(jokes);
                } catch (error) {
                    reject(error);
                }
            });
        }).on('error', (error) => {
            reject(error);
        });
    });
}

function deduplicateJokes(jokes) {
    console.log('\n============================================================');
    const seenIds = new Set();
    const uniqueJokes = [];

    for (const joke of jokes) {
        if (joke.id && !seenIds.has(joke.id)) {
            seenIds.add(joke.id);
            uniqueJokes.push(joke);
        }
    }

    console.log(`Duplicates removed: ${jokes.length - uniqueJokes.length}`);
    console.log(`Unique jokes: ${uniqueJokes.length}`);

    return uniqueJokes;
}

function saveJokes(jokes, filename) {
    console.log('============================================================');
    fs.writeFileSync(filename, JSON.stringify(jokes, null, 2));
    console.log(`Jokes saved to ${filename}`);
    console.log('============================================================');
    console.log('Done!');
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

async function main() {
    try {
        const allJokes = await fetchJokes();

        if (allJokes.length === 0) {
            console.log('No jokes were fetched!');
            return;
        }

        const uniqueJokes = deduplicateJokes(allJokes);
        saveJokes(uniqueJokes, OUTPUT_FILE);
    } catch (error) {
        console.error('Error:', error.message);
        process.exit(1);
    }
}

main();
