import java.util.*;

public class Player {

    private final int PLAYER_X = 1;
    private final int PLAYER_O = 2;

    private int[][] possibleWins = {
            //HORIZONTAL ROWS
            {0, 1, 2, 3}, {4, 5, 6, 7}, {8, 9, 10, 11}, {12, 13, 14, 15},
            {16, 17, 18, 19}, {20, 21, 22, 23}, {24, 25, 26, 27}, {28, 29, 30, 31},
            {32, 33, 34, 35}, {36, 37, 38, 39}, {40, 41, 42, 43}, {44, 45, 46, 47},
            {48, 49, 50, 51}, {52, 53, 54, 55}, {56, 57, 58, 59}, {60, 61, 62, 63},
            //COLUMNS
            {0, 4, 8, 12}, {1, 5, 9, 13}, {2, 6, 10, 14}, {3, 7, 11, 15},
            {16, 20, 24, 28}, {17, 21, 25, 29}, {18, 22, 26, 30}, {19, 23, 27, 31},
            {32, 36, 40, 44}, {33, 37, 41, 45}, {34, 38, 42, 46}, {35, 39, 43, 47},
            {48, 52, 56, 60}, {49, 53, 57, 61}, {50, 54, 58, 62}, {51, 55, 59, 63},
            //FLAT DIAGONALS
            {0, 17, 34, 51}, {4, 21, 38, 55}, {8, 25, 42, 59}, {12, 29, 46, 63},
            {3, 18, 33, 48}, {7, 22, 37, 52}, {11, 26, 41, 56}, {15, 30, 45, 60},
            //FRONT DIAGONALS
            {0, 5, 10, 15}, {16, 21, 26, 31}, {32, 37, 42, 47}, {48, 53, 58, 63},
            {3, 6, 9, 12}, {19, 22, 25, 28}, {35, 38, 41, 44}, {51, 54, 57, 60},
            //INWARDS ROWS
            {0, 16, 32, 48}, {1, 17, 33, 49}, {2, 18, 34, 50}, {3, 19, 35, 51},
            {4, 20, 36, 52}, {5, 21, 37, 53}, {6, 22, 38, 54}, {7, 23, 39, 55},
            {8, 24, 40, 56}, {9, 25, 41, 57}, {10, 26, 42, 58}, {11, 27, 43, 59},
            {12, 28, 44, 60}, {13, 29, 45, 61}, {14, 30, 46, 62}, {15, 31, 47, 63},
            //SIDE DIAGONALS
            {0, 20, 40, 60}, {1, 21, 41, 61}, {2, 22, 42, 62}, {3, 23, 43, 63},
            {12, 24, 36, 48}, {13, 25, 37, 49}, {14, 26, 38, 50}, {15, 27, 39, 51},
            //INTERNAL DIAGONALS
            {0, 21, 42, 63}, {12, 25, 38, 51}, {15, 26, 37, 48}, {3, 22, 41, 60}
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

        int depth = 2;
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

        for(int i = 0; i < 76; i++){
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
