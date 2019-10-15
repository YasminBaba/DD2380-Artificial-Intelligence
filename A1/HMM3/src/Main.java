import java.util.Scanner;

public class Main {

    public static Double[] scale;       //scaling factor

    public static Double[][] createMatrix(String input) {
        //split up the input string by white spaces and place each value in a string array
        String[] splitInput = input.split(" ");

        int N = Integer.parseInt(splitInput[0]);    //number of rows
        int M = Integer.parseInt(splitInput[1]);    //number of columns

        //create a new matrix of dimensions NxM
        Double[][] matrix = new Double[N][M];

        //place values (the third onwards) into the new matrix
        int k = 0;
        for(int i = 0; i < N; i++){
            for (int j = 0; j < M; j++){
                matrix[i][j] = Double.parseDouble(splitInput[k + 2]);
                k++;
            }
        }
        return matrix;
    }

    public static Integer[] createVector(String input) {
        String[] splitInput = input.split(" ");

        int M = Integer.parseInt(splitInput[0]);

        Integer[] vector = new Integer [M];

        int k = 0;
        for (int j = 0; j < M; j++){
            vector[j] = Integer.parseInt(splitInput[k + 1]);
            k++;
        }
        return vector;
    }

    public static void estimateModel(Double[][] A, Double[][] B, Double[][] Pi, Integer[] O) {

        //step 1: initialise
        Integer maxIters = 100;                 //max number of re-estimation iterations
        Integer iters = 0;                      //tracks current iteration
        Double oldLogProb = -1000000.0;    //"-infinity"

        int N = A.length;       //number of hidden states
        int M = B[0].length;    //number of observation symbols
        int T = O.length;       //number of observations in observed sequence
        Double[][] gamma = new Double[T][N];     //gamma
        Double[][][] digamma = new Double[T][N][N];     //di-gamma
        Double denominator = 0.0;
        Double numerator = 0.0;

        while(true) {
            //step 2: compute alpha
            Double[][] alpha = computeAlpha(A, B, Pi, O);

            //step 3: compute beta
            Double[][] beta = computeBeta(A, B, Pi, O);

            //step 4: computer gamma and di-gamma functions
            for (int t = 0; t < T - 1; t++) {
                denominator = 0.0;
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        denominator = denominator + alpha[t][i] * A[i][j] * B[j][O[t + 1]] * beta[t + 1][j];
                    }
                }
                for (int i = 0; i < N; i++) {
                    gamma[t][i] = 0.0;
                    for (int j = 0; j < N; j++) {
                        digamma[t][i][j] = (alpha[t][i] * A[i][j] * B[j][O[t + 1]] * beta[t + 1][j]) / denominator;
                        gamma[t][i] = gamma[t][i] + digamma[t][i][j];
                    }
                }
            }
            //Special case for last gamma
            denominator = 0.0;
            for (int i = 0; i < N; i++) {
                denominator = denominator + alpha[T-1][i];
            }
            for (int i = 0; i < N; i++) {
                gamma[T-1][i] = alpha[T-1][i] / denominator;
            }

            //step 5: Re-estimate A, B and Pi
            //Re-estimate Pi
            for (int i = 0; i < N; i++) {
                Pi[0][i] = gamma[0][i];
            }

            //Re-estimate A
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    numerator = 0.0;
                    denominator = 0.0;
                    for (int t = 0; t < T - 1; t++) {
                        numerator = numerator + digamma[t][i][j];
                        denominator = denominator + gamma[t][i];
                    }
                    A[i][j] = numerator / denominator;
                }
            }

            //Re-estimate B
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
                }
            }

            //Step 6: Compute log[P(O|lamda)]
            Double logProb = 0.0;
            for (int t = 0; t < T; t++) {
                logProb = logProb + Math.log(scale[t]);
            }
            logProb = -1 * logProb;

            //Step 7: To iterate or not to iterate, that is the question...
            iters++;
            if ((iters < maxIters) && (logProb > oldLogProb)) {
                oldLogProb = logProb;
            } else
                break;
        }
        System.out.println(outputMatrix(A));
        System.out.println(outputMatrix(B));
    }

    public static Double[][] computeAlpha(Double[][] A, Double[][] B, Double[][] Pi, Integer[] O) {
        int N = A.length;       //number of hidden states
        int T = O.length;       //number of observations in observed sequence (i.e the number T)

        Double[][] alpha = new Double[T][N];
        scale = new Double[T];

        //Compute initial alpha
        scale[0] = 0.0;     //reset scaling factor to zero
        for(int i = 0; i < N; i++){
            alpha[0][i] = Pi[0][i]*B[i][O[0]];
            scale[0] = scale[0] + alpha[0][i];
        }
        //Scale alpha
        scale[0] = 1.0 / scale[0];
        for(int i = 0; i < N; i++){
            alpha[0][i] = scale[0]*alpha[0][i];
        }
        //Compute alpha
        for(int t = 1; t < T; t++){
            scale[t] = 0.0;
            for(int i = 0; i < N; i++){
                alpha[t][i] = 0.0;
                for(int j = 0; j < N; j++){
                    alpha[t][i] = alpha[t][i] + alpha[t-1][j]*A[j][i];
                }
                alpha[t][i] = alpha[t][i]*B[i][O[t]];
                scale[t] = scale[t] + alpha[t][i];
            }
            //Scale alpha
            scale[t] = 1.0 / scale[t];
            for(int i = 0; i < N; i++){
                alpha[t][i] = scale[t]*alpha[t][i];
            }
        }
        return alpha;
    }

    public static Double[][] computeBeta(Double[][] A, Double[][] B, Double[][] Pi, Integer[] O) {
        int N = A.length;       //number of hidden states
        int T = O.length;       //number of observations in observed sequence

        Double[][] beta = new Double[T][N];     //beta

        for(int i = 0; i < N; i++){
            beta[T-1][i] = scale[T-1];
        }
        //beta pass
        for(int t = T - 2; t >= 0; t--){
            for(int i = 0; i < N; i++){
                beta[t][i] = 0.0;
                for(int j = 0; j < N; j++){
                    beta[t][i] = beta[t][i] + A[i][j]*B[j][O[t+1]]*beta[t+1][j];
                }
                //Scale beta with same scale factor as alpha[k][i]
                beta[t][i] = scale[t]*beta[t][i];
            }
        }
        return beta;
    }

    //convert any matrix to the correct output string format
    public static StringBuilder outputMatrix(Double[][] x) {
        StringBuilder output = new StringBuilder();

        output.append(Integer.toString(x.length));      //number of rows
        output.append(" ");
        output.append(Integer.toString(x[0].length));   //number of columns

        for (int i = 0; i < x.length; i++){
            for (int j = 0; j < x[0].length; j++) {
                output.append(" ");
                output.append(x[i][j]);                 //values of matrix[i][j]
            }
        }
        return output;
    }

    public static void main(String[] args) {
    //Read input from console
    Scanner scanner = new Scanner(System.in);

    String inputA = scanner.nextLine();
    String inputB = scanner.nextLine();
    String inputPi = scanner.nextLine();
    String inputO = scanner.nextLine();

    //Build matrices
    Double[][] A = createMatrix(inputA);    //transition matrix
    Double[][] B = createMatrix(inputB);    //emission matrix
    Double[][] Pi = createMatrix(inputPi);  //pi
    Integer[] O = createVector(inputO);     //observation sequence

    //Re-estimate model
    estimateModel(A, B, Pi, O);
    }
}
