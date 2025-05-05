import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;


/**
 * Predicts the next word in a sequence based on a mapping of words to lists of
 * WordProbability entries. Each list must have strictly ascending cumulative probabilities
 * ending at one. Predictions are made using weighted probabilities.
 */
public class WordPredictor {
    private final Random rng;
    private final Map<String, List<WordProbability>> probs;
    
    /**
     * Constructs a WordPredictor with the provided probability map and random number generator.
     * 
     * In probs, each key is a word and the value is a list of words that could possibly follow it.
     * Each word is stored along with its cumulative probability.
     * The cumulative probability is the probability in the range (0, 1.] that this word
     * or any of the preceding words in the list of possibilities should be chosen.
     * The probabilities must be in strictly ascending order and the final probability
     * must be 1.0. There can be no empty lists.
     * 
     * Example: 
     * { 
     *   the: [[cat, .1], [dog, .5], [lizard, 1.0]],
     *   cat: [[sat, .6], [ate, 1.0]]
     * }
     * In this example:
     * there is a 10% chance that "cat" follows "the" (.1)
     * there is a 40% chance that "dog" follows "the" (.5-.1=.4)
     * there is a 50% chance that "lizard" follows "the" (1.-.5=.5)
     *
     * there is a 40% chance "ate" follows ""cat" (1.-.6=.4)
     * there is a 60% chance "sat" follows "cat" (.6)
     *
     * Validates the map structure before initializing.
     *
     * @param probs a map where each key is a word and the value is a non empty list of WordProbability entries
     *              with strictly ascending cumulative probabilities ending at one
     * @param rng the random number generator to use for making predictions
     * @throws IllegalArgumentException if the probability map is empty, or malformed
     */
    public WordPredictor(Map<String, List<WordProbability>> probs, Random rng) {
        this.probs = Objects.requireNonNull(probs, "probability map must not be null");
        this.rng = Objects.requireNonNull(rng, "random number generator must not be null");
        validateMap();
    }

    // Sets the variables and throws an IllegalArgumentException if the probabilities are malformed
    // Creates a new RNG
    public WordPredictor(Map<String, List<WordProbability>> probs) {
        this(probs, new Random());
    }


    /**
     * Validates the internal probability map structure.
     *
     * Checks that the map is not or empty. For each entry, verifies the list is not null or empty,
     * that cumulative probabilities are strictly ascending, that each probability is greater than zero
     * and at most one, and that the final probability in each list equals one.
     *
     * @throws IllegalArgumentException if any of the validation rules are violated
     */
    private void validateMap() {
        if (probs.isEmpty()) {
            throw new IllegalArgumentException("Probability map must not be empty");
        }
        final double TOL = 1e-3;
        for (Map.Entry<String, List<WordProbability>> entry : probs.entrySet()) {
            String word = entry.getKey();
            List<WordProbability> list = entry.getValue();
            if (list.isEmpty()) {
                throw new IllegalArgumentException(
                    "Probability list for word '" + word + "' must not be empty");
            }
            double previous = 0.0;
            for (WordProbability wp : list) {
                double p = wp.cumulativeProbability();
                if (p <= previous) {
                    throw new IllegalArgumentException(
                        "Cumulative probabilities for word '" + word +
                        "' must be strictly ascending");
                }
                if (p <= 0.0 || p > 1.0 + TOL) {
                    throw new IllegalArgumentException(
                        "Cumulative probability for word '" + word +
                        "' must be > 0 and ≤ 1.0 (within tolerance) but was " + p);
                }
                previous = p;
            }
            if (Math.abs(previous - 1.0) > TOL) {
                throw new IllegalArgumentException(
                    "Final cumulative probability for word '" + word +
                    "' must be within ±0.001 of 1.0 but was " + previous);
            }
        }
        
    }

    /**
     * Predicts the next word in a sequence given the previous word.
     *
     * Picks a random value and finds the next word whose cumulative probability threshold
     * meets or exceeds that value.
     *
     * @param word the previous word in the sequence
     * @return the predicted next word
     */
    public String predict(String word) {
        // Pick a random threshhold
        double target = rng.nextDouble();
        List<WordProbability> listOfWords = probs.get(word);

        // set low and high pointers
        int low = 0;
        int high = listOfWords.size() - 1;

        // USE BS logic to return which string matches (low, mid, high)
        while (true) {
            // cumulative probability for mid word
            int mid = (low + high) / 2;
            WordProbability checkWord = listOfWords.get(mid);
            double checkProbability = checkWord.cumulativeProbability();

            // if mid works, and there is none before it OR if this word works and the word before it does not, return this word
            if (checkProbability >= target && (mid == 0 || listOfWords.get(mid - 1).cumulativeProbability() < target)) {
                // return that string
                return checkWord.word();
            } else if (checkProbability >= target) {
                high = mid - 1;
            } else if (checkProbability < target) {
                low = mid + 1;
            }
        }
    }



    // public String predict(String word) {
    //     // This version is much less efficient!!!

    //     // Pick a random threshhold
    //     double target = rng.nextDouble();
    //     List<WordProbability> listOfWords = probs.get(word);

    //     for (int i = 0; i < listOfWords.size(); i++) {
    //         if (listOfWords.get(i).cumulativeProbability() >= target) {
    //             return listOfWords.get(i).word();
    //         }
    //     }
    //     return null;
    // }
}