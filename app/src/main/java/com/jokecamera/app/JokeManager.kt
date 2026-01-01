package com.jokecamera.app

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.json.JSONArray
import org.json.JSONObject

/**
 * Enum representing different joke categories
 */
enum class JokeType {
    DAD,
    GENERAL,
    KNOCK_KNOCK,
    PROGRAMMING,
    CUSTOM;
    
    companion object {
        fun fromString(value: String): JokeType {
            return try {
                valueOf(value.uppercase().replace("-", "_").replace(" ", "_"))
            } catch (e: IllegalArgumentException) {
                CUSTOM
            }
        }
    }
    
    fun displayName(): String {
        return when (this) {
            DAD -> "Dad Jokes"
            GENERAL -> "General"
            KNOCK_KNOCK -> "Knock-Knock"
            PROGRAMMING -> "Programming"
            CUSTOM -> "Custom"
        }
    }
    
    fun toJsonString(): String {
        return when (this) {
            DAD -> "dad"
            GENERAL -> "general"
            KNOCK_KNOCK -> "knock-knock"
            PROGRAMMING -> "programming"
            CUSTOM -> "custom"
        }
    }
}

/**
 * Data class representing a joke with setup and punchline
 */
data class Joke(
    val id: Int,
    val type: JokeType,
    val setup: String,
    val punchline: String
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("type", type.toJsonString())
            put("setup", setup)
            put("punchline", punchline)
        }
    }
    
    companion object {
        fun fromJson(json: JSONObject, id: Int): Joke {
            return Joke(
                id = id,
                type = JokeType.fromString(json.optString("type", "general")),
                setup = json.getString("setup"),
                punchline = json.getString("punchline")
            )
        }
    }
}

/**
 * Manages the collection of jokes, tracks which have been told,
 * and ensures no repeats until all jokes have been used.
 * Supports custom jokes that can be imported from JSON files.
 */
class JokeManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val appPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val builtInJokes: List<Joke> = generateBuiltInJokes()
    private var customJokes: List<Joke> = loadCustomJokes()
    private val toldJokeIds: MutableSet<Int> = loadToldJokes()
    
    companion object {
        private const val PREFS_NAME = "joke_manager_prefs"
        private const val KEY_TOLD_JOKES = "told_jokes"
        private const val KEY_CUSTOM_JOKES = "custom_jokes"
        private const val KEY_REPLACED_BUILTIN = "replaced_builtin"
        private const val CUSTOM_ID_OFFSET = 100000
        
        // Category preference keys
        const val KEY_CATEGORY_DAD = "category_dad"
        const val KEY_CATEGORY_GENERAL = "category_general"
        const val KEY_CATEGORY_KNOCK_KNOCK = "category_knock_knock"
        const val KEY_CATEGORY_PROGRAMMING = "category_programming"
        const val KEY_CATEGORY_CUSTOM = "category_custom"
    }
    
    /**
     * Gets all available jokes (built-in + custom, or just custom if replaced)
     */
    private val allJokes: List<Joke>
        get() {
            return if (prefs.getBoolean(KEY_REPLACED_BUILTIN, false)) {
                customJokes
            } else {
                builtInJokes + customJokes
            }
        }
    
    /**
     * Gets the list of jokes filtered by enabled categories
     */
    private fun getFilteredJokes(): List<Joke> {
        val enabledTypes = getEnabledCategories()
        return if (enabledTypes.isEmpty()) {
            allJokes
        } else {
            allJokes.filter { it.type in enabledTypes }
        }
    }
    
    /**
     * Gets the set of enabled joke categories
     */
    fun getEnabledCategories(): Set<JokeType> {
        val enabled = mutableSetOf<JokeType>()
        if (appPrefs.getBoolean(KEY_CATEGORY_DAD, true)) enabled.add(JokeType.DAD)
        if (appPrefs.getBoolean(KEY_CATEGORY_GENERAL, true)) enabled.add(JokeType.GENERAL)
        if (appPrefs.getBoolean(KEY_CATEGORY_KNOCK_KNOCK, true)) enabled.add(JokeType.KNOCK_KNOCK)
        if (appPrefs.getBoolean(KEY_CATEGORY_PROGRAMMING, true)) enabled.add(JokeType.PROGRAMMING)
        if (appPrefs.getBoolean(KEY_CATEGORY_CUSTOM, true)) enabled.add(JokeType.CUSTOM)
        return enabled
    }
    
    /**
     * Gets the next joke that hasn't been told yet from enabled categories.
     */
    fun getNextJoke(): Joke {
        val jokes = getFilteredJokes()
        
        if (jokes.isEmpty()) {
            // Fallback if no jokes available
            return Joke(0, JokeType.GENERAL, "Why did the chicken cross the road?", "To get to the other side!")
        }
        
        val availableJokes = jokes.filter { it.id !in toldJokeIds }
        
        if (availableJokes.isEmpty()) {
            val filteredIds = jokes.map { it.id }.toSet()
            toldJokeIds.removeAll(filteredIds)
            saveToldJokes()
            return jokes.random().also { 
                toldJokeIds.add(it.id)
                saveToldJokes()
            }
        }
        
        val joke = availableJokes.random()
        toldJokeIds.add(joke.id)
        saveToldJokes()
        
        return joke
    }
    
    fun getJokeById(id: Int): Joke? = allJokes.find { it.id == id }
    
    fun getRemainingCount(): Int {
        val jokes = getFilteredJokes()
        return jokes.count { it.id !in toldJokeIds }
    }
    
    fun getTotalCount(): Int = getFilteredJokes().size
    
    fun getAllJokesCount(): Int = allJokes.size
    
    fun getBuiltInCount(): Int = builtInJokes.size
    
    fun getCustomCount(): Int = customJokes.size
    
    fun getCountByType(type: JokeType): Int = allJokes.count { it.type == type }
    
    fun isBuiltInReplaced(): Boolean = prefs.getBoolean(KEY_REPLACED_BUILTIN, false)
    
    fun resetToldJokes() {
        toldJokeIds.clear()
        saveToldJokes()
    }
    
    /**
     * Imports jokes from JSON string
     * @param jsonString JSON array of jokes
     * @param replaceExisting If true, replaces built-in jokes; if false, adds to them
     * @return Number of jokes imported
     */
    fun importJokes(jsonString: String, replaceExisting: Boolean): Int {
        try {
            val jsonArray = JSONArray(jsonString)
            val newJokes = mutableListOf<Joke>()
            
            for (i in 0 until jsonArray.length()) {
                val jokeJson = jsonArray.getJSONObject(i)
                val joke = Joke.fromJson(jokeJson, CUSTOM_ID_OFFSET + i)
                newJokes.add(joke)
            }
            
            if (replaceExisting) {
                prefs.edit().putBoolean(KEY_REPLACED_BUILTIN, true).apply()
            }
            
            customJokes = newJokes
            saveCustomJokes()
            resetToldJokes()
            
            return newJokes.size
        } catch (e: Exception) {
            throw Exception("Invalid JSON format: ${e.message}")
        }
    }
    
    /**
     * Clears all custom jokes and restores built-in jokes
     */
    fun clearCustomJokes() {
        customJokes = emptyList()
        prefs.edit()
            .remove(KEY_CUSTOM_JOKES)
            .putBoolean(KEY_REPLACED_BUILTIN, false)
            .apply()
        resetToldJokes()
    }
    
    /**
     * Exports all current jokes as JSON string
     */
    fun exportJokesAsJson(): String {
        val jsonArray = JSONArray()
        allJokes.forEach { joke ->
            jsonArray.put(joke.toJson())
        }
        return jsonArray.toString(2)
    }
    
    /**
     * Gets a template JSON showing the joke format
     */
    fun getTemplateJson(): String {
        return """[
  {
    "type": "general",
    "setup": "Why did the chicken cross the road?",
    "punchline": "To get to the other side!"
  },
  {
    "type": "programming",
    "setup": "Why do programmers prefer dark mode?",
    "punchline": "Because light attracts bugs!"
  },
  {
    "type": "dad",
    "setup": "I'm reading a book about anti-gravity.",
    "punchline": "It's impossible to put down!"
  },
  {
    "type": "knock-knock",
    "setup": "Knock knock.\nWho's there?\nBoo.\nBoo who?",
    "punchline": "Don't cry, it's just a joke!"
  }
]

INSTRUCTIONS:
-------------
1. Each joke needs three fields:
   - "type": One of "general", "programming", "dad", "knock-knock", or any custom type
   - "setup": The joke question or setup line
   - "punchline": The punchline or answer

2. Use \n for line breaks within setup or punchline

3. Make sure to use proper JSON formatting:
   - Strings must be in double quotes
   - Escape special characters (use \" for quotes, \\ for backslash)
   - Separate jokes with commas
   - The whole file should be a JSON array [ ... ]

4. Save the file with .json extension

5. Use the "Upload Jokes" button to import your jokes
"""
    }
    
    private fun loadToldJokes(): MutableSet<Int> {
        val stored = prefs.getStringSet(KEY_TOLD_JOKES, emptySet()) ?: emptySet()
        return stored.mapNotNull { it.toIntOrNull() }.toMutableSet()
    }
    
    private fun saveToldJokes() {
        prefs.edit()
            .putStringSet(KEY_TOLD_JOKES, toldJokeIds.map { it.toString() }.toSet())
            .apply()
    }
    
    private fun loadCustomJokes(): List<Joke> {
        val jsonString = prefs.getString(KEY_CUSTOM_JOKES, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val jokes = mutableListOf<Joke>()
            for (i in 0 until jsonArray.length()) {
                val jokeJson = jsonArray.getJSONObject(i)
                jokes.add(Joke.fromJson(jokeJson, CUSTOM_ID_OFFSET + i))
            }
            jokes
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveCustomJokes() {
        val jsonArray = JSONArray()
        customJokes.forEach { joke ->
            jsonArray.put(joke.toJson())
        }
        prefs.edit().putString(KEY_CUSTOM_JOKES, jsonArray.toString()).apply()
    }
    
    /**
     * Generates 451 built-in jokes
     */
    private fun generateBuiltInJokes(): List<Joke> {
        return listOf(
            Joke(1, JokeType.GENERAL, "What did the fish say when it hit the wall?", "Dam."),
            Joke(2, JokeType.GENERAL, "How do you make a tissue dance?", "You put a little boogie on it."),
            Joke(3, JokeType.GENERAL, "What's Forrest Gump's password?", "1Forrest1"),
            Joke(4, JokeType.GENERAL, "What do you call a belt made out of watches?", "A waist of time."),
            Joke(5, JokeType.GENERAL, "Why can't bicycles stand on their own?", "They are two tired"),
            Joke(6, JokeType.GENERAL, "How does a train eat?", "It goes chew, chew"),
            Joke(7, JokeType.GENERAL, "What do you call a singing Laptop?", "A Dell"),
            Joke(8, JokeType.GENERAL, "How many lips does a flower have?", "Tulips"),
            Joke(9, JokeType.GENERAL, "What kind of shoes does a thief wear?", "Sneakers"),
            Joke(10, JokeType.GENERAL, "What's the best time to go to the dentist?", "Tooth hurty."),
            Joke(11, JokeType.KNOCK_KNOCK, "Knock knock. \n Who's there? \n A broken pencil. \n A broken pencil who?", "Never mind. It's pointless."),
            Joke(12, JokeType.KNOCK_KNOCK, "Knock knock. \n Who's there? \n Cows go. \n Cows go who?", "No, cows go moo."),
            Joke(13, JokeType.KNOCK_KNOCK, "Knock knock. \n Who's there? \n Little old lady. \n Little old lady who?", "I didn't know you could yodel!"),
            Joke(14, JokeType.PROGRAMMING, "Why would a guitarist become a good programmer?", "He's adept at riffing in C#."),
            Joke(15, JokeType.PROGRAMMING, "What's the best thing about a Boolean?", "Even if you're wrong, you're only off by a bit."),
            Joke(16, JokeType.PROGRAMMING, "What's the object-oriented way to become wealthy?", "Inheritance"),
            Joke(17, JokeType.PROGRAMMING, "Where do programmers like to hangout?", "The Foo Bar."),
            Joke(18, JokeType.PROGRAMMING, "Why did the programmer quit his job?", "Because he didn't get arrays."),
            Joke(19, JokeType.GENERAL, "Did you hear about the two silk worms in a race?", "It ended in a tie."),
            Joke(20, JokeType.GENERAL, "What do you call a laughing motorcycle?", "A Yamahahahaha."),
            Joke(21, JokeType.GENERAL, "A termite walks into a bar and says...", "'Where is the bar tended?'"),
            Joke(22, JokeType.GENERAL, "What does C.S. Lewis keep at the back of his wardrobe?", "Narnia business!"),
            Joke(23, JokeType.PROGRAMMING, "A SQL query walks into a bar, walks up to two tables and asks...", "'Can I join you?'"),
            Joke(24, JokeType.PROGRAMMING, "How many programmers does it take to change a lightbulb?", "None that's a hardware problem"),
            Joke(25, JokeType.PROGRAMMING, "If you put a million monkeys at a million keyboards, one of them will eventually write a Java program", "the rest of them will write Perl"),
            Joke(26, JokeType.PROGRAMMING, "['hip', 'hip']", "(hip hip array)"),
            Joke(27, JokeType.PROGRAMMING, "To understand what recursion is...", "You must first understand what recursion is"),
            Joke(28, JokeType.PROGRAMMING, "There are 10 types of people in this world...", "Those who understand binary and those who don't"),
            Joke(29, JokeType.GENERAL, "What did the duck say when he bought lipstick?", "Put it on my bill"),
            Joke(30, JokeType.GENERAL, "What happens to a frog's car when it breaks down?", "It gets toad away"),
            Joke(31, JokeType.GENERAL, "did you know the first French fries weren't cooked in France?", "they were cooked in Greece"),
            Joke(32, JokeType.PROGRAMMING, "Which song would an exception sing?", "Can't catch me - Avicii"),
            Joke(33, JokeType.KNOCK_KNOCK, "Knock knock. \n Who's there? \n Opportunity.", "That is impossible. Opportunity doesn’t come knocking twice!"),
            Joke(34, JokeType.PROGRAMMING, "Why do Java programmers wear glasses?", "Because they don't C#."),
            Joke(35, JokeType.GENERAL, "Why did the mushroom get invited to the party?", "Because he was a fungi."),
            Joke(36, JokeType.GENERAL, "Do you know what the word 'was' was initially?", "Before was was was was was is."),
            Joke(37, JokeType.GENERAL, "I'm reading a book about anti-gravity...", "It's impossible to put down"),
            Joke(38, JokeType.GENERAL, "If you're American when you go into the bathroom, and American when you come out, what are you when you're in there?", "European"),
            Joke(39, JokeType.GENERAL, "Want to hear a joke about a piece of paper?", "Never mind...it's tearable"),
            Joke(40, JokeType.GENERAL, "I just watched a documentary about beavers.", "It was the best dam show I ever saw"),
            Joke(41, JokeType.GENERAL, "If you see a robbery at an Apple Store...", "Does that make you an iWitness?"),
            Joke(42, JokeType.GENERAL, "A ham sandwhich walks into a bar and orders a beer. The bartender says...", "I'm sorry, we don't serve food here"),
            Joke(43, JokeType.GENERAL, "Why did the Clydesdale give the pony a glass of water?", "Because he was a little horse"),
            Joke(44, JokeType.GENERAL, "If you boil a clown...", "Do you get a laughing stock?"),
            Joke(45, JokeType.GENERAL, "Finally realized why my plant sits around doing nothing all day...", "He loves his pot."),
            Joke(46, JokeType.GENERAL, "Don't look at the eclipse through a colander.", "You'll strain your eyes."),
            Joke(47, JokeType.GENERAL, "I bought some shoes from a drug dealer.", "I don't know what he laced them with, but I was tripping all day!"),
            Joke(48, JokeType.GENERAL, "Why do chicken coops only have two doors?", "Because if they had four, they would be chicken sedans"),
            Joke(49, JokeType.GENERAL, "What do you call a factory that sells passable products?", "A satisfactory"),
            Joke(50, JokeType.GENERAL, "When a dad drives past a graveyard: Did you know that's a popular cemetery?", "Yep, people are just dying to get in there"),
            Joke(51, JokeType.GENERAL, "Why did the invisible man turn down the job offer?", "He couldn't see himself doing it"),
            Joke(52, JokeType.GENERAL, "How do you make holy water?", "You boil the hell out of it"),
            Joke(53, JokeType.GENERAL, "I had a dream that I was a muffler last night.", "I woke up exhausted!"),
            Joke(54, JokeType.GENERAL, "Why is peter pan always flying?", "Because he neverlands"),
            Joke(55, JokeType.PROGRAMMING, "How do you check if a webpage is HTML5?", "Try it out on Internet Explorer"),
            Joke(56, JokeType.GENERAL, "What do you call a cow with no legs?", "Ground beef!"),
            Joke(57, JokeType.GENERAL, "I dropped a pear in my car this morning.", "You should drop another one, then you would have a pair."),
            Joke(58, JokeType.PROGRAMMING, "Lady: How do I spread love in this cruel world?", "Random Dude: [...💘]"),
            Joke(59, JokeType.PROGRAMMING, "A user interface is like a joke.", "If you have to explain it then it is not that good."),
            Joke(60, JokeType.KNOCK_KNOCK, "Knock knock. \n Who's there? \n Hatch. \n Hatch who?", "Bless you!"),
            Joke(61, JokeType.GENERAL, "What do you call sad coffee?", "Despresso."),
            Joke(62, JokeType.GENERAL, "Why did the butcher work extra hours at the shop?", "To make ends meat."),
            Joke(63, JokeType.GENERAL, "Did you hear about the hungry clock?", "It went back four seconds."),
            Joke(64, JokeType.GENERAL, "Well...", "That’s a deep subject."),
            Joke(65, JokeType.GENERAL, "Did you hear the story about the cheese that saved the world?", "It was legend dairy."),
            Joke(66, JokeType.GENERAL, "Did you watch the new comic book movie?", "It was very graphic!"),
            Joke(67, JokeType.GENERAL, "I started a new business making yachts in my attic this year...", "The sails are going through the roof."),
            Joke(68, JokeType.GENERAL, "I got hit in the head by a soda can, but it didn't hurt that much...", "It was a soft drink."),
            Joke(69, JokeType.GENERAL, "I can't tell if i like this blender...", "It keeps giving me mixed results."),
            Joke(70, JokeType.GENERAL, "I couldn't get a reservation at the library...", "They were fully booked."),
            Joke(71, JokeType.PROGRAMMING, "I was gonna tell you a joke about UDP...", "...but you might not get it."),
            Joke(72, JokeType.PROGRAMMING, "The punchline often arrives before the set-up.", "Do you know the problem with UDP jokes?"),
            Joke(73, JokeType.PROGRAMMING, "Why do C# and Java developers keep breaking their keyboards?", "Because they use a strongly typed language."),
            Joke(74, JokeType.GENERAL, "What do you give to a lemon in need?", "Lemonaid."),
            Joke(75, JokeType.GENERAL, "Never take advice from electrons.", "They are always negative."),
            Joke(76, JokeType.GENERAL, "Hey, dad, did you get a haircut?", "No, I got them all cut."),
            Joke(77, JokeType.GENERAL, "What time is it?", "I don't know... it keeps changing."),
            Joke(78, JokeType.GENERAL, "A weasel walks into a bar. The bartender says, \"Wow, I've never served a weasel before. What can I get for you?\"", "Pop,goes the weasel."),
            Joke(79, JokeType.GENERAL, "Bad at golf?", "Join the club."),
            Joke(80, JokeType.GENERAL, "Can a kangaroo jump higher than the Empire State Building?", "Of course. The Empire State Building can't jump."),
            Joke(81, JokeType.GENERAL, "Can February march?", "No, but April may."),
            Joke(82, JokeType.GENERAL, "Can I watch the TV?", "Yes, but don’t turn it on."),
            Joke(83, JokeType.GENERAL, "Dad, can you put my shoes on?", "I don't think they'll fit me."),
            Joke(84, JokeType.GENERAL, "Did you hear about the bread factory burning down?", "They say the business is toast."),
            Joke(85, JokeType.GENERAL, "Did you hear about the chameleon who couldn't change color?", "They had a reptile dysfunction."),
            Joke(86, JokeType.GENERAL, "Did you hear about the cheese factory that exploded in France?", "There was nothing left but de Brie."),
            Joke(87, JokeType.GENERAL, "Did you hear about the cow who jumped over the barbed wire fence?", "It was udder destruction."),
            Joke(88, JokeType.GENERAL, "Did you hear about the guy who invented Lifesavers?", "They say he made a mint."),
            Joke(89, JokeType.GENERAL, "Did you hear about the guy whose whole left side was cut off?", "He's all right now."),
            Joke(90, JokeType.GENERAL, "Did you hear about the kidnapping at school?", "It's ok, he woke up."),
            Joke(91, JokeType.GENERAL, "Did you hear about the Mexican train killer?", "He had loco motives"),
            Joke(92, JokeType.GENERAL, "Did you hear about the new restaurant on the moon?", "The food is great, but there’s just no atmosphere."),
            Joke(93, JokeType.GENERAL, "Did you hear about the runner who was criticized?", "He just took it in stride"),
            Joke(94, JokeType.GENERAL, "Did you hear about the scientist who was lab partners with a pot of boiling water?", "He had a very esteemed colleague."),
            Joke(95, JokeType.GENERAL, "Did you hear about the submarine industry?", "It really took a dive..."),
            Joke(96, JokeType.GENERAL, "Did you hear that David lost his ID in prague?", "Now we just have to call him Dav."),
            Joke(97, JokeType.GENERAL, "Did you hear that the police have a warrant out on a midget psychic ripping people off?", "It reads \"Small medium at large.\""),
            Joke(98, JokeType.GENERAL, "Did you hear the joke about the wandering nun?", "She was a roman catholic."),
            Joke(99, JokeType.GENERAL, "Did you hear the news?", "FedEx and UPS are merging. They’re going to go by the name Fed-Up from now on."),
            Joke(100, JokeType.GENERAL, "Did you hear the one about the guy with the broken hearing aid?", "Neither did he."),
            Joke(101, JokeType.GENERAL, "Did you know crocodiles could grow up to 15 feet?", "But most just have 4."),
            Joke(102, JokeType.GENERAL, "What do ghosts call their true love?", "Their ghoul-friend"),
            Joke(103, JokeType.GENERAL, "Did you know that protons have mass?", "I didn't even know they were catholic."),
            Joke(104, JokeType.GENERAL, "Did you know you should always take an extra pair of pants golfing?", "Just in case you get a hole in one."),
            Joke(105, JokeType.GENERAL, "Do I enjoy making courthouse puns?", "Guilty"),
            Joke(106, JokeType.GENERAL, "Do you know where you can get chicken broth in bulk?", "The stock market."),
            Joke(107, JokeType.GENERAL, "Do you want a brief explanation of what an acorn is?", "In a nutshell, it's an oak tree."),
            Joke(108, JokeType.GENERAL, "Ever wondered why bees hum?", "It's because they don't know the words."),
            Joke(109, JokeType.GENERAL, "Have you ever heard of a music group called Cellophane?", "They mostly wrap."),
            Joke(110, JokeType.GENERAL, "Have you heard of the band 1023MB?", "They haven't got a gig yet."),
            Joke(111, JokeType.GENERAL, "Have you heard the rumor going around about butter?", "Never mind, I shouldn't spread it."),
            Joke(112, JokeType.GENERAL, "How are false teeth like stars?", "They come out at night!"),
            Joke(113, JokeType.GENERAL, "How can you tell a vampire has a cold?", "They start coffin."),
            Joke(114, JokeType.GENERAL, "How come a man driving a train got struck by lightning?", "He was a good conductor."),
            Joke(115, JokeType.GENERAL, "How come the stadium got hot after the game?", "Because all of the fans left."),
            Joke(116, JokeType.GENERAL, "How did Darth Vader know what Luke was getting for Christmas?", "He felt his presents."),
            Joke(117, JokeType.GENERAL, "How did the hipster burn the roof of his mouth?", "He ate the pizza before it was cool."),
            Joke(118, JokeType.GENERAL, "How do hens stay fit?", "They always egg-cercise!"),
            Joke(119, JokeType.GENERAL, "How do locomotives know where they're going?", "Lots of training"),
            Joke(120, JokeType.GENERAL, "How do the trees get on the internet?", "They log on."),
            Joke(121, JokeType.GENERAL, "How do you find Will Smith in the snow?", "Look for fresh prints."),
            Joke(122, JokeType.GENERAL, "How do you fix a broken pizza?", "With tomato paste."),
            Joke(123, JokeType.GENERAL, "How do you fix a damaged jack-o-lantern?", "You use a pumpkin patch."),
            Joke(124, JokeType.GENERAL, "How do you get a baby alien to sleep?", "You rocket."),
            Joke(125, JokeType.GENERAL, "How do you get two whales in a car?", "Start in England and drive West."),
            Joke(126, JokeType.GENERAL, "How do you know if there’s an elephant under your bed?", "Your head hits the ceiling!"),
            Joke(127, JokeType.GENERAL, "How do you make a hankie dance?", "Put a little boogie in it."),
            Joke(128, JokeType.GENERAL, "How good are you at Power Point?", "I Excel at it."),
            Joke(129, JokeType.GENERAL, "How do you organize a space party?", "You planet."),
            Joke(130, JokeType.GENERAL, "How do you steal a coat?", "You jacket."),
            Joke(131, JokeType.GENERAL, "How do you tell the difference between a crocodile and an alligator?", "You will see one later and one in a while."),
            Joke(132, JokeType.GENERAL, "How does a dyslexic poet write?", "Inverse."),
            Joke(133, JokeType.GENERAL, "How does a French skeleton say hello?", "Bone-jour."),
            Joke(134, JokeType.GENERAL, "How does a penguin build it’s house?", "Igloos it together."),
            Joke(135, JokeType.GENERAL, "How does a scientist freshen their breath?", "With experi-mints!"),
            Joke(136, JokeType.GENERAL, "How does the moon cut his hair?", "Eclipse it."),
            Joke(137, JokeType.GENERAL, "How many apples grow on a tree?", "All of them!"),
            Joke(138, JokeType.GENERAL, "How many bones are in the human hand?", "A handful of them."),
            Joke(139, JokeType.GENERAL, "How many hipsters does it take to change a lightbulb?", "Oh, it's a really obscure number. You've probably never heard of it."),
            Joke(140, JokeType.GENERAL, "How many kids with ADD does it take to change a lightbulb?", "Let's go ride bikes!"),
            Joke(141, JokeType.GENERAL, "How many optometrists does it take to change a light bulb?", "1 or 2? 1... or 2?"),
            Joke(142, JokeType.GENERAL, "How many seconds are in a year?", "12. January 2nd, February 2nd, March 2nd, April 2nd.... etc"),
            Joke(143, JokeType.GENERAL, "How many tickles does it take to tickle an octopus?", "Ten-tickles!"),
            Joke(144, JokeType.GENERAL, "How much does a hipster weigh?", "An instagram."),
            Joke(145, JokeType.GENERAL, "How was the snow globe feeling after the storm?", "A little shaken."),
            Joke(146, JokeType.GENERAL, "Is the pool safe for diving?", "It deep ends."),
            Joke(147, JokeType.GENERAL, "Is there a hole in your shoe?", "No… Then how’d you get your foot in it?"),
            Joke(148, JokeType.GENERAL, "What did the spaghetti say to the other spaghetti?", "Pasta la vista, baby!"),
            Joke(149, JokeType.GENERAL, "What’s 50 Cent’s name in Zimbabwe?", "200 Dollars."),
            Joke(150, JokeType.GENERAL, "Want to hear a chimney joke?", "Got stacks of em! First one's on the house"),
            Joke(151, JokeType.GENERAL, "Want to hear a joke about construction?", "Nah, I'm still working on it."),
            Joke(152, JokeType.GENERAL, "Want to hear my pizza joke?", "Never mind, it's too cheesy."),
            Joke(153, JokeType.GENERAL, "What animal is always at a game of cricket?", "A bat."),
            Joke(154, JokeType.GENERAL, "What are the strongest days of the week?", "Saturday and Sunday...the rest are weekdays."),
            Joke(155, JokeType.GENERAL, "What biscuit does a short person like?", "Shortbread. "),
            Joke(156, JokeType.GENERAL, "What cheese can never be yours?", "Nacho cheese."),
            Joke(157, JokeType.GENERAL, "What creature is smarter than a talking parrot?", "A spelling bee."),
            Joke(158, JokeType.GENERAL, "What did celery say when he broke up with his girlfriend?", "She wasn't right for me, so I really don't carrot all."),
            Joke(159, JokeType.GENERAL, "What did Michael Jackson name his denim store?", "Billy Jeans!"),
            Joke(160, JokeType.GENERAL, "What did one nut say as he chased another nut?", "I'm a cashew!"),
            Joke(161, JokeType.GENERAL, "What did one plate say to the other plate?", "Dinner is on me!"),
            Joke(162, JokeType.GENERAL, "What did one snowman say to the other snow man?", "Do you smell carrot?"),
            Joke(163, JokeType.GENERAL, "What did one wall say to the other wall?", "I'll meet you at the corner!"),
            Joke(164, JokeType.GENERAL, "What did Romans use to cut pizza before the rolling cutter was invented?", "Lil Caesars"),
            Joke(165, JokeType.GENERAL, "What did the 0 say to the 8?", "Nice belt."),
            Joke(166, JokeType.GENERAL, "What did the beaver say to the tree?", "It's been nice gnawing you."),
            Joke(167, JokeType.GENERAL, "What did the big flower say to the littler flower?", "Hi, bud!"),
            Joke(168, JokeType.GENERAL, "What did the Buffalo say to his little boy when he dropped him off at school?", "Bison."),
            Joke(169, JokeType.GENERAL, "What did the digital clock say to the grandfather clock?", "Look, no hands!"),
            Joke(170, JokeType.GENERAL, "What did the dog say to the two trees?", "Bark bark."),
            Joke(171, JokeType.GENERAL, "What did the Dorito farmer say to the other Dorito farmer?", "Cool Ranch!"),
            Joke(172, JokeType.GENERAL, "What did the fish say when it swam into a wall?", "Damn!"),
            Joke(173, JokeType.GENERAL, "What did the grape do when he got stepped on?", "He let out a little wine."),
            Joke(174, JokeType.GENERAL, "What did the judge say to the dentist?", "Do you swear to pull the tooth, the whole tooth and nothing but the tooth?"),
            Joke(175, JokeType.GENERAL, "What did the late tomato say to the early tomato?", "I’ll ketch up"),
            Joke(176, JokeType.GENERAL, "What did the left eye say to the right eye?", "Between us, something smells!"),
            Joke(177, JokeType.GENERAL, "What did the mountain climber name his son?", "Cliff."),
            Joke(178, JokeType.GENERAL, "What did the ocean say to the beach?", "Thanks for all the sediment."),
            Joke(179, JokeType.GENERAL, "What did the ocean say to the shore?", "Nothing, it just waved."),
            Joke(180, JokeType.GENERAL, "Why don't you find hippopotamuses hiding in trees?", "They're really good at it."),
            Joke(181, JokeType.GENERAL, "What did the pirate say on his 80th birthday?", "Aye Matey!"),
            Joke(182, JokeType.GENERAL, "What did the Red light say to the Green light?", "Don't look at me I'm changing!"),
            Joke(183, JokeType.GENERAL, "What did the scarf say to the hat?", "You go on ahead, I am going to hang around a bit longer."),
            Joke(184, JokeType.GENERAL, "What did the shy pebble wish for?", "That she was a little boulder."),
            Joke(185, JokeType.GENERAL, "What did the traffic light say to the car as it passed?", "Don't look I'm changing!"),
            Joke(186, JokeType.GENERAL, "What did the Zen Buddist say to the hotdog vendor?", "Make me one with everything."),
            Joke(187, JokeType.GENERAL, "What do birds give out on Halloween?", "Tweets."),
            Joke(188, JokeType.GENERAL, "What do I look like?", "A JOKE MACHINE!?"),
            Joke(189, JokeType.GENERAL, "What do prisoners use to call each other?", "Cell phones."),
            Joke(190, JokeType.GENERAL, "What do vegetarian zombies eat?", "Grrrrrainnnnnssss."),
            Joke(191, JokeType.GENERAL, "What do you call a bear with no teeth?", "A gummy bear!"),
            Joke(192, JokeType.GENERAL, "What do you call a bee that lives in America?", "A USB."),
            Joke(193, JokeType.GENERAL, "What do you call a boomerang that won't come back?", "A stick."),
            Joke(194, JokeType.GENERAL, "What do you call a careful wolf?", "Aware wolf."),
            Joke(195, JokeType.GENERAL, "What do you call a cow on a trampoline?", "A milk shake!"),
            Joke(196, JokeType.GENERAL, "What do you call a cow with two legs?", "Lean beef."),
            Joke(197, JokeType.GENERAL, "What do you call a crowd of chess players bragging about their wins in a hotel lobby?", "Chess nuts boasting in an open foyer."),
            Joke(198, JokeType.GENERAL, "What do you call a dad that has fallen through the ice?", "A Popsicle."),
            Joke(199, JokeType.GENERAL, "What do you call a dictionary on drugs?", "High definition."),
            Joke(200, JokeType.GENERAL, "what do you call a dog that can do magic tricks?", "a labracadabrador"),
            Joke(201, JokeType.GENERAL, "What do you call a droid that takes the long way around?", "R2 detour."),
            Joke(202, JokeType.GENERAL, "What do you call a duck that gets all A's?", "A wise quacker."),
            Joke(203, JokeType.GENERAL, "What do you call a fake noodle?", "An impasta."),
            Joke(204, JokeType.GENERAL, "What do you call a fashionable lawn statue with an excellent sense of rhythmn?", "A metro-gnome"),
            Joke(205, JokeType.GENERAL, "What do you call a fat psychic?", "A four-chin teller."),
            Joke(206, JokeType.GENERAL, "What do you call a fly without wings?", "A walk."),
            Joke(207, JokeType.GENERAL, "What do you call a girl between two posts?", "Annette."),
            Joke(208, JokeType.GENERAL, "What do you call a group of disorganized cats?", "A cat-tastrophe."),
            Joke(209, JokeType.GENERAL, "What do you call a group of killer whales playing instruments?", "An Orca-stra."),
            Joke(210, JokeType.GENERAL, "What do you call a monkey in a mine field?", "A babooooom!"),
            Joke(211, JokeType.GENERAL, "What do you call a nervous javelin thrower?", "Shakespeare."),
            Joke(212, JokeType.GENERAL, "What do you call a pig that knows karate?", "A pork chop!"),
            Joke(213, JokeType.GENERAL, "What do you call a pig with three eyes?", "Piiig"),
            Joke(214, JokeType.GENERAL, "What do you call a pile of cats?", "A Meowtain."),
            Joke(215, JokeType.GENERAL, "What do you call a sheep with no legs?", "A cloud."),
            Joke(216, JokeType.GENERAL, "What do you call a troublesome Canadian high schooler?", "A poutine."),
            Joke(217, JokeType.GENERAL, "What do you call an alligator in a vest?", "An in-vest-igator!"),
            Joke(218, JokeType.GENERAL, "What do you call an Argentinian with a rubber toe?", "Roberto"),
            Joke(219, JokeType.GENERAL, "What do you call an eagle who can play the piano?", "Talonted!"),
            Joke(220, JokeType.GENERAL, "What do you call an elephant that doesn’t matter?", "An irrelephant."),
            Joke(221, JokeType.GENERAL, "What do you call an old snowman?", "Water."),
            Joke(222, JokeType.GENERAL, "What do you call cheese by itself?", "Provolone."),
            Joke(223, JokeType.GENERAL, "What do you call corn that joins the army?", "Kernel."),
            Joke(224, JokeType.GENERAL, "What do you call someone with no nose?", "Nobody knows."),
            Joke(225, JokeType.GENERAL, "What do you call two barracuda fish?", "A Pairacuda!"),
            Joke(226, JokeType.GENERAL, "What do you do on a remote island?", "Try and find the TV island it belongs to."),
            Joke(227, JokeType.GENERAL, "What do you do when you see a space man?", "Park your car, man."),
            Joke(228, JokeType.GENERAL, "What do you get hanging from Apple trees?", "Sore arms."),
            Joke(229, JokeType.GENERAL, "What do you get when you cross a bee and a sheep?", "A bah-humbug."),
            Joke(230, JokeType.GENERAL, "What do you get when you cross a chicken with a skunk?", "A fowl smell!"),
            Joke(231, JokeType.GENERAL, "What do you get when you cross a rabbit with a water hose?", "Hare spray."),
            Joke(232, JokeType.GENERAL, "What do you get when you cross a snowman with a vampire?", "Frostbite."),
            Joke(233, JokeType.GENERAL, "What do you give a sick lemon?", "Lemonaid."),
            Joke(234, JokeType.GENERAL, "What does a clock do when it's hungry?", "It goes back four seconds!"),
            Joke(235, JokeType.GENERAL, "What does a pirate pay for his corn?", "A buccaneer!"),
            Joke(236, JokeType.GENERAL, "What does an angry pepper do?", "It gets jalapeño face."),
            Joke(237, JokeType.GENERAL, "What happens when you anger a brain surgeon?", "They will give you a piece of your mind."),
            Joke(238, JokeType.GENERAL, "What has ears but cannot hear?", "A field of corn."),
            Joke(239, JokeType.GENERAL, "What is a centipedes's favorite Beatle song?", "I want to hold your hand, hand, hand, hand..."),
            Joke(240, JokeType.GENERAL, "What is a tornado's favorite game to play?", "Twister!"),
            Joke(241, JokeType.GENERAL, "What is a vampire's favorite fruit?", "A blood orange."),
            Joke(242, JokeType.GENERAL, "What is a witch's favorite subject in school?", "Spelling!"),
            Joke(243, JokeType.GENERAL, "What is red and smells like blue paint?", "Red paint!"),
            Joke(244, JokeType.GENERAL, "What is the difference between ignorance and apathy?", "I don't know and I don't care."),
            Joke(245, JokeType.GENERAL, "What is the hardest part about sky diving?", "The ground."),
            Joke(246, JokeType.GENERAL, "What is the leading cause of dry skin?", "Towels"),
            Joke(247, JokeType.GENERAL, "What is the least spoken language in the world?", "Sign Language"),
            Joke(248, JokeType.GENERAL, "What is the tallest building in the world?", "The library, it’s got the most stories!"),
            Joke(249, JokeType.GENERAL, "What is this movie about?", "It is about 2 hours long."),
            Joke(250, JokeType.GENERAL, "What kind of award did the dentist receive?", "A little plaque."),
            Joke(251, JokeType.GENERAL, "What kind of bagel can fly?", "A plain bagel."),
            Joke(252, JokeType.GENERAL, "What kind of dinosaur loves to sleep?", "A stega-snore-us."),
            Joke(253, JokeType.GENERAL, "What kind of dog lives in a particle accelerator?", "A Fermilabrador Retriever."),
            Joke(254, JokeType.GENERAL, "What kind of magic do cows believe in?", "MOODOO."),
            Joke(255, JokeType.GENERAL, "What kind of music do planets listen to?", "Nep-tunes."),
            Joke(256, JokeType.GENERAL, "What kind of pants do ghosts wear?", "Boo jeans."),
            Joke(257, JokeType.GENERAL, "What kind of tree fits in your hand?", "A palm tree!"),
            Joke(258, JokeType.GENERAL, "What lies at the bottom of the ocean and twitches?", "A nervous wreck."),
            Joke(259, JokeType.GENERAL, "What musical instrument is found in the bathroom?", "A tuba toothpaste."),
            Joke(260, JokeType.GENERAL, "What time did the man go to the dentist?", "Tooth hurt-y."),
            Joke(261, JokeType.GENERAL, "What type of music do balloons hate?", "Pop music!"),
            Joke(262, JokeType.GENERAL, "What was a more important invention than the first telephone?", "The second one."),
            Joke(263, JokeType.GENERAL, "What was the pumpkin’s favorite sport?", "Squash."),
            Joke(264, JokeType.GENERAL, "What's black and white and read all over?", "The newspaper."),
            Joke(265, JokeType.GENERAL, "What's blue and not very heavy?", "Light blue."),
            Joke(266, JokeType.GENERAL, "What's brown and sticky?", "A stick."),
            Joke(267, JokeType.GENERAL, "What's orange and sounds like a parrot?", "A Carrot."),
            Joke(268, JokeType.GENERAL, "What's red and bad for your teeth?", "A Brick."),
            Joke(269, JokeType.GENERAL, "What's the best thing about elevator jokes?", "They work on so many levels."),
            Joke(270, JokeType.GENERAL, "What's the difference between a guitar and a fish?", "You can tune a guitar but you can't \"tuna\"fish!"),
            Joke(271, JokeType.GENERAL, "What's the difference between a hippo and a zippo?", "One is really heavy, the other is a little lighter."),
            Joke(272, JokeType.GENERAL, "What's the difference between a seal and a sea lion?", "An ion! "),
            Joke(273, JokeType.GENERAL, "What's the worst part about being a cross-eyed teacher?", "They can't control their pupils."),
            Joke(274, JokeType.GENERAL, "What's the worst thing about ancient history class?", "The teachers tend to Babylon."),
            Joke(275, JokeType.GENERAL, "What’s brown and sounds like a bell?", "Dung!"),
            Joke(276, JokeType.GENERAL, "What’s E.T. short for?", "He’s only got little legs."),
            Joke(277, JokeType.GENERAL, "What’s Forest Gump’s Facebook password?", "1forest1"),
            Joke(278, JokeType.GENERAL, "What’s the advantage of living in Switzerland?", "Well, the flag is a big plus."),
            Joke(279, JokeType.GENERAL, "What’s the difference between an African elephant and an Indian elephant?", "About 5000 miles."),
            Joke(280, JokeType.GENERAL, "When do doctors get angry?", "When they run out of patients."),
            Joke(281, JokeType.GENERAL, "When does a joke become a dad joke?", "When it becomes apparent."),
            Joke(282, JokeType.GENERAL, "When is a door not a door?", "When it's ajar."),
            Joke(283, JokeType.GENERAL, "Where did you learn to make ice cream?", "Sunday school."),
            Joke(284, JokeType.GENERAL, "Where do bees go to the bathroom?", "The BP station."),
            Joke(285, JokeType.GENERAL, "Where do hamburgers go to dance?", "The meat-ball."),
            Joke(286, JokeType.GENERAL, "Where do rabbits go after they get married?", "On a bunny-moon."),
            Joke(287, JokeType.GENERAL, "Where do sheep go to get their hair cut?", "The baa-baa shop."),
            Joke(288, JokeType.GENERAL, "Where do you learn to make banana splits?", "At sundae school."),
            Joke(289, JokeType.GENERAL, "Where do young cows eat lunch?", "In the calf-ateria."),
            Joke(290, JokeType.GENERAL, "Where does batman go to the bathroom?", "The batroom."),
            Joke(291, JokeType.GENERAL, "Where does Fonzie like to go for lunch?", "Chick-Fil-Eyyyyyyyy."),
            Joke(292, JokeType.GENERAL, "Where does Napoleon keep his armies?", "In his sleevies."),
            Joke(293, JokeType.GENERAL, "Where was the Declaration of Independence signed?", "At the bottom! "),
            Joke(294, JokeType.GENERAL, "Where’s the bin?", "I haven’t been anywhere!"),
            Joke(295, JokeType.GENERAL, "Which side of the chicken has more feathers?", "The outside."),
            Joke(296, JokeType.GENERAL, "Who did the wizard marry?", "His ghoul-friend"),
            Joke(297, JokeType.GENERAL, "Who is the coolest Doctor in the hospital?", "The hip Doctor!"),
            Joke(298, JokeType.GENERAL, "Why are fish easy to weigh?", "Because they have their own scales."),
            Joke(299, JokeType.GENERAL, "Why are fish so smart?", "Because they live in schools!"),
            Joke(300, JokeType.GENERAL, "Why are ghosts bad liars?", "Because you can see right through them!"),
            Joke(301, JokeType.GENERAL, "Why are graveyards so noisy?", "Because of all the coffin."),
            Joke(302, JokeType.GENERAL, "Why are mummys scared of vacation?", "They're afraid to unwind."),
            Joke(303, JokeType.GENERAL, "Why are oranges the smartest fruit?", "Because they are made to concentrate. "),
            Joke(304, JokeType.GENERAL, "Why are pirates called pirates?", "Because they arrr!"),
            Joke(305, JokeType.GENERAL, "Why are skeletons so calm?", "Because nothing gets under their skin."),
            Joke(306, JokeType.GENERAL, "Why can't a bicycle stand on its own?", "It's two-tired."),
            Joke(307, JokeType.GENERAL, "Why can't you use \"Beef stew\"as a password?", "Because it's not stroganoff."),
            Joke(308, JokeType.GENERAL, "Why can't your nose be 12 inches long?", "Because then it'd be a foot!"),
            Joke(309, JokeType.GENERAL, "Why can’t you hear a pterodactyl go to the bathroom?", "The p is silent."),
            Joke(310, JokeType.GENERAL, "Why couldn't the kid see the pirate movie?", "Because it was rated arrr!"),
            Joke(311, JokeType.GENERAL, "Why couldn't the lifeguard save the hippie?", "He was too far out, man."),
            Joke(312, JokeType.GENERAL, "Why did Dracula lie in the wrong coffin?", "He made a grave mistake."),
            Joke(313, JokeType.GENERAL, "Why did Sweden start painting barcodes on the sides of their battleships?", "So they could Scandinavian."),
            Joke(314, JokeType.GENERAL, "Why did the A go to the bathroom and come out as an E?", "Because he had a vowel movement."),
            Joke(315, JokeType.GENERAL, "Why did the barber win the race?", "He took a short cut."),
            Joke(316, JokeType.GENERAL, "Why did the belt go to prison?", "He held up a pair of pants!"),
            Joke(317, JokeType.GENERAL, "Why did the burglar hang his mugshot on the wall?", "To prove that he was framed!"),
            Joke(318, JokeType.GENERAL, "Why did the chicken get a penalty?", "For fowl play."),
            Joke(319, JokeType.GENERAL, "Why did the coffee file a police report?", "It got mugged."),
            Joke(320, JokeType.GENERAL, "Why did the cookie cry?", "Because his mother was a wafer so long"),
            Joke(321, JokeType.GENERAL, "Why did the cookie cry?", "It was feeling crumby."),
            Joke(322, JokeType.GENERAL, "Why did the cowboy have a weiner dog?", "Somebody told him to get a long little doggy."),
            Joke(323, JokeType.GENERAL, "Why did the fireman wear red, white, and blue suspenders?", "To hold his pants up."),
            Joke(324, JokeType.GENERAL, "Why did the girl smear peanut butter on the road?", "To go with the traffic jam."),
            Joke(325, JokeType.GENERAL, "Why did the half blind man fall in the well?", "Because he couldn't see that well!"),
            Joke(326, JokeType.GENERAL, "Why did the house go to the doctor?", "It was having window panes."),
            Joke(327, JokeType.GENERAL, "Why did the kid cross the playground?", "To get to the other slide."),
            Joke(328, JokeType.GENERAL, "Why did the man put his money in the freezer?", "He wanted cold hard cash!"),
            Joke(329, JokeType.GENERAL, "Why did the man run around his bed?", "Because he was trying to catch up on his sleep!"),
            Joke(330, JokeType.GENERAL, "Why did the melons plan a big wedding?", "Because they cantaloupe!"),
            Joke(331, JokeType.GENERAL, "Why did the octopus beat the shark in a fight?", "Because it was well armed."),
            Joke(332, JokeType.GENERAL, "Why did the opera singer go sailing?", "They wanted to hit the high Cs."),
            Joke(333, JokeType.GENERAL, "Why did the scarecrow win an award?", "Because he was outstanding in his field."),
            Joke(334, JokeType.GENERAL, "Why did the tomato blush?", "Because it saw the salad dressing."),
            Joke(335, JokeType.GENERAL, "Why did the tree go to the dentist?", "It needed a root canal."),
            Joke(336, JokeType.GENERAL, "Why did the worker get fired from the orange juice factory?", "Lack of concentration."),
            Joke(337, JokeType.GENERAL, "Why didn't the number 4 get into the nightclub?", "Because he is 2 square."),
            Joke(338, JokeType.GENERAL, "Why didn’t the orange win the race?", "It ran out of juice."),
            Joke(339, JokeType.GENERAL, "Why didn’t the skeleton cross the road?", "Because he had no guts."),
            Joke(340, JokeType.GENERAL, "Why do bananas have to put on sunscreen before they go to the beach?", "Because they might peel!"),
            Joke(341, JokeType.GENERAL, "Why do bears have hairy coats?", "Fur protection."),
            Joke(342, JokeType.GENERAL, "Why do bees have sticky hair?", "Because they use honey combs!"),
            Joke(343, JokeType.GENERAL, "Why do bees hum?", "Because they don't know the words."),
            Joke(344, JokeType.GENERAL, "Why do birds fly south for the winter?", "Because it's too far to walk."),
            Joke(345, JokeType.GENERAL, "Why do choirs keep buckets handy?", "So they can carry their tune"),
            Joke(346, JokeType.GENERAL, "Why do crabs never give to charity?", "Because they’re shellfish."),
            Joke(347, JokeType.GENERAL, "Why do ducks make great detectives?", "They always quack the case."),
            Joke(348, JokeType.GENERAL, "Why do mathematicians hate the U.S.?", "Because it's indivisible."),
            Joke(349, JokeType.GENERAL, "Why do pirates not know the alphabet?", "They always get stuck at \"C\"."),
            Joke(350, JokeType.GENERAL, "Why do pumpkins sit on people’s porches?", "They have no hands to knock on the door."),
            Joke(351, JokeType.GENERAL, "Why do scuba divers fall backwards into the water?", "Because if they fell forwards they’d still be in the boat."),
            Joke(352, JokeType.GENERAL, "Why do trees seem suspicious on sunny days?", "Dunno, they're just a bit shady."),
            Joke(353, JokeType.GENERAL, "Why do valley girls hang out in odd numbered groups?", "Because they can't even."),
            Joke(354, JokeType.GENERAL, "Why do wizards clean their teeth three times a day?", "To prevent bat breath!"),
            Joke(355, JokeType.GENERAL, "Why do you never see elephants hiding in trees?", "Because they're so good at it."),
            Joke(356, JokeType.GENERAL, "Why does a chicken coop only have two doors?", "Because if it had four doors it would be a chicken sedan."),
            Joke(357, JokeType.GENERAL, "Why does a Moon-rock taste better than an Earth-rock?", "Because it's a little meteor."),
            Joke(358, JokeType.GENERAL, "Why does it take longer to get from 1st to 2nd base, than it does to get from 2nd to 3rd base?", "Because there’s a Shortstop in between!"),
            Joke(359, JokeType.GENERAL, "Why does Norway have barcodes on their battleships?", "So when they get back to port, they can Scandinavian."),
            Joke(360, JokeType.GENERAL, "Why does Superman get invited to dinners?", "Because he is a Supperhero."),
            Joke(361, JokeType.GENERAL, "Why does Waldo only wear stripes?", "Because he doesn't want to be spotted."),
            Joke(362, JokeType.PROGRAMMING, "Knock-knock.", "A race condition. Who is there?"),
            Joke(363, JokeType.PROGRAMMING, "What's the best part about TCP jokes?", "I get to keep telling them until you get them."),
            Joke(364, JokeType.PROGRAMMING, "A programmer puts two glasses on his bedside table before going to sleep.", "A full one, in case he gets thirsty, and an empty one, in case he doesn’t."),
            Joke(365, JokeType.GENERAL, "Two guys walk into a bar . . .", "The first guy says \"Ouch!\" and the second says \"Yeah, I didn't see it either.\""),
            Joke(366, JokeType.PROGRAMMING, "What did the router say to the doctor?", "It hurts when IP."),
            Joke(367, JokeType.PROGRAMMING, "An IPv6 packet is walking out of the house.", "He goes nowhere."),
            Joke(368, JokeType.PROGRAMMING, "A DHCP packet walks into a bar and asks for a beer.", "Bartender says, \"here, but I’ll need that back in an hour!\""),
            Joke(369, JokeType.PROGRAMMING, "3 SQL statements walk into a NoSQL bar. Soon, they walk out", "They couldn't find a table."),
            Joke(370, JokeType.GENERAL, "I saw a nice stereo on Craigslist for \$1. Seller says the volume is stuck on ‘high’", "I couldn’t turn it down."),
            Joke(371, JokeType.PROGRAMMING, "What’s the object-oriented way to become wealthy?", "Inheritance."),
            Joke(372, JokeType.GENERAL, "What do you call a bee that can't make up its mind?", "A maybe."),
            Joke(373, JokeType.GENERAL, "Why was Cinderalla thrown out of the football team?", "Because she ran away from the ball."),
            Joke(374, JokeType.GENERAL, "What kind of music do welders like?", "Heavy metal."),
            Joke(375, JokeType.GENERAL, "Why are “Dad Jokes” so good?", "Because the punchline is apparent."),
            Joke(376, JokeType.PROGRAMMING, "Why dot net developers don't wear glasses?", "Because they see sharp."),
            Joke(377, JokeType.GENERAL, "Why is seven bigger than nine?", "Because seven ate nine."),
            Joke(378, JokeType.DAD, "Why do fathers take an extra pair of socks when they go golfing?", "In case they get a hole in one!"),
            Joke(379, JokeType.GENERAL, "What do you call a suspicious looking laptop?", "Asus"),
            Joke(380, JokeType.PROGRAMMING, "What did the Java code say to the C code?", "You've got no class."),
            Joke(381, JokeType.PROGRAMMING, "What is the most used language in programming?", "Profanity."),
            Joke(382, JokeType.PROGRAMMING, "Why do programmers always get Christmas and Halloween mixed up?", "Because DEC 25 = OCT 31"),
            Joke(383, JokeType.PROGRAMMING, "What goes after USA?", "USB."),
            Joke(384, JokeType.DAD, "Why don't eggs tell jokes?", "Because they would crack each other up."),
            Joke(385, JokeType.GENERAL, "How do you make the number one disappear?", "Add the letter G and it’s “gone”!"),
            Joke(386, JokeType.GENERAL, "My older brother always tore the last pages of my comic books, and never told me why.", "I had to draw my own conclusions."),
            Joke(387, JokeType.GENERAL, "The Sergeant-Major growled at the young soldier: I didn’t see you at camouflage training this morning.", "Thank you very much, sir."),
            Joke(388, JokeType.GENERAL, "Why does Waldo only wear stripes?", "Because he doesn't want to be spotted."),
            Joke(389, JokeType.GENERAL, "Why did the kid throw the watch out the window?", "So time would fly."),
            Joke(390, JokeType.PROGRAMMING, "Where did the API go to eat?", "To the RESTaurant."),
            Joke(391, JokeType.GENERAL, "Why did the rooster cross the road?", "He heard that the chickens at KFC were pretty hot."),
            Joke(392, JokeType.GENERAL, "Did you hear about the Viking who was reincarnated?", "He was Bjorn again"),
            Joke(393, JokeType.GENERAL, "What does the mermaid wear to math class?", "Algae-bra."),
            Joke(394, JokeType.GENERAL, "Did you hear about the crime in the parking garage?", "It was wrong on so many levels."),
            Joke(395, JokeType.PROGRAMMING, "Hey, wanna hear a joke?", "Parsing HTML with regex."),
            Joke(396, JokeType.GENERAL, "Why didn't the skeleton go for prom?", "Because it had nobody."),
            Joke(397, JokeType.GENERAL, "A grocery store cashier asked if I would like my milk in a bag.", "I told her 'No, thanks. The carton works fine.'"),
            Joke(398, JokeType.GENERAL, "99.9% of the people are dumb!", "Fortunately I belong to the remaining 1%"),
            Joke(399, JokeType.PROGRAMMING, "I just got fired from my job at the keyboard factory.", "They told me I wasn't putting in enough shifts."),
            Joke(400, JokeType.GENERAL, "You see, mountains aren't just funny.", "They are hill areas."),
            Joke(401, JokeType.GENERAL, "What do elves post on Social Media?", "Elf-ies."),
            Joke(402, JokeType.GENERAL, "While I was sleeping my friends decided to write math equations on me.", "You should have seen the expression on my face when I woke up."),
            Joke(403, JokeType.GENERAL, "Due to complaints, Hawaii passed a law where you're not allowed to laugh above a certain decibel.", "You can only use a low ha."),
            Joke(404, JokeType.GENERAL, "Why are football stadiums so cool?", "Because every seat has a fan in it."),
            Joke(405, JokeType.PROGRAMMING, "How do you generate a random string?", "Put a Windows user in front of Vim and tell them to exit."),
            Joke(406, JokeType.PROGRAMMING, "Why did the functions stop calling each other?", "Because they had constant arguments."),
            Joke(407, JokeType.PROGRAMMING, "Why did the private classes break up?", "Because they never saw each other."),
            Joke(408, JokeType.PROGRAMMING, "Why did the developer quit his job?", "Because he didn't get arrays."),
            Joke(409, JokeType.PROGRAMMING, "What's the best thing about a Boolean?", "Even if you're wrong, you're only off by a bit."),
            Joke(410, JokeType.PROGRAMMING, "How many React developers does it take to change a lightbulb?", "None, they prefer dark mode."),
            Joke(411, JokeType.PROGRAMMING, "Why don't React developers like nature?", "They prefer the virtual DOM."),
            Joke(412, JokeType.PROGRAMMING, "What do you get when you cross a React developer with a mathematician?", "A function component."),
            Joke(413, JokeType.PROGRAMMING, "Why did the developer go broke buying Bitcoin?", "He kept calling it bytecoin and didn't get any."),
            Joke(414, JokeType.PROGRAMMING, "Why did the programmer go to art school?", "He wanted to learn how to code outside the box."),
            Joke(415, JokeType.PROGRAMMING, "Why did the programmer's wife leave him?", "He didn't know how to commit."),
            Joke(416, JokeType.PROGRAMMING, "Why do programmers prefer dark chocolate?", "Because it's bitter like their code."),
            Joke(417, JokeType.PROGRAMMING, "Why did the programmer go broke?", "He used up all his cache"),
            Joke(418, JokeType.PROGRAMMING, "Why did the programmer always mix up Halloween and Christmas?", "Because Oct 31 equals Dec 25."),
            Joke(419, JokeType.PROGRAMMING, "Why don't programmers like nature?", "There's too many bugs."),
            Joke(420, JokeType.PROGRAMMING, "Why was the JavaScript developer sad?", "He didn't know how to null his feelings."),
            Joke(421, JokeType.GENERAL, "Why couldn't the bicycle stand up by itself?", "It was two-tired."),
            Joke(422, JokeType.GENERAL, "Why did the math book look sad?", "Because it had too many problems."),
            Joke(423, JokeType.GENERAL, "What's a computer's favorite snack?", "Microchips."),
            Joke(424, JokeType.GENERAL, "What did the janitor say when he jumped out of the closet?", "Supplies!"),
            Joke(425, JokeType.GENERAL, "What did one ocean say to the other ocean?", "Nothing, they just waved."),
            Joke(426, JokeType.GENERAL, "What's the best thing about Switzerland?", "I don't know, but their flag is a big plus."),
            Joke(427, JokeType.GENERAL, "Why did the golfer bring two pairs of pants?", "In case he got a hole in one."),
            Joke(428, JokeType.GENERAL, "Why did the chicken cross the playground?", "To get to the other slide."),
            Joke(429, JokeType.GENERAL, "Why don't scientists trust atoms?", "Because they make up everything."),
            Joke(430, JokeType.GENERAL, "Why did the scarecrow win an award?", "Because he was outstanding in his field."),
            Joke(431, JokeType.GENERAL, "Why did the coffee file a police report?", "It got mugged."),
            Joke(432, JokeType.GENERAL, "Why don't oysters give to charity?", "Because they're shellfish."),
            Joke(433, JokeType.GENERAL, "Why did the golfer wear two pairs of pants?", "In case he got a hole in one."),
            Joke(434, JokeType.GENERAL, "Why did the cookie go to the doctor?", " Because it was feeling crumbly."),
            Joke(435, JokeType.PROGRAMMING, "What do you call a computer mouse that swears a lot?", "A cursor!"),
            Joke(436, JokeType.PROGRAMMING, "Why did the designer break up with their font?", "Because it wasn't their type."),
            Joke(437, JokeType.PROGRAMMING, "Why did the programmer quit their job?", "They didn't get arrays."),
            Joke(438, JokeType.PROGRAMMING, "Why did the developer go broke?", "They kept spending all their cache."),
            Joke(439, JokeType.PROGRAMMING, "How do you comfort a designer?", "You give them some space... between the elements."),
            Joke(440, JokeType.PROGRAMMING, "Why don't programmers like nature?", "Too many bugs."),
            Joke(441, JokeType.PROGRAMMING, "Why did the programmer bring a ladder to work?", "They heard the code needed to be debugged from a higher level."),
            Joke(442, JokeType.GENERAL, "Why was the developer always calm?", "Because they knew how to handle exceptions."),
            Joke(443, JokeType.PROGRAMMING, "Why was the font always tired?", "It was always bold."),
            Joke(444, JokeType.PROGRAMMING, "Why did the developer go to therapy?", "They had too many unresolved issues."),
            Joke(445, JokeType.PROGRAMMING, "Why was the designer always cold?", "Because they always used too much ice-olation."),
            Joke(446, JokeType.PROGRAMMING, "Why did the programmer bring a broom to work?", "To clean up all the bugs."),
            Joke(447, JokeType.PROGRAMMING, "Why did the developer break up with their keyboard?", "It just wasn't their type anymore."),
            Joke(448, JokeType.PROGRAMMING, "Why did the programmer always carry a pencil?", "They preferred to write in C#."),
            Joke(449, JokeType.GENERAL, "Why don't skeletons fight each other?", "They don't have the guts."),
            Joke(450, JokeType.GENERAL, "What do you call fake spaghetti?", "An impasta."),
            Joke(451, JokeType.GENERAL, "What do you call a thieving alligator?", "A crookodile!")
        )
    }
}
