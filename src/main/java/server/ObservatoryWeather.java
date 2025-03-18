package server;

public class ObservatoryWeather {
	private Double temperatureInKelvins;
	private Double cloudinessPercentance;
	private Double bagroundLightVolume;

	public ObservatoryWeather(Double temperatureInKelvins, Double cloudinessPercentance, Double bagroundLightVolume) {
		this.temperatureInKelvins = temperatureInKelvins;
		this.cloudinessPercentance = cloudinessPercentance;
         this.bagroundLightVolume = bagroundLightVolume;
	}


	public Double getTemperatureInKelvins() {
		return temperatureInKelvins;
	}
	public void setTemperatureInKelvins(Double temperatureInKelvins) {
           this.temperatureInKelvins = temperatureInKelvins;
	}
	public Double getCloudinessPercentance() {
		return cloudinessPercentance;
	}
	public void setCloudinessPercentance(Double cloudinessPercentance) {
		this.cloudinessPercentance = cloudinessPercentance;
	}
	public Double getBagroundLightVolume() {
		return bagroundLightVolume;
	}
	public void setBagroundLightVolume(Double bagroundLightVolume) {
		this.bagroundLightVolume = bagroundLightVolume;
	}


	@Override
	public String toString() {
		return "observatoryWeather{" +
			"temperatureInKelvins='" + temperatureInKelvins + '\'' +
			",cloudinessPercentance=" + cloudinessPercentance +
			", bagroundLightVolume=" + bagroundLightVolume +
			'}';
	}

}
