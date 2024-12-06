package layer2_802Algorithms;

import plot.JEMultiPlotter;
import layer1_802Phy.JE802PhyMode;
import layer2_80211Mac.JE802_11BackoffEntity;
import layer2_80211Mac.JE802_11Mac;
import layer2_80211Mac.JE802_11MacAlgorithm;

enum Mode {
	COOPERATE, DEFFECT
}

public class TitTatBlaster3000 extends JE802_11MacAlgorithm {
	
	private JE802_11BackoffEntity theBackoffEntityAC01;
	
	private double theSamplingTime_sec;

	private PIDController2 pid_controller;

	private boolean explore = true;

	private double throughput_setpoint;

	private Mode opponent_mode;

	// Fields for estimating the throughput
	private int previous_queue_state;
	private int measurement_periode;
	private int current_measurement_period;
	private int packet_sent;
	private double current_throughput;

	public TitTatBlaster3000(String name, JE802_11Mac mac) {
		super(name, mac);
		this.theBackoffEntityAC01 = this.mac.getBackoffEntity(1);
		message("This is station " + this.dot11MACAddress.toString() +". Tit-Tat Blaster 3000 up and blastin' algorithm: '" + this.algorithmName + "'.", 100);
		this.mac.getPhy().setCurrentTransmitPower_dBm(0);
		this.mac.getPhy().setCurrentPhyMode("64QAM34");
		theBackoffEntityAC01.setDot11EDCAAIFSN(2);
		theBackoffEntityAC01.setDot11EDCACWmin(15);
		this.pid_controller = new PIDController(-0.001, 0.001, 0.002, 0.0001);
		this.throughput_setpoint = 0.5;

		// Player variables
		this.previous_opponent_mode = Mode.COOPERATE; // We assume that the opponent is cooperative at the begininng
		this.current_opponent_mode = Mode.COOPERATE;
	
		this.previous_queue_state = 0;
		this.measurement_periode = 10; // After how many calls of compute() the throughput should be calculated
		this.current_measurement_period = 0;
		this.packet_sent = 0;
		this.current_throughput = 1; // FIXME: Change the initial value to something reasonable
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

	private double estimate_throughput() {
		// If the queue size was 1 in the previous state and now it is 0, we assume that a packet was sent
		if (this.previous_queue_state == 1 && aCurrentQueueSize == 0) {
			this.packet_sent++;
		}

		if (this.current_measurement_period > this.measurement_periode) {
			this.current_throughput = this.packet_sent / (this.measurement_periode * this.theSamplingTime_sec);
			this.packet_sent = 0;
			this.current_measurement_period = 0;
			message("Throughput: " + this.current_throughput, 100);
		}

		this.current_measurement_period++;

		return this.current_throughput;
	}
	
	@Override
	public void compute() {
		this.mac.getMlme().setTheIterationPeriod(0.1);  // the sampling period in seconds, which is the time between consecutive calls of this method "compute()"
		this.theSamplingTime_sec =  this.mac.getMlme().getTheIterationPeriod().getTimeS(); // this sampling time can only be read after the MLME was constructed.
	
		int aQueueSize = this.theBackoffEntityAC01.getQueueSize();
		int aCurrentQueueSize = this.theBackoffEntityAC01.getCurrentQueueSize();

		
		Integer AIFSN_AC01 = theBackoffEntityAC01.getDot11EDCAAIFSN();
		Integer CWmin_AC01 = theBackoffEntityAC01.getDot11EDCACWmin();

		// We calculate the throughput based on how the queue size changes
		double current_throughput = this.estimate_throughput();

		// We calculate if the opponent is cooperative or deffecting based on the current throughput
		if (current_throughput > 10) {	// FIXME: Change the threshold to something reasonable
			this.current_opponent_mode = Mode.COOPERATE;
		} else {
			this.current_opponent_mode = Mode.DEFFECT;
		}

		// FIXME: Currently implemented the standard Tit-Tat strategy.
		int baseCWmin;
		if (this.previous_opponent_mode == Mode.COOPERATE) {
			this.throughput_setpoint = 5;
			baseCWmin = 1; 
		} else {
			this.throughput_setpoint = 10;
			baseCWmin = 30;
		}

		this.previous_opponent_mode = this.current_opponent_mode;

		double pid_output = this.pid_controller.response(this.throughput_setpoint, this.current_throughput, this.theSamplingTime_sec);

		int ACWmin = Math.max(1, baseCWmin + pid_output);

		// theBackoffEntityAC01.setDot11EDCAAIFSN(1);
		theBackoffEntityAC01.setCurrentPhyMode("64QAM34");
		theBackoffEntityAC01.setDot11EDCAAIFSN(2);	
		theBackoffEntityAC01.setDot11EDCACWmin(ACWmin);

		// if (this.pid_controller.getLastResponse() <= 0.5) {
		// 	this.cooperate(AIFSN_AC01, CWmin_AC01);
		// } else {
		// 	this.deffect(AIFSN_AC01, CWmin_AC01);
		// }
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
