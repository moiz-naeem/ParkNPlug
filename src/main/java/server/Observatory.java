package server;

public class Observatory {
    private String observatoryName;
    private Double latitude;
    private Double longitude;

    public Observatory(String observatoryName, Double latitude, Double longitude) {
        this.observatoryName = observatoryName;
		this.latitude = latitude;
		this.longitude = longitude;
    }

	public void setObservatoryName (String observatoryName) {
		this.observatoryName = observatoryName;
	}
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public String getObservatoryName() {
		return observatoryName;
	}

	public Double getLatitude() {
		return latitude;
	}
	public Double getLongitude() {
		return longitude;
	}



    @Override
	public String toString() {
		return "Observatory{" +
			"observatoryName='" + observatoryName + '\'' +
			", latitude=" + latitude +
			", longitude=" + longitude +
			'}';
	}

}
