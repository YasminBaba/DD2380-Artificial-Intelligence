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

    public static Integer[] getBestPath(Double[][] a, Double[][] b, Double[][] pi, Integer[] o){
        int N = a.length;   //number of hidden states
        int K = o.length;   //number of observations in observed sequence

        Double[][] delta = new Double[K][N];
        Integer[][] tracker = new Integer[K][N];

        //Veterbi Algorithm
        //Compute initial delta
        for(int i = 0; i < N; i++){
            delta[0][i] = pi[0][i]*b[i][o[0]];
        }
        //Compute delta
        for(int k = 1; k < K; k++){
            for(int i = 0; i < N; i++){
                delta[k][i] = 0.0;
                Double temp = 0.0;
                for(int j = 0; j < N; j++){
                    temp = delta[k-1][j]*a[j][i]*b[i][o[k]];
                    System.out.println(temp);
                    if (temp > delta[k][i]){
                        delta[k][i] = temp;
                        tracker[k][i] = j;
                    }
                }
            }
        }

        //Finding the best path
        Integer[] bestPath = new Integer[K];
        Double temp = 0.0;

        for(int i = 0; i < N; i++){
            if(delta[K-1][i] > temp){
                temp = delta[K-1][i];
                bestPath[K-1] = i;
            }
        }

        for(int k = K-2; k >= 0; k--){
            bestPath[k] = tracker[k+1][bestPath[k+1]];
        }

        return bestPath;
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

        //estimate the most probable sequence of states
        Integer[] bestPath = getBestPath(A, B, Pi, O);

        //convert to proper outputting format
        StringBuilder output = new StringBuilder();
        for(int i = 0; i < bestPath.length; i++){
            output.append(bestPath[i]);
            output.append(" ");
        }

        //output estimate the most probable sequence of states
        System.out.println(output);
    }
}