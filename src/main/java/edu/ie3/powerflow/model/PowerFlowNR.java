/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.model;

import static java.lang.Math.toIntExact;

import edu.ie3.powerflow.interfaces.PowerFlowAlgorithm;
import edu.ie3.powerflow.interfaces.PowerFlowEvaluation;
import edu.ie3.powerflow.model.enums.NodeType;
import edu.ie3.powerflow.model.evaluation.NewtonRaphsonEvaluation;
import edu.ie3.powerflow.model.evaluation.NewtonRaphsonIterationEvaluation;
import edu.ie3.powerflow.util.exceptions.PowerFlowException;
import edu.ie3.util.ArrayHelper;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import no.uib.cipr.matrix.DenseLU;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.MatrixSingularException;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.FieldMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SparseFieldMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kittl
 * @since 23.10.2018
 * @deprecated This class needs refactoring. Therefore, it is marked as deprecated.
 */
@Deprecated
public class PowerFlowNR implements PowerFlowAlgorithm {

  private static final Logger logger = LoggerFactory.getLogger(PowerFlowNR.class);

  /* Convergence threshold */
  private double epsilon;

  /* Maximum amount of iterations permissible */
  private int maxIterations;

  /* Admittance matrix of the system */
  private FieldMatrix<Complex> admittanceMatrix;

  /* Amount of nodes covered by the admittance matrix */
  protected int nodeCount;

  /* Array denoting the type of the node at the current position */
  private NodeType[] nodeTypes;

  /* Index of the slack node */
  private int sl = -1;

  /* Counting the occurrences of the different node types */
  private Map<NodeType, Integer> nodeTypeCount;

  /* Target voltage at each node */
  private Complex[] vTarget;

  /* When the admittance matrix has changed, then the kept jacobian matrix is useless */ boolean
      admittanceMatrixChanged;

  RealMatrix jacobianMatrix = null;

  public PowerFlowNR(double epsilon, int maxIterations) {
    this.epsilon = epsilon;
    this.maxIterations = maxIterations;
  }

  @Override
  public double getEpsilon() {
    return epsilon;
  }

  @Override
  public void setEpsilon(double epsilon) {
    this.epsilon = epsilon;
  }

  public int getMaxIterations() {
    return maxIterations;
  }

  public void setMaxIterations(int maxIterations) {
    this.maxIterations = maxIterations;
  }

  @Override
  public FieldMatrix<Complex> getAdmittanceMatrix() {
    return admittanceMatrix;
  }

  /**
   * Setting the admittance matrix needed for calculations
   *
   * @param admittanceMatrix {@link SparseFieldMatrix} to be used
   * @throws PowerFlowException is thrown, if the matrix does not meet the requirements
   */
  @Override
  public void setAdmittanceMatrix(FieldMatrix<Complex> admittanceMatrix) throws PowerFlowException {
    /* plausibility check */
    if (!admittanceMatrix.isSquare()) {
      throw new PowerFlowException(
          "The admittance matrix has non matching dimensions. Has to be a NxN-matrix.");
    }
    if (Arrays.stream(admittanceMatrix.getData())
        .anyMatch(d -> Arrays.stream(d).anyMatch(Complex::isNaN))) {
      throw new PowerFlowException("The admittance matrix does contain NaN values!");
    }
    if (Arrays.stream(admittanceMatrix.getData())
        .anyMatch(d -> Arrays.stream(d).anyMatch(Objects::isNull))) {
      throw new PowerFlowException("The admittance matrix does contain NULL values!");
    }

    this.admittanceMatrix = admittanceMatrix;
    this.nodeCount = admittanceMatrix.getColumnDimension();
    this.admittanceMatrixChanged = true;
  }

  public void setNodeTypeCount(Map<NodeType, Integer> nodeTypeCount) {
    this.nodeTypeCount = nodeTypeCount;
  }

  public NodeType[] getNodeTypes() {
    return nodeTypes;
  }

  public Complex[] getvTarget() {
    return vTarget;
  }

  public void setvTarget(Complex[] vTarget) {
    this.vTarget = vTarget;
  }

  /**
   * Set the vector of {@link NodeType}s and count their occurences
   *
   * @param nodeTypes Array of {@link NodeType}s
   * @throws PowerFlowException if there is some inconsistency in the data
   */
  public void setNodeTypes(NodeType[] nodeTypes) throws PowerFlowException {
    if (nodeTypes.length != nodeCount) {
      throw new PowerFlowException(
          "The vector of node types does not match the total amount of nodes.");
    }
    this.nodeTypes = nodeTypes;
    this.nodeTypeCount =
        Arrays.stream(NodeType.values())
            .collect(
                Collectors.toMap(
                    e -> (NodeType) e,
                    e -> toIntExact(Arrays.stream(nodeTypes).filter(t -> t.equals(e)).count())));
    this.sl = -1;

    for (int cnt = 0; cnt < nodeTypes.length; cnt++) {
      if (this.nodeTypes[cnt].equals(NodeType.SL)) {
        if (sl == -1) {
          this.sl = cnt;
        } else {
          logger.warn("Found more than one slack node. Take only the first one.");
          break;
        }
      }
    }
    if (sl == -1) {
      throw new PowerFlowException("Did not found any slack node. That's not okay...");
    }
  }

  @Override
  public PowerFlowResultJava calc(Complex[] actualS) throws PowerFlowException {
    Complex[] v = new Complex[nodeCount];
    Complex[] decrS = new Complex[nodeCount];
    Complex[] incrS = new Complex[nodeCount];
    Arrays.fill(v, new Complex(1d, 0d));
    Arrays.fill(decrS, new Complex(0d, 0d));
    Arrays.fill(decrS, new Complex(0d, 0d));
    return calc(actualS, decrS, incrS, v);
  }

  @Override
  public PowerFlowResultJava calc(Complex[] actualS, Complex[] decrS, Complex[] incrS)
      throws PowerFlowException {
    Complex[] v = new Complex[nodeCount];
    Arrays.fill(v, new Complex(1d, 0d));
    return calc(actualS, decrS, incrS, v);
  }

  @Override
  public PowerFlowResultJava calc(Complex[] actualS, Complex[] v) throws PowerFlowException {
    Complex[] decrS = new Complex[nodeCount];
    Complex[] incrS = new Complex[nodeCount];
    Arrays.fill(decrS, new Complex(0d, 0d));
    Arrays.fill(decrS, new Complex(0d, 0d));
    return calc(actualS, decrS, incrS, v);
  }

  @Override
  public PowerFlowResultJava calc(Complex[] actualS, Complex[] decrS, Complex[] incrS, Complex[] v)
      throws PowerFlowException {
    /* Some plausibility checks */
    if (admittanceMatrix == null) {
      throw new PowerFlowException("Admittance matrix is not set, yet! Invoke setter first!");
    }
    if (nodeTypes == null) {
      throw new PowerFlowException("Node types are not set, yet! Invoke setter first!");
    }
    if (vTarget == null) {
      throw new PowerFlowException("Target voltages are not set, yet! Invoke setter first!");
    }
    if (actualS.length != nodeCount) {
      throw new PowerFlowException(
          "The amount of nodal powers does not match the amount of nodes covered by the admittance matrix.");
    }

    /* Determine the array of desired voltages at each node */
    double[] targetV2 =
        ArrayHelper.pow(Arrays.stream(vTarget).map(Complex::abs).toArray(Double[]::new), 2d);

    /* When there is no valid start voltage vector given --> Do a flat start */
    if (v == null) {
      /* Take the nodal target voltages. */
      v = getvTarget();
    } else {
      /* Ensure, that the slack node is fixed to it's "new" target voltage */
      v[sl] = getvTarget()[sl];
    }

    long calcStart = System.currentTimeMillis();
    long calcEnd;

    LinkedList<PowerFlowEvaluation> evaluation = new LinkedList<>();

    int iterationCount = 0;
    boolean divergent = true;
    double[] iteratedV2 = new double[nodeCount]; /* iterated square nodal voltage magnitude */
    Complex[] iteratedS = new Complex[nodeCount]; /* iterated nodal apparent power */
    Complex[] i = new Complex[nodeCount]; /* iterated nodal current */

    while (divergent && iterationCount < maxIterations) {
      /* Build the nodal current sum at each node of the grid by applying Kirchhoff's law */
      Arrays.fill(iteratedS, new Complex(0d)); // Reset the nodal iterated apparent power
      Arrays.fill(i, new Complex(0.0, 0.0));
      for (int nodeI = 0; nodeI < nodeCount; nodeI++) {
        /* If node I is of type PV, then calculate the squared nodal voltage magnitude */
        if (nodeTypes[nodeI].equals(NodeType.PV)) {
          iteratedV2[nodeI] = Math.pow(v[nodeI].abs(), 2);
        } else {
          iteratedV2[nodeI] = 0;
        }

        for (int nodeJ = 0; nodeJ < nodeCount; nodeJ++) {
          /* Get the admittance of the direct connection between node A and B */
          Complex yIJ = admittanceMatrix.getEntry(nodeI, nodeJ);
          i[nodeI] = i[nodeI].add(yIJ.multiply(v[nodeJ]));
        }

        /* All currents going to or coming from node I have to sum up to 0. With the above algorithm all branch
         * currents have been calculated. What is left now, is the current flowing into the shunt elements at
         * node I. Calculate the apparent power and compare it to what is given from outside.*/
        iteratedS[nodeI] = v[nodeI].multiply(i[nodeI].conjugate());
      }

      /* Calculate the difference between the iterated values and those given as precondition */
      Complex[] deltaS = ArrayHelper.add(actualS, iteratedS);
      double[] deltaV2 = ArrayHelper.subtract(iteratedV2, targetV2);
      //            logger.debug("Iteration " + iterationCount + ": deltaP = [" +
      // String.format("%.3e",
      //                            Arrays.stream(deltaS).mapToDouble(Complex::getReal).min()
      //                                            .orElse(Double.NEGATIVE_INFINITY)) + "; " +
      // String.format("%.3e",
      //
      // Arrays.stream(deltaS).mapToDouble(Complex::getReal).average().orElse(0d)) + "; " +
      //                         String.format("%.3e",
      // Arrays.stream(deltaS).mapToDouble(Complex::getReal).max()
      //                                         .orElse(Double.POSITIVE_INFINITY)) + "], deltaQ =
      // [" + String.format("%.3e",
      //                            Arrays.stream(deltaS).mapToDouble(Complex::getImaginary).min()
      //                                            .orElse(Double.NEGATIVE_INFINITY)) + "; " +
      // String.format("%.3e",
      //
      // Arrays.stream(deltaS).mapToDouble(Complex::getImaginary).average().orElse(0d)) + "; " +
      //                         String.format("%.3e",
      // Arrays.stream(deltaS).mapToDouble(Complex::getImaginary).max()
      //                                         .orElse(Double.POSITIVE_INFINITY)) + "], e = [" +
      // String.format("%.3e",
      //
      // Arrays.stream(v).mapToDouble(Complex::getReal).min().orElse(Double.NEGATIVE_INFINITY)) +
      //                         "; " +
      //                         String.format("%.3e",
      // Arrays.stream(v).mapToDouble(Complex::getReal).average().orElse(0d)) +
      //                         "; " + String.format("%.3e",
      //
      // Arrays.stream(v).mapToDouble(Complex::getReal).max().orElse(Double.POSITIVE_INFINITY)) +
      //                         "] , f = [" + String.format("%.3e",
      // Arrays.stream(v).mapToDouble(Complex::getImaginary).min()
      //                            .orElse(Double.NEGATIVE_INFINITY)) + "; " +
      // String.format("%.3e",
      //
      // Arrays.stream(v).mapToDouble(Complex::getImaginary).average().orElse(0d)) + "; " +
      //                         String.format("%.3e",
      // Arrays.stream(v).mapToDouble(Complex::getImaginary).max()
      //                                         .orElse(Double.POSITIVE_INFINITY)) + "]");

      /* Reduce the difference vectors */
      RealVector delta = reduceDeviationVector(deltaS, deltaV2);
      RealVector deltaPred = delta.getSubVector(0, nodeCount - 1);
      RealVector deltaQred =
          delta.getSubVector(nodeCount - 1, nodeCount - 1 - nodeTypeCount.get(NodeType.PV));
      RealVector deltaV2Red =
          nodeTypeCount.get(NodeType.PV) != 0
              ? delta.getSubVector(
                  2 * (nodeCount - 1) - nodeTypeCount.get(NodeType.PV),
                  nodeTypeCount.get(NodeType.PV))
              : null;

      jacobianMatrix = buildReducedJacobianMatrix(v);

      /* Invert the Jacobian matrix and get the solution */
      RealVector solution;
      try {
        solution = solveSystemOfEquations(jacobianMatrix, delta);
      } catch (MatrixSingularException em) {
        logger.error("Error during reduction of reduced Jacobian matrix. Matrix is singular. ", em);
        solution = null;
        evaluation.addLast(new NewtonRaphsonIterationEvaluation(iterationCount, false));
        iterationCount = maxIterations;
      } catch (RuntimeException e) {
        logger.error("Error during solving of system of equations. ", e);
        solution = null;
        evaluation.addLast(new NewtonRaphsonIterationEvaluation(iterationCount, false));
        iterationCount = maxIterations;
      }

      if (solution != null) {
        v = applyCorrectionToVoltages(v, solution);

        /* sanity checks for deltaPred and deltaQred */
        for (int cnt = 0; cnt < deltaPred.getDimension(); cnt++) {
          if (Double.isNaN(deltaPred.getEntry(cnt))) {
            calcEnd = System.currentTimeMillis();
            throw new PowerFlowException(
                "deltaPred contains a NaN value at position "
                    + cnt
                    + ". How could this have happened?!",
                new NewtonRaphsonEvaluation(
                    false,
                    evaluation,
                    epsilon,
                    Duration.of(calcEnd - calcStart, ChronoUnit.MILLIS)));
          }
        }
        for (int cnt = 0; cnt < deltaQred.getDimension(); cnt++) {
          if (Double.isNaN(deltaQred.getEntry(cnt))) {
            calcEnd = System.currentTimeMillis();
            throw new PowerFlowException(
                "deltaQred contains a NaN value at position "
                    + cnt
                    + ". How could this have happened?!",
                new NewtonRaphsonEvaluation(
                    false,
                    evaluation,
                    epsilon,
                    Duration.of(calcEnd - calcStart, ChronoUnit.MILLIS)));
          }
        }

        /* Increment the iteration count and check for convergence */
        divergent = deltaPred.getLInfNorm() >= epsilon || deltaQred.getLInfNorm() >= epsilon;
        RealVector deltaF = solution.getSubVector(0, nodeCount - 1);
        RealVector deltaE = solution.getSubVector(nodeCount - 1, nodeCount - 1);
        evaluation.addLast(
            new NewtonRaphsonIterationEvaluation(
                iterationCount,
                true,
                deltaPred.getLInfNorm(),
                deltaQred.getLInfNorm(),
                deltaV2Red != null ? deltaV2Red.getLInfNorm() : Double.POSITIVE_INFINITY,
                deltaE.getLInfNorm(),
                deltaF.getLInfNorm()));
        iterationCount += 1;
      }
    }

    if (!divergent) {
      logger.info(
          "Newton Raphson power flow did converge after "
              + iterationCount
              + " iterations with epsilon = "
              + epsilon);
    } else {
      logger.info(
          "Newton Raphson power flow did NOT converge after "
              + iterationCount
              + " iterations with epsilon = "
              + epsilon);
    }
    calcEnd = System.currentTimeMillis();
    return new PowerFlowResultJava<>(
        !divergent,
        v,
        iteratedS,
        new NewtonRaphsonEvaluation(
            !divergent, evaluation, epsilon, Duration.of(calcEnd - calcStart, ChronoUnit.MILLIS)),
        iteratedS[sl]);
  }

  /**
   * Build the jacobian matrix according to the commonly known rules and reduce it by eliminating
   * the row and column of the reference node
   *
   * @param v Current complex nodal voltages
   * @return A {@link RealMatrix} as the jacobian matrix
   */
  private RealMatrix buildReducedJacobianMatrix(Complex[] v) {
    RealMatrix jMatrix = new Array2DRowRealMatrix(2 * (nodeCount - 1), 2 * (nodeCount - 1));
    double[][] j1 = new double[nodeCount][nodeCount];
    double[][] j2 = new double[nodeCount][nodeCount];
    double[][] j3 = new double[nodeCount][nodeCount];
    double[][] j4 = new double[nodeCount][nodeCount];
    double[][] j5 = new double[nodeCount][nodeCount];
    double[][] j6 = new double[nodeCount][nodeCount];
    /* Build the partial matrices but do not reduce them! */
    for (int i = 0; i < nodeCount; i++) {
      double j1Hilf = 0, j4Hilf = 0, j2Hilf = 0, j3Hilf = 0;
      for (int j = 0; j < nodeCount; j++) {
        double gij = admittanceMatrix.getEntry(i, j).getReal();
        double bij = admittanceMatrix.getEntry(i, j).getImaginary();
        double ei = v[i].getReal();
        double ej = v[j].getReal();
        double fi = v[i].getImaginary();
        double fj = v[j].getImaginary();
        j1[i][j] = -ei * bij + fi * gij; // dPi/dfj
        j2[i][j] = ei * gij + fi * bij; // dPi/dej
        j3[i][j] = -fi * bij - ei * gij; // dQi/dfj
        j4[i][j] = fi * gij - ei * bij; // dQi/dej
        j5[i][j] = 0; // dU²i/dfj
        j6[i][j] = 0; // dU²i/dej
        if (i != j) {
          j1Hilf += fj * gij + ej * bij;
          j2Hilf += ej * gij - fj * bij;
          j3Hilf += ej * gij - fj * bij;
          j4Hilf += -(fj * gij + ej * bij);
        } else {
          j1[i][j] = 2 * fi * gij;
          j2[i][j] = 2 * ei * gij;
          j3[i][j] = -2 * fi * bij;
          j4[i][j] = -2 * ei * bij;
          j5[i][j] = 2 * fi;
          j6[i][j] = 2 * ei;
        }
      }
      j1[i][i] += j1Hilf;
      j2[i][i] += j2Hilf;
      j3[i][i] += j3Hilf;
      j4[i][i] += j4Hilf;
    }
    /* Reduce the partial matrices */
    double[][] j1a = new double[nodeCount - 1][nodeCount - 1];
    double[][] j2a = new double[nodeCount - 1][nodeCount - 1];
    double[][] j3a = new double[nodeCount - 1 - nodeTypeCount.get(NodeType.PV)][nodeCount - 1];
    double[][] j4a = new double[nodeCount - 1 - nodeTypeCount.get(NodeType.PV)][nodeCount - 1];
    int z = 0;
    for (int i = 0; i < nodeCount; i++) {
      if (!nodeTypes[i].equals(NodeType.SL)) {
        int y = 0;
        for (int j = 0; j < nodeCount; j++) {
          if (!nodeTypes[j].equals(NodeType.SL)) {
            j1a[z][y] = j1[i][j];
            j2a[z][y] = j2[i][j];
            y += 1;
          }
        }
        z += 1;
      }
    }
    z = 0;
    for (int i = 0; i < nodeCount; i++) {
      if (nodeTypes[i].equals(NodeType.PQ)) {
        int y = 0;
        for (int j = 0; j < nodeCount; j++) {
          if (!nodeTypes[j].equals(NodeType.SL)) {
            j3a[z][y] = j3[i][j];
            j4a[z][y] = j4[i][j];
            y += 1;
          }
        }
        z += 1;
      }
    }
    /* Assemble the full jacobian matrix */
    jMatrix.setSubMatrix(j1a, 0, 0);
    jMatrix.setSubMatrix(j2a, 0, nodeCount - 1);
    jMatrix.setSubMatrix(j3a, nodeCount - 1, 0);
    jMatrix.setSubMatrix(j4a, nodeCount - 1, nodeCount - 1);
    if (nodeTypeCount.get(NodeType.PV) > 0) {
      double[][] j5a = new double[nodeTypeCount.get(NodeType.PV)][nodeCount - 1];
      double[][] j6a = new double[nodeTypeCount.get(NodeType.PV)][nodeCount - 1];
      z = 0;
      for (int i = 0; i < nodeCount; i++) {
        if (nodeTypes[i].equals(NodeType.PV)) {
          int y = 0;
          for (int j = 0; j < nodeCount; j++) {
            if (!nodeTypes[j].equals(NodeType.SL)) {
              j5a[z][y] = j5[i][j];
              j6a[z][y] = j6[i][j];
              y += 1;
            }
          }
          z += 1;
        }
      }
      jMatrix.setSubMatrix(j5a, nodeCount - 1 + nodeCount - 1 - nodeTypeCount.get(NodeType.PV), 0);
      jMatrix.setSubMatrix(
          j6a, nodeCount - 1 + nodeCount - 1 - nodeTypeCount.get(NodeType.PV), nodeCount - 1);
    }

    /* Register, that the changes in admittance matrix have been "incorporated" in the Jacobian matrix */
    admittanceMatrixChanged = false;

    return jMatrix;
  }

  /**
   * Solve the system of euqations by inverting the Jacobian Matrix and multiplying the delta vector
   * to it. TODO: Iterativer solver oder andere Datenstrukturen könnten noch effizienter sein.
   * Ausprobieren.
   *
   * @param jacobianMatrix Jacobian Matrix as {@link RealMatrix}
   * @param delta {@link RealVector} of correctional values
   * @return An {@link ArrayRealVector} of corrections to the nodal voltages. Upper half:
   *     Corrections to real part, lower half: to imaginary part
   * @throws RuntimeException If e.g. the matrix cannot be inverted (possibly singular?!)
   */
  protected ArrayRealVector solveSystemOfEquations(RealMatrix jacobianMatrix, RealVector delta)
      throws RuntimeException {
    return new ArrayRealVector(
        DenseLU.factorize(new DenseMatrix(jacobianMatrix.getData()))
            .solve(new DenseMatrix(new DenseVector(delta.toArray())))
            .getData());
  }

  /**
   * Builds the reduced deviation vector to be used in {@link
   * PowerFlowNR#solveSystemOfEquations(RealMatrix, RealVector)}
   *
   * @param deltaS Array of {@link Complex} deviation between estimated and actual power consumption
   *     at the nodes
   * @param deltaV2 Array of deviation between estimated and actual squared nodal voltage magnitudes
   * @return The assembled and reduced vector of deviations as the combination of active and
   *     reactive deviation as well as deviation of squared voltage magnitudes
   * @throws PowerFlowException If there is no vector of squared voltage magnitude deviations are
   *     available
   */
  protected RealVector reduceDeviationVector(Complex[] deltaS, double[] deltaV2)
      throws PowerFlowException {
    int cntNoSl = 0, cntPQ = 0, cntPV = 0;
    RealVector deltaPred = new ArrayRealVector(nodeCount - 1);
    RealVector deltaQred = new ArrayRealVector(nodeCount - 1 - nodeTypeCount.get(NodeType.PV));
    RealVector deltaV2Red = null;
    if (nodeTypeCount.get(NodeType.PV) > 0) {
      deltaV2Red = new ArrayRealVector(nodeTypeCount.get(NodeType.PV));
    }
    for (int cntNode = 0; cntNode < nodeCount; cntNode++) {
      if (nodeTypes[cntNode] != NodeType.SL) {
        deltaPred.setEntry(cntNoSl, deltaS[cntNode].getReal());
        cntNoSl += 1;
      }
      if (nodeTypes[cntNode] == NodeType.PQ) {
        deltaQred.setEntry(cntPQ, deltaS[cntNode].getImaginary());
        cntPQ += 1;
      } else if (nodeTypes[cntNode] == NodeType.PV) {
        if (deltaV2 == null) {
          throw new PowerFlowException(
              "The vector of squared voltage magnitude deviation may not be null!");
        }
        if (deltaV2Red == null) {
          throw new PowerFlowException(
              "The vector of squared voltage magnitude deviation may not be null!");
        }
        deltaV2Red.setEntry(cntPV, deltaV2[cntNode]);
        cntPV += 1;
      }
    }

    /* Create difference vectors and assemble them */
    RealVector delta = deltaPred.append(deltaQred);
    if (nodeTypeCount.get(NodeType.PV) > 0) {
      delta = delta.append(deltaV2Red);
    }
    return delta;
  }

  /**
   * Apply the corrections in nodal voltage to the nodal voltages
   *
   * @param v Previous nodal voltages
   * @param deltaEF Composed vector of corrections
   * @return Corrected nodal voltages
   */
  protected Complex[] applyCorrectionToVoltages(Complex[] v, RealVector deltaEF) {
    Complex[] corrected = Arrays.copyOf(v, v.length);

    RealVector deltaF = deltaEF.getSubVector(0, nodeCount - 1);
    RealVector deltaE = deltaEF.getSubVector(nodeCount - 1, nodeCount - 1);

    /* Build the new voltage vector */
    int cntNoSl = 0;
    for (int cntNode = 0; cntNode < nodeCount; cntNode++) {
      if (!nodeTypes[cntNode].equals(NodeType.SL)) {
        corrected[cntNode] =
            corrected[cntNode].subtract(
                new Complex(deltaE.getEntry(cntNoSl), deltaF.getEntry(cntNoSl)));
        cntNoSl += 1;
      }
    }
    return corrected;
  }
}
