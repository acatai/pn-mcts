package mcts;

import game.Game;
import main.collections.FastArrayList;
import other.AI;
import other.RankUtils;
import other.context.Context;
import other.move.Move;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PNSMCTS_L2 extends AI {


    public static boolean FIN_MOVE_SEL = true;
    public static int SOLVERLIKE_MINVISITS = Integer.MAX_VALUE; // 5; // Integer.MAX_VALUE;
    public static int counter = 0;
    public static double CONTEMPT_FACTOR = -100; // -0.1; 0.0; 0.1; 0.2; ... ?

    //-------------------------------------------------------------------------

    /**
     * Our player index
     */
    protected int player = -1;

    /**
     * Settings contain (in order): pnConstant, explorationConstant, time per turn
     */
    private static double[] settings;

    // Used to count simulations per second
    private static double sims = 0;
    private static double simsThisTurn = 0;
    private static double turns = 0;

    //-------------------------------------------------------------------------

    /**
     * Constructor
     */
    public PNSMCTS_L2() {
        this.friendlyName = "PNS_L2 UCT";
        double[] defaultSettings = {1.0, Math.sqrt(2), 1.0}; // PN-Constant, MCTS-Constant, Time per turn
        this.settings = defaultSettings;
    }

    public PNSMCTS_L2(double[] settings) {
        this.friendlyName = "PNS_L2 UCT";
        this.settings = settings;
    }

    public PNSMCTS_L2(boolean finMove, int minVisits, double pnCons, double contemptFactor) {
        this.FIN_MOVE_SEL = finMove;
        this.SOLVERLIKE_MINVISITS = minVisits;
        this.CONTEMPT_FACTOR = contemptFactor; // applies only if FIN_MOVE_SEL is true
        this.friendlyName = "PNS_L2 UCT";
        double[] defaultSettings = {pnCons, Math.sqrt(2), 1.0}; // PN-Constant, MCTS-Constant, Time per turn
        this.settings = defaultSettings;
        this.counter = 0;
    }

    public void resetCounter() {
        counter = 0;
    }


    //-------------------------------------------------------------------------

    @Override
    public Move selectAction(
            final Game game,
            final Context context,
            final double maxSeconds,
            final int maxIterations,
            final int maxDepth
    ) {
        this.simsThisTurn = this.sims;
        this.turns++;
        // Start out by creating a new root node (no tree reuse in this example)
        final Node root = new Node(null, null, context, player);

        // We'll respect any limitations on max seconds and max iterations (don't care about max depth)
        final long stopTime = (maxSeconds > 0.0) ? System.currentTimeMillis() + (long) (maxSeconds * 1000L) : Long.MAX_VALUE;
        final int maxIts = (maxIterations >= 0) ? maxIterations : Integer.MAX_VALUE;

        int numIterations = 0;

        // Our main loop through MCTS iterations
        while (
                numIterations < maxIts &&                    // Respect iteration limit
                        System.currentTimeMillis() < stopTime &&    // Respect time limit
                        !wantsInterrupt                                // Respect GUI user clicking the pause button
        ) {
            // Start in root node
            Node current = root;

            // Traverse tree
            while (true) {
                if (current.context.trial().over()) {
                    // We've reached a terminal state
                    break;
                }

                current = select(current);

                if (current.visitCount == 0) {
                    // We've expanded a new node, time for playout!
                    break;
                }
            }

            Context contextEnd = current.context;

            if (!contextEnd.trial().over()) {
                // Run a playout if we don't already have a terminal game state in node
                contextEnd = new Context(contextEnd);
                game.playout
                        (
                                contextEnd,
                                null,
                                -1.0,
                                null,
                                0,
                                -1,
                                ThreadLocalRandom.current()
                        );
                sims++;
            }

            // This computes utilities for all players at the of the playout,
            // which will all be values in [-1.0, 1.0]
            final double[] utilities = RankUtils.utilities(contextEnd);

            // Backpropagate utilities through the tree
            boolean changed = true;
            boolean firstNode = true;
            while (current != null) {
                current.visitCount += 1;
                for (int p = 1; p <= game.players().count(); ++p) {
                    current.scoreSums[p] += utilities[p];
                }
                if (!firstNode) {
                    if (changed) {
                        changed = current.setProofAndDisproofNumbers();
                        if (current.getChildren().size() > 0) {
                            current.setChildRanks();
                        }
                    }
                } else {
                    firstNode = false;
                }

                current = current.parent;
            }
            // if proofNum of root changed -> check is proven or disproven
            // if (changed) {
                // for (Node child : root.children) {
                    // if root is proven -> stop searching
                    // if (child.proofNum == 0) { // causes problems with robust child final move selection
                    //     return finalMoveSelection(root);
                    // }
                // }
            // }

            // Increment iteration count
            ++numIterations;
        }

        // Return the move we wish to play
        return finalMoveSelection(root);
    }

    /**
     * Selects child of the given "current" node according to UCT-PN equation.
     * This method also implements the "Expansion" phase of MCTS, and creates
     * new nodes if the given current node has unexpanded moves.
     *
     * @param current
     * @return Selected node (if it has 0 visits, it will be a newly-expanded node).
     */
    public static Node select(final Node current) {
        // All child nodes are created and added to the child list of the current node
        if (!current.expanded) {
            return current.developNode();
        }

        // Don't use UCT-PN until all nodes have been visited once
        if (current.getUnexpandedChildren().size() > 0) {
            return current.getUnexpandedChildren().remove(ThreadLocalRandom.current().nextInt(current.unexpandedChildren.size()));
        }

        // use UCT-PN equation to select from all children, with random tie-breaking
        Node bestChild = null;
        double bestValue = Double.NEGATIVE_INFINITY;
        int numBestFound = 0;

        double explorationConstant = settings[1];

        double pnConstant = settings[0];
        double total = current.getChildren().size();

        final int numChildren = current.children.size();
        final int mover = current.context.state().mover();

        for (int i = 0; i < numChildren; ++i) {
            final Node child = current.children.get(i);

            // sanity check
//            if (child.proofNum == 0 && child.proofNumL2 != 0) System.err.println("L1 proof 0 but L2 not!!!");
//            if (child.disproofNumL2 == 0 && child.disproofNum != 0) System.err.println("L2 disproof 0 but L1 not!!!");


            // original solver
//            if (current.proofNum != 0 && current.disproofNum != 0) {
//                if (child.proofNum == 0 && child.visitCount > SOLVERLIKE_MINVISITS) continue;
//                if (child.disproofNum == 0 && child.visitCount > SOLVERLIKE_MINVISITS) continue;
//            }

            // slightly modified 2Level solver
            if (current.proofNumL2 != 0 && current.disproofNum != 0) {
                if (child.proofNumL2 == 0 && child.visitCount > SOLVERLIKE_MINVISITS) continue; // win or draw
                if (child.disproofNum == 0 && child.visitCount > SOLVERLIKE_MINVISITS) continue; // lose or draw
            }

            final double exploit = child.scoreSums[mover] / child.visitCount;
            final double explore = Math.sqrt((Math.log(current.visitCount)) / child.visitCount); //UCT with changeable exploration constant
            final double pnEffect = 1 - (child.getRank() / total); // This formula assures that the node with lowest rank (best node) has the highest pnEffect

            // UCT-PN Formula
            final double uctValue = exploit + (explorationConstant * explore) + (pnConstant * pnEffect);

            if (uctValue > bestValue) {
                bestValue = uctValue;
                bestChild = child;
                numBestFound = 1;
            } else if (uctValue == bestValue && ThreadLocalRandom.current().nextInt() % ++numBestFound == 0) {
                // this case implements random tie-breaking
                bestChild = child;
            }
        }
        return bestChild;
    }

    /**
     * Selects the move we wish to play using the "Robust Child" strategy
     * (meaning that we play the move leading to the child of the root node
     * with the highest visit count).
     *
     * @param rootNode
     * @return Final move as selected by PN-MCTS
     */
    public static Move finalMoveSelection(final Node rootNode) {
        Node bestChild = null;
        int bestVisitCount = Integer.MIN_VALUE;
        int numBestFound = 0;

        final int numChildren = rootNode.children.size();

        for (int i = 0; i < numChildren; ++i) {
            final Node child = rootNode.children.get(i);
            final int visitCount = child.visitCount;

            if (visitCount > bestVisitCount) {
                bestVisitCount = visitCount;
                bestChild = child;
                numBestFound = 1;
            } else if (visitCount == bestVisitCount && ThreadLocalRandom.current().nextInt() % ++numBestFound == 0) {
                // this case implements random tie-breaking
                bestChild = child;
            }
        }

        // To ensure a proven node will select the proven child too
        if (FIN_MOVE_SEL) {
            //System.out.println("XXXXXXX");
            if (rootNode.proofNum == 0) {
                for (Node child : rootNode.children) {
                    if (child.proofNum == 0) {
                        bestChild = child;
                        break;
                    }
                }
            } else if (rootNode.proofNumL2 == 0) { // Level 2 check
                double rootscore = rootNode.scoreSums[rootNode.context.state().mover()] / rootNode.visitCount;
                //System.out.println("Can prove draw (not win), root score " + rootscore);
                if (rootscore <= CONTEMPT_FACTOR) {
                    ++counter;
                    // uncomment line below for verbose!
                    //System.out.println("FinMoveSel proven DRAW, rootscore: " + rootscore);
                    for (Node child : rootNode.children) {
                        if (child.proofNumL2 == 0) {
                            bestChild = child;
                            break;
                        }
                    }
                }
            }
            //System.out.println("rootscore: " + rootNode.scoreSums[rootNode.context.state().mover()] / rootNode.visitCount);
        }

        return bestChild.moveFromParent;
    }

    @Override
    public void initAI(final Game game, final int playerID) {
        this.player = playerID;
    }

    @Override
    // Notifies Ludii if the game is playable by PN-MCTS
    public boolean supportsGame(final Game game) {
        if (game.isStochasticGame())
            return false;

        if (!game.isAlternatingMoveGame())
            return false;

        return true;
    }

    //-------------------------------------------------------------------------

    /**
     * Inner class for nodes used by example UCT
     *
     * @author Dennis Soemers
     */
    private static class Node implements Comparable<Node> {

        /**
         * Our parent node
         */
        private final Node parent;

        /**
         * The move that led from parent to this node
         */
        private final Move moveFromParent;

        /**
         * This objects contains the game state for this node (this is why we don't support stochastic games)
         */
        private final Context context;

        /**
         * Visit count for this node
         */
        private int visitCount = 0;

        /**
         * For every player, sum of utilities / scores backpropagated through this node
         */
        private final double[] scoreSums;

        /**
         * Child nodes
         */
        private final List<Node> children = new ArrayList<Node>();

        /**
         * List of moves for which we did not yet create a child node
         */
        private final FastArrayList<Move> unexpandedMoves;

        private final List<Node> unexpandedChildren = new ArrayList<Node>();

        /**
         * Flag to keep track of if a node has expanded its children yet
         */
        private boolean expanded = false;

        /**
         * Proof and Disproof number of current node
         */
        private double proofNum;
        private double disproofNum;

        /**
         * Proof and Disproof number Level 2
         */
        private double proofNumL2;
        private double disproofNumL2;

        // tutaj dodać i w 393: public void evaluate() dodać obsługę nowych z różnicą zachowania w przypadku remisu >= 0.5 zamiast == 1.0
        // zmienić compareTo(Node), zmienić to w: PNSMCTS_Extension2

        /**
         * Rank of a node compared to "siblings". Needed for UCT-PN. Ranks ordered best to worst
         */
        private int rank;

        /**
         * Various necessary information variables.
         */
        private final PNSNodeTypes type;

        private PNSNodeValues value;

        public final int proofPlayer;

        public enum PNSNodeTypes {
            /**
             * An OR node
             */
            OR_NODE,

            /**
             * An AND node
             */
            AND_NODE
        }

        /**
         * Values of nodes in search trees in PNS
         */
        public enum PNSNodeValues {
            /**
             * A proven node
             */
            TRUE,

            /**
             * A disproven node
             */
            FALSE,

            /**
             * A disproven node
             */
            DRAW,

            /**
             * Unknown node (yet to prove or disprove)
             */
            UNKNOWN
        }


        /**
         * Constructor
         *
         * @param parent
         * @param moveFromParent
         * @param context
         */
        public Node(final Node parent, final Move moveFromParent, final Context context, final int proofPlayer) {
            this.parent = parent;
            this.moveFromParent = moveFromParent;
            this.context = context;
            final Game game = context.game();
            this.proofPlayer = proofPlayer;
            scoreSums = new double[game.players().count() + 1];
            // Set node type
            if (context.state().mover() == proofPlayer) {
                this.type = PNSNodeTypes.OR_NODE;
            } else {
                this.type = PNSNodeTypes.AND_NODE;
            }
            evaluate();
            setProofAndDisproofNumbers();

            // For simplicity, we just take ALL legal moves.
            // This means we do not support simultaneous-move games.
            unexpandedMoves = new FastArrayList<Move>(game.moves(context).moves());

            if (parent != null)
                parent.children.add(this);
        }

        /**
         * Evaluates a node as in PNS according to L. V. Allis' "Searching for Solutions in Games and Artificial Intelligence"
         */
        public void evaluate() {
            if (this.context.trial().over()) {
                if (RankUtils.utilities(this.context)[proofPlayer] == 1.0) {
                    this.value = PNSNodeValues.TRUE;
                } else if (RankUtils.utilities(this.context)[proofPlayer] >= 0.0) {
                    this.value = PNSNodeValues.DRAW;
                } else {
                    this.value = PNSNodeValues.FALSE;
                }
            } else {
                this.value = PNSNodeValues.UNKNOWN;
            }
        }


        /**
         * Sets the proof and disproof values of the current node as it is done for PNS in L. V. Allis' "Searching for
         * Solutions in Games and Artificial Intelligence". Set differently depending on if the node has children yet.
         *
         * @return Returns true if something was changed and false if not. Used to improve PN-MCTS speed
         */
        public boolean setProofAndDisproofNumbers() {
            // If this node has child nodes
            if (this.expanded) {
                if (this.type == PNSNodeTypes.AND_NODE) {
                    double proof = 0;
                    double proofL2 = 0;
                    for (int i = 0; i < this.children.size(); i++) {
                        proof += this.children.get(i).getProofNum();
                        proofL2 += this.children.get(i).getProofNumL2();
                    }
                    double disproof = Double.POSITIVE_INFINITY;
                    double disproofL2 = Double.POSITIVE_INFINITY;
                    for (int i = 0; i < this.children.size(); i++) {
                        if (this.children.get(i).getDisproofNum() < disproof) {
                            disproof = this.children.get(i).getDisproofNum();
                        }
                        if (this.children.get(i).getDisproofNumL2() < disproofL2) {
                            disproofL2 = this.children.get(i).getDisproofNumL2();
                        }
                    }
                    //If nothing changed return false
                    if (this.proofNum == proof && this.proofNumL2 == proofL2 && this.disproofNum == disproof && this.disproofNumL2 == disproofL2) {
                        return false;
                    } else {
                        this.proofNum = proof;
                        this.disproofNum = disproof;
                        this.proofNumL2 = proofL2;
                        this.disproofNumL2 = disproofL2;
                        return true;
                    }
                } else if (this.type == PNSNodeTypes.OR_NODE) {
                    double disproof = 0;
                    double disproofL2 = 0;
                    for (int i = 0; i < this.children.size(); i++) {
                        disproof += this.children.get(i).getDisproofNum();
                        disproofL2 += this.children.get(i).getDisproofNumL2();
                    }
                    double proof = Double.POSITIVE_INFINITY;
                    double proofL2 = Double.POSITIVE_INFINITY;
                    for (int i = 0; i < this.children.size(); i++) {
                        if (this.children.get(i).getProofNum() < proof) {
                            proof = this.children.get(i).getProofNum();
                        }
                        if (this.children.get(i).getProofNumL2() < proofL2) {
                            proofL2 = this.children.get(i).getProofNumL2();
                        }
                    }
                    //If nothing changed return false
                    if (this.proofNum == proof && this.proofNumL2 == proofL2 && this.disproofNum == disproof && this.disproofNumL2 == disproofL2) {
                        return false;
                    } else {
                        this.proofNum = proof;
                        this.disproofNum = disproof;
                        this.proofNumL2 = proofL2;
                        this.disproofNumL2 = disproofL2;
                        return true;
                    }
                }
            } else if (!this.expanded) {
                // (Dis)proof numbers are set according to evaluation until properly checked
                if (this.value == PNSNodeValues.FALSE) {
                    this.proofNum = Double.POSITIVE_INFINITY;
                    this.disproofNum = 0;
                    this.proofNumL2 = Double.POSITIVE_INFINITY;
                    this.disproofNumL2 = 0;
                } else if (this.value == PNSNodeValues.DRAW) {
                    this.proofNum = Double.POSITIVE_INFINITY;
                    this.disproofNum = 0;
                    this.proofNumL2 = 0;
                    this.disproofNumL2 = Double.POSITIVE_INFINITY;
                } else if (this.value == PNSNodeValues.TRUE) {
                    this.proofNum = 0;
                    this.disproofNum = Double.POSITIVE_INFINITY;
                    this.proofNumL2 = 0;
                    this.disproofNumL2 = Double.POSITIVE_INFINITY;
                } else if (this.value == PNSNodeValues.UNKNOWN) {
                    this.proofNum = 1;
                    this.disproofNum = 1;
                    this.proofNumL2 = 1;
                    this.disproofNumL2 = 1;
                }
            }
            //If we haven't expanded yet it will definitely be changed so return true
            return true;
        }

        /**
         * Develops a node by adding all the children nodes. Then returns one child at random for the selection phase.
         *
         * @return One of the new child nodes
         */
        public Node developNode() {
            if (this.value == PNSNodeValues.UNKNOWN) {
                for (int i = 0; i < this.unexpandedMoves.size(); i++) {
                    final Move move = this.unexpandedMoves.get(i);
                    final Context context = new Context(this.context);
                    context.game().apply(context, move);
                    Node node = new Node(this, move, context, this.proofPlayer);
                    unexpandedChildren.add(node);
                }
                this.expanded = true;
                //this.setProofAndDisproofNumbers();
                return this.unexpandedChildren.remove(ThreadLocalRandom.current().nextInt(this.unexpandedChildren.size()));
            } else {
                this.expanded = true;
                return this;
            }
        }

        /**
         * Set an ordered ranking for the UCT-PN formula in the selection step of MCTS
         */

        public void setChildRanks() {
            List<Node> sorted = new ArrayList<Node>(this.children);
            Collections.sort(sorted);
            Node lastNode = null;
            for (int i = 0; i < sorted.size(); i++) {
                Node child = sorted.get(i);
                // If there's a tie
                if (lastNode != null && this.type == PNSNodeTypes.OR_NODE && lastNode.getProofNum() == child.getProofNum()) {
                    child.setRank(lastNode.getRank());
                    // If there's a tie
                } else if (lastNode != null && this.type == PNSNodeTypes.AND_NODE && lastNode.getDisproofNum() == child.getDisproofNum()) {
                    child.setRank(lastNode.getRank());
                } else {
                    child.setRank(i + 1);
                }
                lastNode = child;
            }
        }

        public List<Node> getChildren() {
            return children;
        }

        public double getProofNum() {
            return proofNum;
        }

        public double getProofNumL2() {
            return proofNumL2;
        }

        public double getDisproofNum() {
            return disproofNum;
        }

        public double getDisproofNumL2() {
            return disproofNumL2;
        }

        public PNSNodeTypes getType() {
            return type;
        }

        public int getRank() {
            return rank;
        }

        public List<Node> getUnexpandedChildren() {
            return unexpandedChildren;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

        // Used to rank children
        @Override
        public int compareTo(Node o) {
            if (this.parent.getType() == PNSNodeTypes.OR_NODE) {
                if (this.getProofNum() < o.getProofNum()) {
                    return -1;
                } else if (this.getProofNum() > o.getProofNum()) {
                    return 1;
                } else if (this.getProofNumL2() < o.getProofNumL2()) {
                    return -1;
                } else if (this.getProofNumL2() > o.getProofNumL2()) {
                    return 1;
                } else {
                    return 0;
                }
            } else if (this.parent.getType() == PNSNodeTypes.AND_NODE) {
                if (this.getDisproofNum() < o.getDisproofNum()) {
                    return -1;
                } else if (this.getDisproofNum() > o.getDisproofNum()) {
                    return 1;
                } else if (this.getDisproofNumL2() < o.getDisproofNumL2()) {
                    return -1;
                } else if (this.getDisproofNumL2() > o.getDisproofNumL2()) {
                    return 1;
                } else {
                    return 0;
                }
            }
            return 0;
        }
    }

    //-------------------------------------------------------------------------


}
