package layer2_802Algorithms;

import layer2_80211Mac.JE802_11Mac;
import layer2_80211Mac.JE802_11MacAlgorithm;
import layer2_80211Mac.JE802_11BackoffEntity;
import statistics.JERandomVar;

public class randomMOD extends JE802_11MacAlgorithm {
	
	private JE802_11BackoffEntity theBackoffEntityAC01;

	private int stepper = 0;

	private int r_min_val = 0;
	private int r_max_val = 1;

	private JERandomVar theRandomVar;

	public randomMOD(String name, JE802_11Mac mac) {
		super(name, mac);
		this.theRandomVar = new JERandomVar(this.theUniqueRandomGenerator, "Uniform", this.r_min_val, this.r_max_val);
		this.theBackoffEntityAC01 = this.mac.getBackoffEntity(1);
		message("This is station " + this.dot11MACAddress.toString() +". RANDOM MOD algorithm: '" + this.algorithmName + "'.", 100);
	}
	
	@Override
	public void compute() {
		this.stepper += 1;
		if (this.stepper > 500) {
			// Changing parameters:
			double val = this.theRandomVar.nextvalue();
			message("random number between " + this.r_min_val + " and " + this.r_max_val + ": " + this.theRandomVar.nextvalue());

			if (val <= 0.5) {
				message("Now cooperating with 15, 2");
				theBackoffEntityAC01.setDot11EDCAAIFSN(15);
				theBackoffEntityAC01.setDot11EDCACWmin(2);
			} else {
				message("Now deffecting with 1, 1");
				theBackoffEntityAC01.setDot11EDCAAIFSN(1);
				theBackoffEntityAC01.setDot11EDCACWmin(1);
			}
			this.stepper = 0;
		}
	}
	
	@Override
	public void plot() {
		// TODO Auto-generated method stub
		
	}

}
