package layer2_802Algorithms;

// **ADDED**
public final class PIDController {

	private double commulating_error = 0;
	private double prev_error = 0; 
	private double prev_val = Double.POSITIVE_INFINITY;

	private double bias;
	private double Kp;
	private double Ki;
	private double Kd;

	protected double state = 0;

	public PIDController(double bias, double Kp, double Ki, double Kd) {
		this.bias = bias;
		this.Kp = Kp;
		this.Ki = Ki;
		this.Kd = Kd;
	}

	protected double getLastResponse() {
		return this.state;
	}

	// Calculates PID value 
	protected double response(double current_error, double delta_time)  {
		double error = current_error - this.state;
		if (this.prev_error == Double.POSITIVE_INFINITY) {
			this.prev_error = error;
		}
		this.commulating_error += error;
		double proportional = this.Kp * error;
    	double integral = this.Ki * this.commulating_error * delta_time;
    	double derivative = this.Kd * (error - this.prev_error) / delta_time;
		this.prev_error = error;
		this.state = state * 0.96 + (proportional + integral + derivative) * 0.4 + bias;
		return this.state; 
	}
}