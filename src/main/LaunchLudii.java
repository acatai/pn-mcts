package main;

import app.StartDesktopApp;
import manager.ai.AIRegistry;
import mcts.*;
import search.mcts.MCTS;

/**
 * The main method of this launches the Ludii application with its GUI, and registers
 * the example AIs from this project such that they are available inside the GUI.
 *
 * @author Dennis Soemers
 */
public class LaunchLudii
{
	/**
	 * The main method
	 * @param args
	 */
	public static void main(final String[] args)
	{
		// Register our example AIs
		if (!AIRegistry.registerAI("Example Standard UTC AI", () -> MCTS.createUCT(), (game) -> true))
			System.err.println("WARNING! Failed to register AI because one with that name already existed!");

		double[] setting = {1, Math.sqrt(2), 1};
		if (!AIRegistry.registerAI("PN UCT", () -> new PNSMCTS_L2(setting), (game) -> new PNSMCTS_L2(setting).supportsGame(game)))
			System.err.println("WARNING! Failed to register AI because one with that name already existed!");
		
		// Run Ludii
		StartDesktopApp.main(new String[0]);
	}
}
