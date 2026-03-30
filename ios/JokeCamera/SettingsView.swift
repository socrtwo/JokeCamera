import SwiftUI

struct SettingsView: View {
    @ObservedObject var jokeManager: JokeManager
    @Environment(\.dismiss) private var dismiss

    @AppStorage("manualMode") private var manualMode = false
    @AppStorage("detectionEnabled") private var detectionEnabled = true
    @AppStorage("timerMode") private var timerMode = false
    @AppStorage("timerDelay") private var timerDelay = 3.0
    @AppStorage("detectionMode") private var detectionMode = 3
    @AppStorage("punchlineDelay") private var punchlineDelay = 0.81
    @AppStorage("nextJokeWait") private var nextJokeWait = 2.5

    var body: some View {
        NavigationView {
            List {
                // Categories
                Section("Joke Categories") {
                    CategoryToggle(label: "General", type: "general", jokeManager: jokeManager)
                    CategoryToggle(label: "Programming", type: "programming", jokeManager: jokeManager)
                    CategoryToggle(label: "Knock-Knock", type: "knock-knock", jokeManager: jokeManager)
                    CategoryToggle(label: "Dad Jokes", type: "dad", jokeManager: jokeManager)
                }

                // Mode
                Section("Mode") {
                    Toggle("Manual Joke Mode", isOn: $manualMode)
                    Toggle("Smile/Laugh Detection", isOn: $detectionEnabled)
                    Toggle("Timer Mode", isOn: $timerMode)

                    if timerMode {
                        VStack(alignment: .leading) {
                            Text("Timer Delay: \(timerDelay, specifier: "%.1f")s")
                            Slider(value: $timerDelay, in: 0.5...10.0, step: 0.1)
                        }
                    }
                }

                // Detection Mode
                if detectionEnabled {
                    Section("Detection Mode") {
                        Picker("Mode", selection: $detectionMode) {
                            Text("Smile Only").tag(0)
                            Text("Laugh Only").tag(1)
                            Text("Smile AND Laugh").tag(2)
                            Text("Smile OR Laugh").tag(3)
                        }
                        .pickerStyle(.inline)
                    }
                }

                // Timing
                Section("Timing") {
                    VStack(alignment: .leading) {
                        Text("Punchline Delay: \(punchlineDelay, specifier: "%.2f")s")
                        Slider(value: $punchlineDelay, in: 0...2, step: 0.01)
                    }
                    VStack(alignment: .leading) {
                        Text("Wait Before Next Joke: \(nextJokeWait, specifier: "%.1f")s")
                        Slider(value: $nextJokeWait, in: 0.5...10, step: 0.1)
                    }
                }

                // Stats
                Section("Joke Statistics") {
                    let remaining = jokeManager.getRemainingCount()
                    let total = jokeManager.getTotalCount()
                    Text("Jokes remaining: \(remaining) / \(total)")
                    Text("General: \(jokeManager.getCountByType("general"))")
                    Text("Programming: \(jokeManager.getCountByType("programming"))")
                    Text("Knock-Knock: \(jokeManager.getCountByType("knock-knock"))")
                    Text("Dad: \(jokeManager.getCountByType("dad"))")

                    Button("Reset All Jokes") {
                        jokeManager.resetToldJokes()
                    }
                }
            }
            .navigationTitle("Settings")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}

struct CategoryToggle: View {
    let label: String
    let type: String
    @ObservedObject var jokeManager: JokeManager

    var isEnabled: Binding<Bool> {
        Binding(
            get: { jokeManager.enabledCategories.contains(type) },
            set: { newValue in
                if newValue {
                    jokeManager.enabledCategories.insert(type)
                } else {
                    jokeManager.enabledCategories.remove(type)
                    // Ensure at least one category
                    if jokeManager.enabledCategories.isEmpty {
                        jokeManager.enabledCategories.insert("general")
                    }
                }
                jokeManager.saveCategories()
            }
        )
    }

    var body: some View {
        Toggle("\(label) (\(jokeManager.getCountByType(type)))", isOn: isEnabled)
    }
}
