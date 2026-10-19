/**
 * JokeManager - Manages joke selection, tracking, and categories.
 * Mirrors the Android JokeManager behavior.
 */
class JokeManager {
    constructor() {
        this.jokes = [...ALL_JOKES];
        this.toldJokeIds = new Set(JSON.parse(localStorage.getItem('toldJokeIds') || '[]'));
        this.enabledCategories = JSON.parse(localStorage.getItem('enabledCategories') ||
            '["general","programming","knock-knock","dad"]');
        this.customJokes = JSON.parse(localStorage.getItem('customJokes') || '[]');
        window._jokeManager = this;
    }

    reloadJokes(jokes) {
        this.jokes = jokes;
    }

    getAllJokes() {
        return [...this.jokes, ...this.customJokes];
    }

    getFilteredJokes() {
        return this.getAllJokes().filter(j => this.enabledCategories.includes(j.type));
    }

    getNextJoke() {
        const filtered = this.getFilteredJokes();
        if (filtered.length === 0) {
            return { id: 0, type: 'general', setup: "Why did the chicken cross the road?", punchline: "To get to the other side!" };
        }

        let available = filtered.filter(j => !this.toldJokeIds.has(j.id));
        if (available.length === 0) {
            // Reset told jokes for filtered categories
            const filteredIds = new Set(filtered.map(j => j.id));
            for (const id of filteredIds) {
                this.toldJokeIds.delete(id);
            }
            this.saveToldJokes();
            available = filtered;
        }

        const joke = available[Math.floor(Math.random() * available.length)];
        this.toldJokeIds.add(joke.id);
        this.saveToldJokes();
        return joke;
    }

    getRemainingCount() {
        const filtered = this.getFilteredJokes();
        return filtered.filter(j => !this.toldJokeIds.has(j.id)).length;
    }

    getTotalCount() {
        return this.getFilteredJokes().length;
    }

    resetToldJokes() {
        this.toldJokeIds.clear();
        this.saveToldJokes();
    }

    setEnabledCategories(categories) {
        this.enabledCategories = categories;
        localStorage.setItem('enabledCategories', JSON.stringify(categories));
    }

    importJokes(jsonString, replace) {
        const imported = JSON.parse(jsonString);
        if (!Array.isArray(imported)) throw new Error('Expected JSON array');

        const newJokes = imported.map((j, i) => ({
            id: 100000 + i,
            type: j.type || 'custom',
            setup: j.setup,
            punchline: j.punchline
        }));

        if (replace) {
            this.customJokes = newJokes;
            this.jokes = [];
        } else {
            this.customJokes = newJokes;
        }
        localStorage.setItem('customJokes', JSON.stringify(this.customJokes));
        this.resetToldJokes();
        return newJokes.length;
    }

    exportJokesAsJson() {
        return JSON.stringify(this.getAllJokes().map(j => ({
            type: j.type, setup: j.setup, punchline: j.punchline
        })), null, 2);
    }

    getCountByType(type) {
        return this.getAllJokes().filter(j => j.type === type).length;
    }

    saveToldJokes() {
        localStorage.setItem('toldJokeIds', JSON.stringify([...this.toldJokeIds]));
    }
}
