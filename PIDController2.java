package layer2_802Algorithms;

// **ADDED**
public final class PIDController2 {

	private double commulating_error = 0;
	private double prev_error = 0; 
	private double prev_val = Double.POSITIVE_INFINITY;

	private double bias;
	private double Kp;
	private double Ki;
	private double Kd;

	private double pid_output = 0;
	private double state = 0;

	public PIDController2(double bias, double Kp, double Ki, double Kd) {
		this.bias = bias;
		this.Kp = Kp;
		this.Ki = Ki;
		this.Kd = Kd;
	}

	protected double getLastResponse() {
		return this.state;
	}

	protected double getLastPidOutput() {
		return this.pid_output;
	}

	// Calculates PID value 
	protected double response(double setpoint, double current_value, double delta_time)  {
		double error = setpoint - current_value;
		if (this.prev_error == Double.POSITIVE_INFINITY) {
			this.prev_error = error;
		}
		this.commulating_error += error;
		double proportional = this.Kp * error;
    	double integral = this.Ki * this.commulating_error * delta_time;
    	double derivative = this.Kd * (error - this.prev_error) / delta_time;
		this.prev_error = error;
		return proportional + integral + derivative;
	}
}