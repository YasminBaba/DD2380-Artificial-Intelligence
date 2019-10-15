import java.util.*;

public class Player {

    //Code references:
    // http://www.cs.columbia.edu/~devans/TIC/AB.html?fbclid=IwAR3QihZLu3r54GPKOPlSi64ILj74kOroRwpaZiFMdB2NgI2zT2aF225Sy_s
    // http://etheses.dur.ac.uk/7770/1/Masters_Thesis_Final.pdf?DDD10%20&fbclid=IwAR3ZBevTBsy5InqMklQ5gE3UKt-hJ5EZDGvJRYvDWv2rXRk2O-EqtVhPk0g

    private HashMap<String, Integer> hashMap;

    private int myColour;
    private int otherColour;

    private int[] kingValues = {
            8,  8,  8,  8,
            7,  7,  7,  7,
            6,  6,  6,  6,
            5,  5,  5,  5,
            4,  4,  4,  4,
            3,  3,  3,  3,
            2,  2,  2,  2,
            1,  1,  1,  1
    };

    int redScoreBoard[] = {
            4,   3,   2,   3,
            4,   2,   3,   2,
            4,   2,   3,   4,
            6,   5,   4,   5,
            4,   5,   6,   6,
            6,   5,   6,   6,
            7,   7,   8,   8,
            10,  9,  10,   10
    };

    int whiteScoreBoard[] = {
            10,   10,   9,   10,
            8,    8,    7,   7,
            6,    6,   5,    6,
            6,    6,    5,   4,
            5,    4,   5,    6,
            4,    3,    2,   4,
            2,    3,   2,    4,
            3,    2,    3,   4
    };

    public Player(){
        hashMap = new HashMap<String, Integer>();
    }

    /**
     * Performs a move
     *
     * @param pState
     *            the current state of the board
     * @param pDue
     *            time before which we must have returned
     * @return the next state the board is in after our move
     */

    public GameState play(final GameState pState, final Deadline pDue) {

        Vector<GameState> nextStates = new Vector<GameState>();
        pState.findPossibleMoves(nextStates);

        if (nextStates.size() == 0) {

            return new GameState(pState, new Move());
        }

        int maxDepth = 10;
        int[] score = new int[nextStates.size()];
        int bestMove = 0;

        //Find out max and min players
        myColour = pState.getNextPlayer();
        if(myColour == Constants.CELL_RED){
            otherColour = Constants.CELL_WHITE;
        }
        else{
            otherColour = Constants.CELL_RED;
        }

        //Iterative Deepening Search
        for(int depth = 0; depth < maxDepth; depth++) {

            int alpha = Integer.MIN_VALUE;
            int beta = Integer.MAX_VALUE;
            int bestScore = Integer.MIN_VALUE;

            for (int i = 0; i < nextStates.size(); i++) {

                int value = alphaBeta(nextStates.get(i), depth, alpha, beta, otherColour);

                //Move Ordering
                score[i] = value;
                nextStates = sortStatesAscending(score, nextStates);

                if (value > bestScore) {
                    bestScore = value;
                    bestMove = i;
                }
                alpha = Math.max(value, alpha);
            }
        }

        return nextStates.elementAt(bestMove);
    }

    private Vector<GameState> sortStatesAscending(int[] score, Vector<GameState> nextStates){
        int biggestIndex;
        int temp;
        GameState temp2;

        for (int i = 0; i < score.length - 1; i++) {
            biggestIndex = i;
            for (int minIndex = i + 1; minIndex < score.length; minIndex++) {
                if (score[minIndex] > score[biggestIndex]) {
                    biggestIndex = minIndex;
                }
            }
            temp = score[biggestIndex];
            temp2 = nextStates.get(biggestIndex);
            score[biggestIndex] = score[i];
            nextStates.set(biggestIndex, nextStates.get(i));
            score[i] = temp;
            nextStates.set(i, temp2);

        }
        return nextStates;
    }

    private int alphaBeta(GameState gameState, int depth, int alpha, int beta, int player){
        int value;
        String hash;

        if (depth == 0 || gameState.isEOG()){
            value = heuristic(gameState);
            return value;
        }

        Vector<GameState> nextStates = new Vector<GameState>();
        gameState.findPossibleMoves(nextStates);

        //Hash Map
        hash = gameState.toMessage().substring(0, 32);
        if (hashMap.get(hash) != null) {
            value = hashMap.get(hash);
            return value;
        }
        //Reversed state
        hash = gameState.reversed().toMessage().substring(0, 32);
        if (hashMap.get(hash) != null) {
            value = hashMap.get(hash);
            return value;
        }


        if (player == myColour) {
            value = Integer.MIN_VALUE;
            for (GameState state : nextStates) {
                value = Math.max(value, alphaBeta(state, depth - 1, alpha, beta, otherColour));
                alpha = Math.max(alpha, value);
                if (beta <= alpha) {
                    break;
                }
            }
        } else {
            value = Integer.MAX_VALUE;
            for (GameState state : nextStates) {
                value = Math.min(value, alphaBeta(state, depth - 1, alpha, beta, myColour));
                beta = Math.min(beta, value);
                if (beta <= alpha) {
                    break;
                }
            }
        }
        if(depth > 6) {
            hashMap.put(hash, value);
        }

        return value;
    }

    private int heuristic(GameState state){

        if(state.isEOG()){
            if(myColour == Constants.CELL_RED){
                if(state.isRedWin()){
                    return Integer.MAX_VALUE;
                }
                else if(state.isWhiteWin()){
                    return Integer.MIN_VALUE;
                }
                else{
                    return 0;
                }
            }
            else {
                if (state.isRedWin()) {
                    return Integer.MIN_VALUE;
                } else if (state.isWhiteWin()) {
                    return Integer.MAX_VALUE;
                } else {
                    return 0;
                }
            }
        }

        int score;
        int scoreNumber = 0;
        int scoreKings = 0;
        int scorePosition = 0;


        for (int i = 0; i < 32; i++) {
            if(myColour == Constants.CELL_RED) {
                if (state.get(i) == Constants.CELL_RED) {
                    scoreNumber += 3;
                    scoreKings += kingValues[i];
                    scorePosition += redScoreBoard[i];
                } else if (state.get(i) == (Constants.CELL_RED | Constants.CELL_KING)) {
                    scoreNumber += 5;
                    scoreKings += kingValues[i];
                    scorePosition += redScoreBoard[i];
                } else if (state.get(i) == Constants.CELL_WHITE) {
                    scoreNumber -= 3;
                    scoreKings -= 8 - kingValues[i];
                    scorePosition -= whiteScoreBoard[i];
                } else if (state.get(i) == (Constants.CELL_WHITE | Constants.CELL_KING)) {
                    scoreNumber -= 5;
                    scoreKings -= 8 - kingValues[i];
                    scorePosition -= whiteScoreBoard[i];
                }
            }
            else{
                if (state.get(i) == Constants.CELL_WHITE) {
                    scoreNumber += 3;
                    scoreKings += kingValues[i];
                    scorePosition += whiteScoreBoard[i];
                } else if (state.get(i) == (Constants.CELL_WHITE | Constants.CELL_KING)) {
                    scoreNumber += 5;
                    scoreKings += kingValues[i];
                    scorePosition += whiteScoreBoard[i];
                } else if (state.get(i) == Constants.CELL_RED) {
                    scoreNumber -= 3;
                    scoreKings -= 8 - kingValues[i];
                    scorePosition -= redScoreBoard[i];
                } else if (state.get(i) == (Constants.CELL_RED | Constants.CELL_KING)) {
                    scoreNumber -= 5;
                    scoreKings -= 8 - kingValues[i];
                    scorePosition -= redScoreBoard[i];
                }
            }
        }

        score = (scoreNumber * 1000) + (scoreKings * 100) + scorePosition;

        return score;
    }
}