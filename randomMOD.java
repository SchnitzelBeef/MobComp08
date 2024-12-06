package layer2_802Algorithms;

import plot.JEMultiPlotter;
import layer2_80211Mac.JE802_11Mac;
import layer2_80211Mac.JE802_11MacAlgorithm;
import layer2_80211Mac.JE802_11BackoffEntity;
import statistics.JERandomVar;

public class randomMOD extends JE802_11MacAlgorithm {
	
	private JE802_11BackoffEntity theBackoffEntityAC01;

	private int stepper = 0;

	private int r_min_val = 0;
	private int r_max_val = 1;

	private boolean cooperating;

	private JERandomVar theRandomVar;

	public randomMOD(String name, JE802_11Mac mac) {
		super(name, mac);
		this.theRandomVar = new JERandomVar(this.theUniqueRandomGenerator, "Uniform", this.r_min_val, this.r_max_val);
		this.theBackoffEntityAC01 = this.mac.getBackoffEntity(1);
		this.mac.getPhy().setCurrentPhyMode("64QAM34");
		message("This is station " + this.dot11MACAddress.toString() +". RANDOM MOD algorithm: '" + this.algorithmName + "'.", 100);

		this.cooperating = true;
		message("Now cooperating with 4, 30");
		theBackoffEntityAC01.setDot11EDCAAIFSN(4);
		theBackoffEntityAC01.setDot11EDCACWmin(30);
	}
	
	@Override
	public void compute() {
		this.stepper += 1;
		if (this.stepper > 50) {
			// Changing parameters:
			double val = this.theRandomVar.nextvalue();
			message("random number between " + this.r_min_val + " and " + this.r_max_val + ": " + this.theRandomVar.nextvalue());

			if (val <= 0.5) {
				this.cooperating = true;
				message("Now cooperating with 4, 30");
				theBackoffEntityAC01.setDot11EDCAAIFSN(4);
				theBackoffEntityAC01.setDot11EDCACWmin(30);
			} else {
				this.cooperating = false;
				message("Now deffecting with 2, 5");
				theBackoffEntityAC01.setDot11EDCAAIFSN(2);
				theBackoffEntityAC01.setDot11EDCACWmin(5);
			}
			this.stepper = 0;
		}
	}
	
	@Override
	public void plot() {
		if (plotter == null) {
			plotter = new JEMultiPlotter("First random station, Station " + this.dot11MACAddress.toString(), "current", "time [s]", "MAC Queue", this.theUniqueEventScheduler.getEmulationEnd().getTimeMs() / 1000.0, true);
			plotter.addSeries("Cooperating (high), deffecting (low)");
			plotter.display();
		}
		plotter.plot(((Double) theUniqueEventScheduler.now().getTimeMs()).doubleValue() / 1000.0, theBackoffEntityAC01.getCurrentQueueSize(), 0);
		plotter.plot(((Double) theUniqueEventScheduler.now().getTimeMs()).doubleValue() / 1000.0, this.cooperating? -.1 : -.2, 1);
	}

}
