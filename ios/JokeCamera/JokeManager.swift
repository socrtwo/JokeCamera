import Foundation

struct Joke: Codable, Identifiable {
    let id: Int
    let type: String
    let setup: String
    let punchline: String
}

class JokeManager: ObservableObject {
    @Published private(set) var jokes: [Joke] = []
    @Published var enabledCategories: Set<String> = ["general", "programming", "knock-knock", "dad"]

    private var toldJokeIds: Set<Int> = []
    private var customJokes: [Joke] = []

    private let toldJokesKey = "toldJokeIds"
    private let customJokesKey = "customJokes"
    private let categoriesKey = "enabledCategories"

    init() {
        loadBuiltInJokes()
        loadToldJokes()
        loadCustomJokes()
        loadCategories()
    }

    private func loadBuiltInJokes() {
        guard let url = Bundle.main.url(forResource: "jokes", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let loaded = try? JSONDecoder().decode([Joke].self, from: data) else {
            // Fallback jokes
            jokes = [
                Joke(id: 1, type: "general", setup: "What did the fish say when it hit the wall?", punchline: "Dam."),
                Joke(id: 2, type: "general", setup: "How do you make a tissue dance?", punchline: "You put a little boogie on it."),
                Joke(id: 3, type: "programming", setup: "Why do Java programmers wear glasses?", punchline: "Because they don't C#.")
            ]
            return
        }
        jokes = loaded
    }

    var allJokes: [Joke] {
        jokes + customJokes
    }

    var filteredJokes: [Joke] {
        allJokes.filter { enabledCategories.contains($0.type) }
    }

    func getNextJoke() -> Joke {
        let filtered = filteredJokes
        guard !filtered.isEmpty else {
            return Joke(id: 0, type: "general", setup: "Why did the chicken cross the road?", punchline: "To get to the other side!")
        }

        var available = filtered.filter { !toldJokeIds.contains($0.id) }
        if available.isEmpty {
            let filteredIds = Set(filtered.map(\.id))
            toldJokeIds.subtract(filteredIds)
            saveToldJokes()
            available = filtered
        }

        let joke = available.randomElement()!
        toldJokeIds.insert(joke.id)
        saveToldJokes()
        return joke
    }

    func getRemainingCount() -> Int {
        filteredJokes.filter { !toldJokeIds.contains($0.id) }.count
    }

    func getTotalCount() -> Int {
        filteredJokes.count
    }

    func resetToldJokes() {
        toldJokeIds.removeAll()
        saveToldJokes()
    }

    func getCountByType(_ type: String) -> Int {
        allJokes.filter { $0.type == type }.count
    }

    func importJokes(jsonString: String, replace: Bool) throws -> Int {
        guard let data = jsonString.data(using: .utf8) else {
            throw NSError(domain: "JokeManager", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid string"])
        }

        struct ImportedJoke: Codable {
            let type: String
            let setup: String
            let punchline: String
        }

        let imported = try JSONDecoder().decode([ImportedJoke].self, from: data)
        customJokes = imported.enumerated().map { (i, j) in
            Joke(id: 100000 + i, type: j.type, setup: j.setup, punchline: j.punchline)
        }

        if replace {
            jokes = []
        }

        saveCustomJokes()
        resetToldJokes()
        return customJokes.count
    }

    func exportJokesAsJson() -> String {
        struct ExportJoke: Codable {
            let type: String
            let setup: String
            let punchline: String
        }

        let exportable = allJokes.map { ExportJoke(type: $0.type, setup: $0.setup, punchline: $0.punchline) }
        let encoder = JSONEncoder()
        encoder.outputFormatting = .prettyPrinted
        guard let data = try? encoder.encode(exportable) else { return "[]" }
        return String(data: data, encoding: .utf8) ?? "[]"
    }

    // MARK: - Persistence

    private func loadToldJokes() {
        let ids = UserDefaults.standard.array(forKey: toldJokesKey) as? [Int] ?? []
        toldJokeIds = Set(ids)
    }

    private func saveToldJokes() {
        UserDefaults.standard.set(Array(toldJokeIds), forKey: toldJokesKey)
    }

    private func loadCustomJokes() {
        guard let data = UserDefaults.standard.data(forKey: customJokesKey),
              let loaded = try? JSONDecoder().decode([Joke].self, from: data) else { return }
        customJokes = loaded
    }

    private func saveCustomJokes() {
        if let data = try? JSONEncoder().encode(customJokes) {
            UserDefaults.standard.set(data, forKey: customJokesKey)
        }
    }

    private func loadCategories() {
        if let cats = UserDefaults.standard.stringArray(forKey: categoriesKey) {
            enabledCategories = Set(cats)
        }
    }

    func saveCategories() {
        UserDefaults.standard.set(Array(enabledCategories), forKey: categoriesKey)
    }
}
