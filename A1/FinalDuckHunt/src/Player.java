import java.util.Arrays;
import java.util.Random;

class Player {


    public static final int TIME_PER_ROUND = 100;

    public int currentTime = -1;
    public int lastRound = -1;

    public int[] speciesGuess;
    public int[] deathTime;
    public int[][] observations;
    public boolean[] knownSpecies = new boolean[Constants.COUNT_SPECIES];
    public double[][] probOfNextEmission;
    public double[] trackAttemptedShots;

    public HMM[] speciesHmm = new HMM[Constants.COUNT_SPECIES];

    public Player() {
    }

    public Action shoot(GameState pState, Deadline pDue) {

        int numOfBirds = pState.getNumBirds();                      //number of birds in the round
        int shootTime = (TIME_PER_ROUND - numOfBirds * 2);          //time to start shooting
        probOfNextEmission = new double[numOfBirds][2];             //[for each bird][probability, emission]
        trackAttemptedShots = new double[numOfBirds]; //track birds we've attempted to shoot

        if (pState.getRound() != lastRound) {
            currentTime = -1;                                   //new round -> reset timer
            lastRound = pState.getRound();
            deathTime = new int[numOfBirds];
            speciesGuess = new int[numOfBirds];                 //initialise guess to UNKNOWN_SPECIES
            for (int birdIndex = 0; birdIndex < numOfBirds; birdIndex++) {
                speciesGuess[birdIndex] = -1;
            }
        }

        currentTime++;                                          //increment timer every time step

        int[][] currentObservation = new int[numOfBirds][currentTime + 1];

        for (int birdIndex = 0; birdIndex < numOfBirds; birdIndex++) {
            if (pState.getBird(birdIndex).isDead()) {
                if (deathTime[birdIndex] == 0) {
                    deathTime[birdIndex] = currentTime - 1;     //stores the time step a bird died
                }
            }
        }

        // Monitor the birds' emissions if alive
        for (int birdIndex = 0; birdIndex < numOfBirds; birdIndex++) {
            Bird bird = pState.getBird(birdIndex);
            if (bird.isAlive()) {
                // Update the observation matrix
                for (int i = 0; i < currentTime + 1; i++) {
                    currentObservation[birdIndex][i] = bird.getObservation(i);
                }
            }
        }
        observations = currentObservation;

        // Guess the bird's specie if alive
        for (int birdIndex = 0; birdIndex < numOfBirds; birdIndex++) {
            Bird bird = pState.getBird(birdIndex);
            if (bird.isAlive()) {
                speciesGuess[birdIndex] = guessBird(currentObservation[birdIndex]);
                // Check model has been estimated
                if(speciesHmm[speciesGuess[birdIndex]] == null){
                    continue;
                }
                if (currentTime >= shootTime) {
                    int[] statePath = speciesHmm[speciesGuess[birdIndex]].viterbi(currentObservation[birdIndex]);
                    int currentState = statePath[currentTime];
                    probOfNextEmission[birdIndex] = speciesHmm[speciesGuess[birdIndex]].getNextEmission(currentState);
                }
            }
        }

        // Shoot the bird that with highest probability
        if (currentTime >= shootTime) {
            int nextMove = 0;
            int birdToShoot = 0;
            double maxProb = 0.0;
            for (int birdIndex = 0; birdIndex < numOfBirds; birdIndex++) {
                if (pState.getBird(birdIndex).isAlive()) {
                    int birdSpecies = speciesGuess[birdIndex];
                    // Do not shoot if not sure of the species or if it is a BLACK_STORK
                    if (birdSpecies == Constants.SPECIES_UNKNOWN || birdSpecies == Constants.SPECIES_BLACK_STORK)
                        continue;
                        // Find most likely emission + bird
                    else if (probOfNextEmission[birdIndex][0] > maxProb && pState.getBird(birdIndex).isAlive()) {
                        maxProb = probOfNextEmission[birdIndex][0];
                        birdToShoot = birdIndex;
                        nextMove = (int) probOfNextEmission[birdIndex][1];
                    }
                }
            }

            if (maxProb > 0.6 && pState.getBird(birdToShoot).isAlive() && trackAttemptedShots[birdToShoot] < 2) {
                trackAttemptedShots[birdToShoot]++;
                // SHOOT!!!
                return new Action(birdToShoot, nextMove);
            }
        }
        return cDontShoot;
    }

    public int[] guess(GameState pState, Deadline pDue) {

        int numOfBirds = pState.getNumBirds();
        int[] sequence;

        // Guess Pigeon for the first round
        if (pState.getRound() == 0) {
            for (int i = 0; i < numOfBirds; ++i) {
                speciesGuess[i] = Constants.SPECIES_PIGEON;
            }
        }
        else {
            for (int birdIndex = 0; birdIndex < numOfBirds; birdIndex++) {
                Bird bird = pState.getBird(birdIndex);
                sequence = new int [bird.getSeqLength()];
                // Gather observation sequence
                for(int j = 0; j < bird.getSeqLength(); j++){
                    if(bird.wasAlive(j)){
                        sequence[j] = bird.getObservation(j);
                    }
                }
                // Guess the species
                speciesGuess[birdIndex] = guessBird(sequence);
            }
        }
        int[] lGuess = speciesGuess;
        return lGuess;
    }

    public void hit(GameState pState, int pBird, Deadline pDue) {
        System.err.println("HIT BIRD!!!");
    }

    public void reveal(GameState pState, int[] pSpecies, Deadline pDue) {
        int numOfBirds = pState.getNumBirds();

        for (int birdIndex = 0; birdIndex < numOfBirds; birdIndex++) {
            int specie = pSpecies[birdIndex];
            // If we haven't tried to guess the bird, skip it
            if (pSpecies[birdIndex] == -1) {
                continue;
            }
            // Initialise the specie model
            if (speciesHmm[specie] == null) {
                speciesHmm[specie] = new HMM();
                knownSpecies[specie] = true;
            }
            // If the bird is dead, only gather observations up until its death time
            if (pState.getBird(birdIndex).isDead()) {
                speciesHmm[specie].estimateModel(Arrays.copyOfRange(observations[birdIndex], 0, deathTime[birdIndex]));
            }
            else {
                speciesHmm[specie].estimateModel(observations[birdIndex]);
            }
        }
    }

    public int guessBird(int[] guessBirdObs) {

        double[] probability = new double[Constants.COUNT_SPECIES];
        double lowProbability = Double.NEGATIVE_INFINITY;

        // Compute and store each P(O|estimated model) for each species model
        for (int i = 0; i < Constants.COUNT_SPECIES; i++) {
            // Check if the species HMM has been estimated
            if (speciesHmm[i] == null) {
                // If there is no species model, set the probability low
                probability[i] = lowProbability;
                continue;
            }
            // Using the Alpha Pass compute P(O|estimated model)
            double logProb = speciesHmm[i].probOfObsSeq(guessBirdObs);
            probability[i] = logProb;
        }

        // Get the max probability -> most likely species
        double bestProbability = lowProbability;
        int guess = -1;
        for (int i = 0; i < Constants.COUNT_SPECIES; i++) {
            if (probability[i] > bestProbability) {
                guess = i;
                bestProbability = probability[i];
            }
        }
        // If the probability is high enough, return the guessed species
        if ( bestProbability > -300) {
            return guess;
        }
        // Else randomly select a bird we don't have a model for yet
        else {
            boolean boolAllGuessed = true;
            // Check if all of the species (except the stork) have been guessed
            for (int j = 0; j < Constants.COUNT_SPECIES - 1; j++) {
                if (knownSpecies[j] == false) {
                    boolAllGuessed = false;
                }
            }
            // Guess a random species which has not been revealed
            Random random = new Random();
            int randNum = random.nextInt(Constants.COUNT_SPECIES);
            int iter = 0;
            for (int i = randNum; iter < Constants.COUNT_SPECIES; i = (i + 1) % Constants.COUNT_SPECIES) {
                if (!knownSpecies[i] && (i != Constants.SPECIES_BLACK_STORK))
                // Check if species has not been revealed before you can guess it
                {
                    return i;
                } else if (boolAllGuessed) {
                    return Constants.SPECIES_PIGEON;
                }
                iter++;
            }
        }
        return Constants.SPECIES_UNKNOWN;
    }

    public final Action cDontShoot = new Action(-1, -1);
}