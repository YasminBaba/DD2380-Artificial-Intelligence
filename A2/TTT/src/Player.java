import java.util.*;

public class Player {

    private final int PLAYER_X = 1;
    private final int PLAYER_O = 2;

    private int[][] possibleWins = {
            {0, 1, 2, 3}, {4, 5, 6, 7}, {8, 9, 10, 11}, {12, 13, 14, 15},      //rows
            {0, 4, 8, 12}, {1, 5, 9, 13}, {2, 6, 10, 14}, {3, 7, 11, 15},      //columns
            {0, 5, 10, 15}, {3, 6, 9, 12}                                       //diagonals
    };
    private int[][] evaluationMatrix = {
            {0, -10, -100, -1000, -10000},
            {10,  0,  0,  0,  0},
            {100,  0,  0,  0,  0},
            {1000,  0,  0,  0,  0},
            {10000,  0,  0,  0,  0}
    };

    /**
     * Performs a move
     *
     * @param gameState
     *            the current state of the board
     * @param deadline
     *            time before which we must have returned
     * @return the next state the board is in after our move
     */
    public GameState play(final GameState gameState, final Deadline deadline) {
        Vector<GameState> nextStates = new Vector<GameState>();
        gameState.findPossibleMoves(nextStates);

        if (nextStates.size() == 0) {
            // Must play "pass" move if there are no other moves possible.
            return new GameState(gameState, new Move());
        }

        /**
         * Here you should write your algorithms to get the best next move, i.e.
         * the best next state. This skeleton returns a random move instead.
         */

        int depth = 4;
        int beta = 100_000_000;
        int alpha = -100_000_000;
        int stateTracker = 0;

        if(gameState.getNextPlayer() == PLAYER_X){
            int bestValue = -100_000_000;
            for(int i = 0; i < nextStates.size(); i++){
                int value = alphaBetaAlgo(nextStates.elementAt(i), depth, alpha, beta, PLAYER_O);
                if(value > bestValue){
                    bestValue = value;
                    stateTracker = i;
                }
                alpha = Math.max(alpha, value);
            }
            return nextStates.elementAt(stateTracker);
        }
        else{
            int bestValue = 100_000_000;
            for(int i = 0; i < nextStates.size(); i++){
                int value = alphaBetaAlgo(nextStates.elementAt(i), depth, alpha, beta, PLAYER_X);
                if(value < bestValue){
                    bestValue = value;
                    stateTracker = i;
                }
                beta = Math.min(beta, value);
            }
            return nextStates.elementAt(stateTracker);
        }
    }

    public int alphaBetaAlgo(GameState state, int depth, int alpha, int beta, int player){

        if(depth == 0 || state.isEOG()){
            return evaluate(state);
        }

        Vector<GameState> nextStates = new Vector<GameState>();
        state.findPossibleMoves(nextStates);

        int value;

        if(player == PLAYER_X){
            value = -100_000_000;
            for(int i = 0; i < nextStates.size(); i++){
                value = Math.max(value, alphaBetaAlgo(nextStates.get(i), depth - 1, alpha, beta, PLAYER_O));
                alpha = Math.max(alpha, value);
                if(beta <= alpha){
                    return value;
                }
            }
        }
        else{
            value = 100_000_000;
            for(int i = 0; i < nextStates.size(); i++){
                value = Math.min(value, alphaBetaAlgo(nextStates.get(i), depth - 1, alpha, beta, PLAYER_X));
                beta = Math.min(beta, value);
                if(beta <= alpha) {
                    return value;
                }
            }
        }
        return value;
    }

    public int evaluate(GameState state){
        if(state.isEOG()) {
            if (state.isXWin()) {
                return 100_000_000;
            } else if (state.isOWin()) {
                return -100_000_000;
            } else {
                return 0;
            }
        }

        int score = 0;
        int scoreX;
        int scoreO;

        for(int i = 0; i < 10; i++){
            scoreX = 0;
            scoreO = 0;
            for(int j = 0; j < 4; j++){
                int currentCell = state.at(possibleWins[i][j]);
                if(currentCell == PLAYER_X){
                    scoreX++;
                }
                if(currentCell == PLAYER_O){
                    scoreO++;
                }
            }
            score += evaluationMatrix[scoreX][scoreO];
        }
        return score;
    }
}
