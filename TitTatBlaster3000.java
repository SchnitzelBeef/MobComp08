package layer2_802Algorithms;

import plot.JEMultiPlotter;
import layer1_802Phy.JE802PhyMode;
import layer2_80211Mac.JE802_11BackoffEntity;
import layer2_80211Mac.JE802_11Mac;
import layer2_80211Mac.JE802_11MacAlgorithm;
import java.util.Random;


public class TitTatBlaster3000 extends JE802_11MacAlgorithm {
	
	private JE802_11BackoffEntity theBackoffEntityAC01;
	
	private double theSamplingTime_sec;

	private PIDController pid_controller;
	
	private int forgive_counter;

	private double aCurrentQueueSize;

	private Random rand;

	public TitTatBlaster3000(String name, JE802_11Mac mac) {
		super(name, mac);
		this.rand = new Random();
		this.theBackoffEntityAC01 = this.mac.getBackoffEntity(1);
		message("This is station " + this.dot11MACAddress.toString() +". Tit-Tat Blaster 3000 up and blastin' algorithm: '" + this.algorithmName + "'.", 100);
		this.mac.getPhy().setCurrentTransmitPower_dBm(0);
		this.mac.getPhy().setCurrentPhyMode("64QAM34");
		theBackoffEntityAC01.setDot11EDCAAIFSN(8);
		theBackoffEntityAC01.setDot11EDCACWmin(32);
		this.pid_controller = new PIDController(-0.2, 0.002, 0.5, 0.001);
		this.forgive_counter = 0;
	}


	private void cooperate(int AIFSN, int CWmin) {	
		//Standard cooperate settings:
		theBackoffEntityAC01.setDot11EDCAAIFSN(8);
		theBackoffEntityAC01.setDot11EDCACWmin(32);
	}

	private void deffect(int AIFSN, int CWmin) {
		//Standard deffect settings
		theBackoffEntityAC01.setDot11EDCAAIFSN(2);
		theBackoffEntityAC01.setDot11EDCACWmin(8);
		//A small chance to suddenly turn nice:
		if (this.forgive_counter <= 0 && this.rand.nextInt(50) == 0) {
			this.forgive_counter = 10;
		}
	}
	
	@Override
	public void compute() {
		this.mac.getMlme().setTheIterationPeriod(0.1);  // the sampling period in seconds, which is the time between consecutive calls of this method "compute()"
		this.theSamplingTime_sec =  this.mac.getMlme().getTheIterationPeriod().getTimeS(); // this sampling time can only be read after the MLME was constructed.
	
		int aQueueSize = this.theBackoffEntityAC01.getQueueSize();
		
		if (this.forgive_counter > 0) {
			this.forgive_counter -= 1;
			aCurrentQueueSize = 0.33d;
		} else {
			aCurrentQueueSize = (double)this.theBackoffEntityAC01.getCurrentQueueSize();
		}
		
		this.pid_controller.response(aCurrentQueueSize, this.theSamplingTime_sec);
		
		Integer AIFSN_AC01 = theBackoffEntityAC01.getDot11EDCAAIFSN();
		Integer CWmin_AC01 = theBackoffEntityAC01.getDot11EDCACWmin();


		if (this.pid_controller.getLastResponse() <= 0.66) {
			this.cooperate(AIFSN_AC01, CWmin_AC01);
		} else {
			this.deffect(AIFSN_AC01, CWmin_AC01);
		}
	}
	
	@Override
	public void plot() {
		if (plotter == null) {
			plotter = new JEMultiPlotter("First Tit-Tat Blaster 3000 station, Station " + this.dot11MACAddress.toString(), "PID", "time [s]", "Value", this.theUniqueEventScheduler.getEmulationEnd().getTimeMs() / 1000.0, true);
			plotter.addSeries("Cooperating (high), deffecting (low)");
			plotter.addSeries("current");
			plotter.display();
		}
		plotter.plot(((Double) theUniqueEventScheduler.now().getTimeMs()).doubleValue() / 1000.0, this.pid_controller.getLastResponse(), 0);
		plotter.plot(((Double) theUniqueEventScheduler.now().getTimeMs()).doubleValue() / 1000.0, this.pid_controller.getLastResponse() <= 0.6 ? -.1 : -.2, 1);
		plotter.plot(((Double) theUniqueEventScheduler.now().getTimeMs()).doubleValue() / 1000.0, this.aCurrentQueueSize, 2);
	}

}
