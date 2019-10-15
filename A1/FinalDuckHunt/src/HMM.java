import java.util.Arrays;
import java.util.Random;

public class HMM {

    public double[][] A;
    public double[][] B;
    public double[][] Pi;

    public int[] observationHistory = new int[0];
    public double[] scale;

    public int N;
    public int M;
    public int numOfStates;

    //Initialise HMM with number of states = 2
    HMM() {
        initializeA(2);
        initializeB(2);
        initializePi(2);
    }

    private void initializeA(int states) {

        Random random = new Random();

        A = new double[states][states];
        double tempScale = 0.0;

        for (int i = 0; i < states; i++) {
            for (int j = 0; j < states; j++) {
                A[i][j] = random.nextDouble();
                tempScale += A[i][j];
            }
            tempScale = 1 / tempScale;
            for (int j = 0; j < states; j++) {
                A[i][j] = tempScale * A[i][j];
            }
            tempScale = 0.0;
        }
    }
    private void initializeB(int states) {

        Random random = new Random();

        B = new double[states][Constants.COUNT_MOVE];
        double tempScale = 0.0;

        for (int i = 0; i < states; i++) {
            for (int j = 0; j < Constants.COUNT_MOVE; j++) {
                B[i][j] = random.nextDouble();
                tempScale += B[i][j];
            }
            tempScale = 1 / tempScale;
            for (int j = 0; j < Constants.COUNT_MOVE; j++) {
                B[i][j] = tempScale * B[i][j];
            }
            tempScale = 0.0;
        }
    }

    private void initializePi(int states) {

        Random random = new Random();

        Pi = new double[1][states];
        double tempScale = 0.0;

        for (int j = 0; j < states; j++) {
            Pi[0][j] = random.nextDouble();
            tempScale += Pi[0][j];
        }
        tempScale = 1 / tempScale;
        for (int j = 0; j < states; j++) {
            Pi[0][j] = tempScale * Pi[0][j];
        }
    }

    public void estimateModel(int[] O) {
        // Observation sequence is limited to size 1,000
        // Add new observations up to this limit
        if (observationHistory.length + O.length < 1000){
            int[] observationHistoryTemp = new int[observationHistory.length + O.length];

            for (int i = 0; i<observationHistory.length; i++){
                observationHistoryTemp[i] = observationHistory[i];
            }
            for (int j = 0; j<O.length; j++){
                observationHistoryTemp[j+observationHistory.length] = O[j];
            }
            observationHistory = observationHistoryTemp;
        }
        O = observationHistory;

        int maxIters;
        //As the observation length grows, increase the number of states
        if(O.length < 150){
            numOfStates = 2;
            maxIters = 100;
        }
        else if(O.length < 300){
            numOfStates = 3;
            maxIters = 100;
        }
        else{
            numOfStates = 4;
            maxIters = 75;
        }

        // Step 1: Initialisations
        initializeA(numOfStates);
        initializeB(numOfStates);
        initializePi(numOfStates);

        int T = O.length;
        N = A.length;
        M = B[0].length;

        int iters = 0; // tracks current iteration
        double oldLogProb = -1000000.0; // "-infinity"
        double[][] gamma = new double[T][N]; // gamma
        double[][][] digamma = new double[T][N][N]; // di-gamma
        double denominator;
        double numerator;

        while (true) {
            // Step 2: Alpha-Pass
            double[][] alpha = computeAlpha(A, B, Pi, O);

            // Step 3: Beta-Pass
            double[][] beta = computeBeta(A, B, Pi, O);

            // Step 4: Gamma and Di-Gamma
            for (int t = 0; t < T - 1; t++) {
                denominator = 0.0;
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        denominator = denominator + alpha[t][i] * A[i][j] * B[j][O[t + 1]] * beta[t + 1][j];
                        if (Double.isNaN(denominator)) {
                            denominator = 0.0;
                        }
                    }
                }
                for (int i = 0; i < N; i++) {
                    gamma[t][i] = 0.0;
                    for (int j = 0; j < N; j++) {
                        digamma[t][i][j] = (alpha[t][i] * A[i][j] * B[j][O[t + 1]] * beta[t + 1][j]) / denominator;
                        if (Double.isNaN(digamma[t][i][j])) {
                            digamma[t][i][j] = 0.0;
                        }
                        gamma[t][i] = gamma[t][i] + digamma[t][i][j];
                    }
                }
            }
            // Special case for last gamma
            denominator = 0.0;
            for (int i = 0; i < N; i++) {
                denominator = denominator + alpha[T - 1][i];
            }
            for (int i = 0; i < N; i++) {
                gamma[T - 1][i] = alpha[T - 1][i] / denominator;
                if (Double.isNaN(gamma[T - 1][i])) {
                    gamma[T - 1][i] = 0.0;
                }
            }

            // Step 5: Re-estimate A, B and Pi
            // Re-estimate Pi
            for (int i = 0; i < N; i++) {
                Pi[0][i] = gamma[0][i];
            }

            // Re-estimate A
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    numerator = 0.0;
                    denominator = 0.0;
                    for (int t = 0; t < T - 1; t++) {
                        numerator = numerator + digamma[t][i][j];
                        denominator = denominator + gamma[t][i];
                    }
                    A[i][j] = numerator / denominator;
                    if (Double.isNaN(A[i][j])) {
                        A[i][j] = 0.0;
                    }
                }
            }

            // Re-estimate B
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < M; j++) {
                    numerator = 0.0;
                    denominator = 0.0;
                    for (int t = 0; t < T; t++) {
                        if (O[t] == j) {
                            numerator = numerator + gamma[t][i];
                        }
                        denominator = denominator + gamma[t][i];
                    }

                    B[i][j] = numerator / denominator;

                    if (Double.isNaN(B[i][j])) {
                        B[i][j] = 0.0;
                    }
                }
            }

            // Step 6: Compute log[P(O|model)]
            Double logProb = 0.0;
            for (int t = 0; t < T; t++) {
                logProb = logProb + Math.log(scale[t]);
            }
            logProb = -1 * logProb;

            // Step 7: To iterate or not to iterate, that is the question...
            iters++;
            if ((iters < maxIters) && (logProb > oldLogProb)) {
                oldLogProb = logProb;
            } else
                break;
        }
    }

    public double[][] computeAlpha(double[][] A, double[][] B, double[][] Pi, int[] O) {

        int T = O.length;
        double[][] alpha = new double[T][N];
        scale = new double[T];

        // Compute initial alpha
        scale[0] = 0.0;
        for (int i = 0; i < N; i++) {
            alpha[0][i] = Pi[0][i] * B[i][O[0]];
            scale[0] = scale[0] + alpha[0][i];
        }
        // Scale alpha
        scale[0] = 1.0 / scale[0];
        if (Double.isNaN( scale[0])) {
            scale[0] = 0.0;
        }

        for (int i = 0; i < N; i++) {
            alpha[0][i] = scale[0] * alpha[0][i];
        }
        // Compute alpha
        for (int t = 1; t < T; t++) {
            scale[t] = 0.0;
            for (int i = 0; i < N; i++) {
                alpha[t][i] = 0.0;
                for (int j = 0; j < N; j++) {
                    if (Double.isNaN( alpha[t - 1][j])) {
                        alpha[t - 1][j] = 0.0;
                    }
                    alpha[t][i] = alpha[t][i] + alpha[t - 1][j] * A[j][i];
                }
                alpha[t][i] = alpha[t][i] * B[i][O[t]];
                if (Double.isNaN( alpha[t][i])) {
                    alpha[t][i] = 0.0;
                }
                scale[t] = scale[t] + alpha[t][i];
            }
            // Scale alpha
            scale[t] = 1.0 / scale[t];
            if (Double.isNaN( scale[t])) {
                scale[t] = 0.0;
            }
            for (int i = 0; i < N; i++) {
                alpha[t][i] = scale[t] * alpha[t][i];
                if (Double.isNaN( alpha[t][i])) {
                    alpha[t][i] = 0.0;
                }
            }
        }
        return alpha;
    }

    public double[][] computeBeta(double[][] A, double[][] B, double[][] Pi, int[] O) {

        int T = O.length;
        double[][] beta = new double[T][N];

        for (int i = 0; i < N; i++) {
            beta[T - 1][i] = scale[T - 1];
        }

        for (int t = T - 2; t >= 0; t--) {
            for (int i = 0; i < N; i++) {
                beta[t][i] = 0.0;
                for (int j = 0; j < N; j++) {
                    if (Double.isNaN( beta[t + 1][i])) {
                        beta[t][i] = 0.0;
                    }
                    beta[t][i] = beta[t][i] + (A[i][j] * B[j][O[t + 1]] * beta[t + 1][j]);
                    if (Double.isNaN( beta[t][i])) {
                        beta[t][i] = 0.0;
                    }
                }
                beta[t][i] = scale[t] * beta[t][i];
                if (Double.isNaN( beta[t][i])) {
                    beta[t][i] = 0.0;
                }
            }
        }
        return beta;
    }

    public int[] viterbi(int[] O) {

        int T = O.length;
        double[][] delta = new double[T][N];
        int[][] tracker = new int[T][N];

        // Compute initial delta
        for (int i = 0; i < N; i++) {
            delta[0][i] = Pi[0][i]*B[i][O[0]];
        }
        // Compute delta
        for (int t = 1; t < T; t++) {
            for (int i = 0; i < N; i++) {
                delta[t][i] = 0.0;
                Double temp = 0.0;
                for (int j = 0; j < N; j++) {
                    temp = delta[t - 1][j] * A[j][i] * B[i][O[t]];
                    if (temp > delta[t][i]) {
                        delta[t][i] = temp;
                        tracker[t][i] = j;
                    }
                }
            }
        }
        // Finding the best path
        int[] stateSequence = new int[T];
        double temp = 0.0;

        for (int i = 0; i < N; i++) {
            if (delta[T - 1][i] > temp) {
                temp = delta[T - 1][i];
                stateSequence[T - 1] = i;
            }
        }
        for (int t = T - 2; t >= 0; t--) {
            stateSequence[t] = tracker[t + 1][stateSequence[t + 1]];
        }
        return stateSequence;
    }

    public double[] getNextEmission(int currentState) {
        double[] mostProbableEmission = new double[2];
        double[][] stateDis = new double[Pi.length][N];

        for (int i = 0; i < N; i++) {
            if (i != currentState) {
                stateDis[0][i] = 0;
            } else if (i == currentState) {
                stateDis[0][i] = 1;
            }
        }

        double[][] nextStateDis = multiplyMatrix(stateDis, A);
        double[][] nextEmissionDis = multiplyMatrix(nextStateDis, B);

        for (int i = 0; i < M; i++) {
            if (nextEmissionDis[0][i] > mostProbableEmission[0]) {
                mostProbableEmission[0] = nextEmissionDis[0][i];
                mostProbableEmission[1] = i;
            }
        }
        // mostProbableEmission will contain: the probability of the most likely
        // emission and the index (i.e. move in Constants.java)
        return mostProbableEmission;
    }

    public double probOfObsSeq(int[] O) {
        // The probability of the observation sequence given the model
        // is computed via the Alpha-Pass

        int T = O.length;
        double[][] alpha = new double[T][N];
        double[] scaler = new double[T];

        initializePi(numOfStates);

        // Compute initial alpha
        scaler[0] = 0.0;
        for (int i = 0; i < N; i++) {
            alpha[0][i] = Pi[0][i] * B[i][O[0]];
            scaler[0] = scaler[0] + alpha[0][i];
        }
        // Scale alpha
        scaler[0] = 1.0 / scaler[0];
        if (Double.isNaN( scaler[0])) {
            scaler[0] = 0.0;
        }

        for (int i = 0; i < N; i++) {
            alpha[0][i] = scaler[0] * alpha[0][i];
        }
        // Compute alpha
        for (int t = 1; t < T; t++) {
            scaler[t] = 0.0;
            for (int i = 0; i < N; i++) {
                alpha[t][i] = 0.0;
                for (int j = 0; j < N; j++) {
                    if (Double.isNaN( alpha[t - 1][j])) {
                        alpha[t - 1][j] = 0.0;
                    }
                    alpha[t][i] = alpha[t][i] + alpha[t - 1][j] * A[j][i];
                }
                alpha[t][i] = alpha[t][i] * B[i][O[t]];
                if (Double.isNaN( alpha[t][i])) {
                    alpha[t][i] = 0.0;
                }
                scaler[t] = scaler[t] + alpha[t][i];
            }
            // Scale alpha
            scaler[t] = 1.0 / scaler[t];
            if (Double.isNaN( scaler[t])) {
                scaler[t] = 0.0;
            }
            for (int i = 0; i < N; i++) {
                alpha[t][i] = scaler[t] * alpha[t][i];
                if (Double.isNaN( alpha[t][i])) {
                    alpha[t][i] = 0.0;
                }
            }
        }
        double PO = 0.0;
        for (int i = 0; i < T; i++) {
            PO -= Math.log(scaler[i]);
        }
        return PO;
    }

    public double[][] multiplyMatrix(double[][] x, double[][] y) {
        int len1 = x.length;
        int len2 = x[0].length;
        int len3 = y[0].length;
        int len4 = y.length;

        // create new matrix of size NxM
        double[][] result = new double[len1][len3];

        // check dimensions are valid
        if (len2 == len4) {
            double sum = 0.0;

            // matrix multiplication
            for (int i = 0; i < len1; i++) {
                for (int j = 0; j < len3; j++) {
                    for (int k = 0; k < len2; k++) {
                        sum = sum + x[i][k] * y[k][j];
                    }
                    result[i][j] = sum;
                    sum = 0.0;
                }
            }
        } else {
            System.out.println("Matrix dimensions are incorrect");
        }
        return result;
    }
}