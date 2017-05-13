package ist.cnv;

import com.amazonaws.services.dynamodbv2.xspec.L;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.*;

/**
 * Created on 11-05-2017.
 */

public class MatrixEstimator {
    private static final int SIDE = 40;
    private static final int LINE = 0;
    private static final int COLUMN = 1;

    private int[][] matrix;
    private long[] pixel;
    private long sideCounter;

    private Fraction[] used;
    private Fraction prop;

    public MatrixEstimator(int[][] matrix) {
        this.matrix = matrix;
        pixel = new long[] {0, 0};
        sideCounter = 1;
        used = new Fraction[] {new Fraction(0, 1), new Fraction(0, 1)};
        prop = new Fraction(matrix[0].length, SIDE);
    }

    public double nextPixel() {
        long nextCol = (long) floor(prop.add(pixel[COLUMN]).add(used[COLUMN]).toDouble());
        List<List<long[]>> neededPixels = new ArrayList<>();
        long neededLine = (long) ceil(prop.add(used[LINE]).toDouble());
        long neededCol = (long) ceil(prop.add(used[COLUMN]).toDouble());

        // Gets the needed pixels for the calculation
        for(long i = 0; i < neededLine; i++) {
            List<long[]> tmpList = new ArrayList<>();
            neededPixels.add(tmpList);

            long l = pixel[LINE] + i;
            for(long j = 0; j < neededCol; j++)
                tmpList.add(new long[] {l, pixel[LINE] + j});
        }

        // Calculates the value for the load balancer table
        double res = 0;
        for(long i = 0; i < neededLine; i++) {
            Fraction useLine;

            if(i == neededLine - 1) {
                useLine = used[LINE].add(prop);

                if(useLine.toDouble() < 0)
                    useLine = prop;
                else
                    useLine.removeInteger();

                if(useLine.toDouble() == 0) {
                    if(SIDE < matrix[0].length)
                        useLine = new Fraction(1,1);
                    else
                        useLine = prop;
                }
            }

            else if(i == 0)
                useLine = used[LINE].inverseSub(1);
            else
                useLine = new Fraction(1,1);

            for(long j = 0; j < neededCol; j++) {
                Fraction useCol;
                if(j == neededCol - 1) {
                    useCol = used[COLUMN].add(prop);

                    if(useCol.toDouble() < 0)
                        useCol = prop;
                    else
                        useCol.removeInteger();

                    if(useCol.toDouble() == 0) {
                        if(SIDE < matrix[0].length)
                            useCol = new Fraction(1, 1);
                        else
                            useCol = prop;
                    }
                }

                else if(j == 0)
                    useCol = used[COLUMN].inverseSub(1);
                else
                    useCol = new Fraction(1, 1);

                long lineM = neededPixels.get((int) i).get((int) j)[LINE];
                long colM = neededPixels.get((int) i).get((int) j)[COLUMN];

                res += useLine.mul(useCol).mul(matrix[ (int) lineM][(int) colM]).toDouble();
            }
        }

        // Updates the next pixel to be calculated
        if(nextCol < matrix[0].length && sideCounter < SIDE) {
            pixel[COLUMN] = nextCol;
            used[COLUMN] = used[COLUMN].add(pixel[COLUMN]).add(prop);
            used[COLUMN].removeInteger();
            sideCounter++;
        }

        else {
            long nextLine = (long) floor(used[LINE].add(pixel[LINE]).add(prop).toDouble());
            pixel = new long[] {nextLine, 0};

            used[LINE] = used[LINE].add(pixel[LINE]).add(prop);
            used[LINE].removeInteger();

            used[COLUMN] = new Fraction(0,1);
            sideCounter = 1;
        }

        return res;
    }

    public void convertToSquare() {
        long nLines = matrix.length;
        long nCols = matrix[0].length;
        boolean noPadding = (nLines + nCols) % 2 == 0;

        // Non-initialized vars
        double linesPad = -1;
        double colsPad = -1;
        long sqrSide = -1;

        if(nCols > nLines) {
            sqrSide = noPadding ? nCols : (nCols + 1);
            linesPad = (double) sqrSide/2 - (double) nLines/2;
        }

        else {
            sqrSide = noPadding ? nLines : (nLines + 1);
            colsPad = (double) sqrSide/2 - (double) nCols/2;
        }

        int[][] tmpMatrix = new int[(int) sqrSide][(int) sqrSide];

        if(linesPad > -1) {
            if(noPadding) {
                for(int i = 0; i < nLines; i++)
                    for(int j = 0; j < nCols; j++)
                        tmpMatrix[(int) (i + linesPad)][j] = matrix[i][j];
            }

            else {
                Fraction ratio = new Fraction(nCols, sqrSide);
                for(int i = 0; i < nLines; i++) {
                    long current = 0;
                    Fraction used = new Fraction(0,1);
                    Fraction value;

                    for(int j = 0; j < sqrSide; j++) {
                        if(ratio.toDouble() > used.inverseSub(1).toDouble()) {
                            Fraction n1 = used.inverseSub(1).mul(matrix[i][(int) current]);
                            Fraction n2 = used.add(ratio).sub(1).mul(matrix[i][(int) (current + 1)]);
                            value = n1.add(n2);
                            current += 1;
                            used = used.add(ratio).sub(1);
                        }

                        else {
                            value = ratio.mul(matrix[i][(int) current]);
                            used = used.add(ratio);
                        }

                        // TODO: Check if we want matrices of integers or doubles!!!
                        tmpMatrix[(int) (i + linesPad)][j] = (int) value.toDouble();
                    }
                }
            }
        }

        else {
            if(noPadding) {
                for(int i = 0; i < nLines; i++)
                    for(int j = 0; j < nCols; j++)
                        tmpMatrix[i][(int) (j + colsPad)] = matrix[i][j];
            }

            else {
                Fraction ratio = new Fraction(nLines, sqrSide);

                for(int j = 0; j < nCols; j++) {
                    long current = 0;
                    Fraction used = new Fraction(0,1);
                    Fraction value;

                    for(int i = 0; i < nLines; i++) {
                        if(ratio.toDouble() > used.inverseSub(1).toDouble()) {
                            Fraction n1 = used.inverseSub(1).mul(matrix[(int) current][j]);
                            Fraction n2 = used.add(ratio).sub(1).mul(matrix[(int) (current + 1)][j]);
                            value = n1.add(n2);
                            current += 1;
                            used = used.add(ratio).sub(1);
                        }

                        else {
                            value = ratio.mul(matrix[(int) current][j]);
                            used = used.add(ratio);
                        }

                        // TODO: Check if we want matrices of integers or doubles!!!
                        tmpMatrix[i][(int) (j + colsPad)] = (int) value.toDouble();
                    }
                }
            }
        }

        matrix = tmpMatrix;
    }

    public void revertTranslation(long sl, long sc, long sloff, long scoff) {
        long nLines = matrix.length;
        long nCols = matrix[0].length;
        long maxLineIndex = sl - 1;
        long maxLineIndexFirst = nLines - 1;

        int[][] tmpMatrix = new int[(int) sl][(int) sc];

        for(int i = 0; i < nLines; i++)
            for(int j = 0; j < nCols; j++)
                tmpMatrix[(int) (maxLineIndex - i - sloff)][(int) (j + scoff)] = matrix[(int) (maxLineIndexFirst - i)][j];

        matrix = tmpMatrix;
    }

    // TODO: Check if this can be done in the nextPixel!!!
    public int[][] scaleToStore() {
        int[][] res = new int[SIDE][SIDE];

        for(int i = 0; i < SIDE; i++)
            for(int j = 0; j < SIDE; j++)
                // TODO: Check if we want matrices of integers or doubles!!!
                res[i][j] = (int) nextPixel();

        return res;
    }

    // FIXME: Just for testing purposes!!!
    public void printMatrix() {
        long nLines = matrix.length;
        long nCols = matrix[0].length;

        for(int i = 0; i < nLines; i++) {
            StringBuilder line = new StringBuilder("|");
            for(int j = 0; j < nCols; j++)
                line.append(String.valueOf(matrix[i][j]) + "|");
            System.out.println(line);
        }
    }

    // FIXME: Just for testing purposes!!!
    public static void main(String[] args) {
        int[][] matrix = new int[80][80];
        MatrixEstimator me = new MatrixEstimator(matrix);

        me.printMatrix();
        me.nextPixel();
    }
}
