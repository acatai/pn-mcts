package experiments;

import game.Game;
import mcts.PNSMCTS_Extension;
import other.AI;
import other.GameLoader;
import other.context.Context;
import other.model.Model;
import other.trial.Trial;
import search.mcts.MCTS;
import utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Górski
 */
public class RunCustomMatch {
    /**
     * Name of game we wish to play
     */
    static final String GAME_NAME = "games/Minishogi.lud";
//    static final String GAME_NAME = "games/Awari.lud";
//    static final String GAME_NAME = "games/Lines of Action 8x8.lud";
//    static final String GAME_NAME = "games/Knightthrough.lud";
    static final File GAME_FILE = new File(GAME_NAME);
    private static double TIME_FOR_GAME = 1.0;
    static final int NUM_GAMES = 1000;

    public static void main(final String[] args) {
        System.out.println(GAME_FILE.getAbsolutePath());

        // load and create game
        final Game game = GameLoader.loadGameFromFile(GAME_FILE);
        final Trial trial = new Trial(game);
        final Context context = new Context(game, trial);

        Map<String, Integer> results = new HashMap<>();
        int draws = 0;

        // params to test:
        boolean finMove = false;
        int minVisits = 5;
        double pnCons = 1;

        AI testedAI = new PNSMCTS_Extension(finMove, minVisits, pnCons);

        for (int gameCounter = 1; gameCounter <= NUM_GAMES; ++gameCounter) {
            List<AI> ais = new ArrayList<>();
            if (gameCounter % 2 == 0) {
                ais.add(null);
                ais.add(MCTS.createUCT());
                ais.add(testedAI);
            } else {
                ais.add(null);
                ais.add(testedAI);
                ais.add(MCTS.createUCT());
            }
            if (gameCounter == 1) {
                results.put("MCTS", 0);
                results.put("PNSMCTS_Extension", 0);
            }
            game.start(context);
            for (int p = 1; p < ais.size(); ++p) {
                ais.get(p).initAI(game, p);
            }
            final Model model = context.model();
            while (!context.trial().over()) {
                model.startNewStep(context, ais, TIME_FOR_GAME);
            }
            int winner = context.trial().status().winner();
            if (winner > 0) {
                if (gameCounter % 2 == winner % 2) {
                    results.put("PNSMCTS_Extension", results.get("PNSMCTS_Extension") + 1);
                } else {
                    results.put("MCTS", results.get("MCTS") + 1);
                }
            } else
                ++draws;
            System.out.print(GAME_NAME + ": " + TIME_FOR_GAME + ": " + gameCounter + ": " + finMove + ", " + minVisits + ", " + pnCons + ": ");
            for (String algoName : results.keySet()) {
                System.out.print(algoName + ": " + results.get(algoName) + " RATIO: " + Utils.ratio(results.get(algoName), gameCounter - draws) + ", ");
            }
            System.out.println();
        }
    }
}
