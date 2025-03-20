package server;

public class WeatherData {

	private double temperatureInKelvins;
	private double cloudinessPercentance;
	private double bagroundLightVolume;

	// Getters and Setters
	public double getTemperatureInKelvins() {
		return temperatureInKelvins;
	}

	public void setTemperatureInKelvins(double temperatureInKelvins) {
		this.temperatureInKelvins = temperatureInKelvins;
	}

	public double getCloudinessPercentance() {
		return cloudinessPercentance;
	}

	public void setCloudinessPercentance(double cloudinessPercentance) {
		this.cloudinessPercentance = cloudinessPercentance;
	}

	public double getBagroundLightVolume() {
		return bagroundLightVolume;
	}

	public void setBagroundLightVolume(double bagroundLightVolume) {
		this.bagroundLightVolume = bagroundLightVolume;
	}
}
