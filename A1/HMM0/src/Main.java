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

        //place values into the new matrix (skipping the first two)
        int k = 0;
        for(int i = 0; i < N; i++){
            for (int j = 0; j < M; j++){
                matrix[i][j] = Double.parseDouble(splitInput[k + 2]);
                k++;
            }
        }
        return matrix;
    }

    //multiply two matrices together
    public static Double[][] multiplyMatrix(Double[][] x, Double[][] y) {
        //This function will multiple matrix x of dimension NxP and matrix y of dimension OxM given P == O
        int N = x.length;
        int P = x[0].length;
        int M = y[0].length;
        int O = y.length;

        //create new matrix of size NxM
        Double[][] result = new Double[N][M];

        //check dimensions are valid
        if(P == O) {
            Double sum = 0.0;

            //matrix multiplication
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < M; j++) {
                    for (int k = 0; k < P; k++) {
                        sum = sum + x[i][k] * y[k][j];
                    }
                    result[i][j] = sum;
                    sum = 0.0;
                }
            }
        }
        else{
            System.out.println("Matrix dimensions are incorrect");
        }
        return result;
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

        //Build matrices
        Double[][] A = createMatrix(inputA);
        Double[][] B = createMatrix(inputB);
        Double[][] Pi = createMatrix(inputPi);

        //Calculations
        Double[][] probX2 = multiplyMatrix(Pi, A);
        Double[][] probEmission = multiplyMatrix(probX2, B);

        //Output to user the probability distribution of the next emission
        System.out.println(outputMatrix(probEmission));
    }
}
