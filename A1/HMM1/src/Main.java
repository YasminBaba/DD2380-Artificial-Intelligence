import java.util.Scanner;

public class Main {

    //built Matrix from input string
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

    public static Double[][] getAlpha(Double[][] a, Double[][] b, Double[][] pi, Integer[] o){
        int N = a.length;   //number of hidden states
        int K = o.length;   //number of observations in observed sequence

        Double[][] alpha = new Double[K][N];
        //Compute initial alpha
        for(int i = 0; i < N; i++){
            alpha[0][i] = pi[0][i]*b[i][o[0]];
        }
        //Compute alpha
        for(int k = 1; k < K; k++){
            for(int i = 0; i < N; i++){
                Double temp = 0.0;
                for(int j = 0; j < N; j++){
                    temp = temp + alpha[k-1][j]*a[j][i];
                }
                alpha[k][i] = temp*b[i][o[k]];
            }
        }
        return alpha;
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

        //compute alpha
        Double[][] alpha = getAlpha(A, B, Pi, O);

        //compute the probability of the given sequence
        Double ProbO = 0.0;
        for(int i = 0; i < A.length; i++){
            ProbO = ProbO + alpha[O.length-1][i];
        }

        //output the probability of the given sequence
        System.out.println(ProbO);
    }
}
