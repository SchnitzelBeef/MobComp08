package layer2_802Algorithms;

import plot.JEMultiPlotter;
import layer1_802Phy.JE802PhyMode;
import layer2_80211Mac.JE802_11BackoffEntity;
import layer2_80211Mac.JE802_11Mac;
import layer2_80211Mac.JE802_11MacAlgorithm;

public class TitTatBlaster3000 extends JE802_11MacAlgorithm {
	
	private JE802_11BackoffEntity theBackoffEntityAC01;
	
	private double theSamplingTime_sec;

	private PIDController pid_controller;

	private boolean explore = true;

	public TitTatBlaster3000(String name, JE802_11Mac mac) {
		super(name, mac);
		this.theBackoffEntityAC01 = this.mac.getBackoffEntity(1);
		message("This is station " + this.dot11MACAddress.toString() +". Tit-Tat Blaster 3000 up and blastin' algorithm: '" + this.algorithmName + "'.", 100);
		this.mac.getPhy().setCurrentTransmitPower_dBm(0);
		this.mac.getPhy().setCurrentPhyMode("64QAM34");
		theBackoffEntityAC01.setDot11EDCAAIFSN(2);
		theBackoffEntityAC01.setDot11EDCACWmin(15);
		this.pid_controller = new PIDController(-0.001, 0.001, 0.002, 0.0001);
	}


	private void cooperate(int AIFSN, int CWmin) {	
		//Standard cooperate settings:
		theBackoffEntityAC01.setDot11EDCAAIFSN(15);
		theBackoffEntityAC01.setDot11EDCACWmin(2);
	}

	private void deffect(int AIFSN, int CWmin) {
		//Standard deffect settings
		theBackoffEntityAC01.setDot11EDCAAIFSN(1);
		theBackoffEntityAC01.setDot11EDCACWmin(1);
	}

	private int setIntValue(double state, int lowerBound, int upperBound) {
		double ret = state * (upperBound - lowerBound);
		if (ret >= upperBound) {
			return upperBound;
		}
		if (ret <= lowerBound) {
			return lowerBound;
		}
		return (int)Math.round(ret);
	}
	
	@Override
	public void compute() {
		this.mac.getMlme().setTheIterationPeriod(0.1);  // the sampling period in seconds, which is the time between consecutive calls of this method "compute()"
		this.theSamplingTime_sec =  this.mac.getMlme().getTheIterationPeriod().getTimeS(); // this sampling time can only be read after the MLME was constructed.
	
		int aQueueSize = this.theBackoffEntityAC01.getQueueSize();
		int aCurrentQueueSize = this.theBackoffEntityAC01.getCurrentQueueSize();
		
		this.pid_controller.response((double)aCurrentQueueSize, this.theSamplingTime_sec);
		
		Integer AIFSN_AC01 = theBackoffEntityAC01.getDot11EDCAAIFSN();
		Integer CWmin_AC01 = theBackoffEntityAC01.getDot11EDCACWmin();

		if (this.pid_controller.getLastResponse() <= 0.5) {
			this.cooperate(AIFSN_AC01, CWmin_AC01);
		} else {
			this.deffect(AIFSN_AC01, CWmin_AC01);
		}
		theBackoffEntityAC01.setDot11EDCAAIFSN(1);
		theBackoffEntityAC01.setDot11EDCACWmin(1);
	}
	
	@Override
	public void plot() {
		if (plotter == null) {
			plotter = new JEMultiPlotter("PID Controller, Station " + this.dot11MACAddress.toString(), "max", "time [s]", "MAC Queue", this.theUniqueEventScheduler.getEmulationEnd().getTimeMs() / 1000.0, true);
			plotter.addSeries("current");
			plotter.display();
		}
		plotter.plot(((Double) theUniqueEventScheduler.now().getTimeMs()).doubleValue() / 1000.0, theBackoffEntityAC01.getQueueSize(), 0);
		plotter.plot(((Double) theUniqueEventScheduler.now().getTimeMs()).doubleValue() / 1000.0, theBackoffEntityAC01.getCurrentQueueSize(), 1);
		
	}

}
