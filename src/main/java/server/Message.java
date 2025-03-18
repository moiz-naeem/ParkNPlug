package server;
import org.json.JSONArray;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

public class Message {
    private String recordIdentifier;
    private String recordDescription;
    private String recordPayload;
    private String recordRightAscension;
    private String recordDeclination;
    private ZonedDateTime sent;
    private long recordTimeReceived;
	private String recordOwner;
	private Observatory observatory;
	private ObservatoryWeather observatoryWeather;

    public Message(String recordIdentifier, String recordDescription, String recordPayload,
                   String recordRightAscension, String recordDeclination, ZonedDateTime sent,
				   String recordOwner, Observatory observatory, ObservatoryWeather observatoryWeather) {

        this.recordIdentifier = recordIdentifier;
        this.recordDescription = recordDescription;
        this.recordPayload = recordPayload;
        this.recordRightAscension = recordRightAscension;
        this.recordDeclination = recordDeclination;
        this.sent = sent;
		this.recordTimeReceived = ZonedDateTime.now().toInstant().toEpochMilli();
		this.recordOwner = recordOwner;
		this.observatory = observatory;
		this.observatoryWeather = observatoryWeather;
	}

    public Message(String recordIdentifier, String recordDescription, String recordPayload,
                   String recordRightAscension, String recordDeclination, long recordTimeReceived,
				   String recordOwner , Observatory observatory, ObservatoryWeather observatoryWeather) {
        this.recordIdentifier = recordIdentifier;
        this.recordDescription = recordDescription;
        this.recordPayload = recordPayload;
        this.recordRightAscension = recordRightAscension;
        this.recordDeclination = recordDeclination;
        this.recordTimeReceived =  recordTimeReceived;
        this.sent = ZonedDateTime.ofInstant(Instant.ofEpochMilli(recordTimeReceived), ZoneOffset.UTC);
		this.recordOwner = recordOwner;
		this.observatory = observatory;
		this.observatoryWeather = observatoryWeather;
    }
    public void setRecordTimeReceived (long epoch) {
        this.recordTimeReceived = epoch;
        this.sent = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
    }
    public void setRecordTimeReceived(ZonedDateTime sent) {
        this.sent = sent;
        this.recordTimeReceived = sent.toInstant().toEpochMilli();
    }

	public void setRecordOwner(String recordOwner) {
		this.recordOwner = recordOwner;
	}

	public String getRecordOwner() {
		return recordOwner;
	}

	public String getRecordIdentifier() {
        return recordIdentifier;
    }

	public void setObservatory(Observatory observatory) {
		this.observatory = observatory;
	}

	public Observatory getObservatory() {
		return observatory;
	}

    public void setRecordIdentifier(String recordIdentifier) {
        this.recordIdentifier = recordIdentifier;
    }

    public String getRecordDescription() {
        return recordDescription;
    }

    public void setRecordDescription(String recordDescription) {
        this.recordDescription = recordDescription;
    }

    public String getRecordPayload() {
        return recordPayload;
    }

	public void setObservatoryWeather(ObservatoryWeather observatoryWeather) {
		this.observatoryWeather = observatoryWeather;
	}

	public ObservatoryWeather getObservatoryWeather() {
		return observatoryWeather;
	}

	public void setRecordPayload(String recordPayload) {
        this.recordPayload = recordPayload;
    }

    public String getRecordRightAscension() {
        return recordRightAscension;
    }

    public void setRecordRightAscension(String recordRightAscension) {
        this.recordRightAscension = recordRightAscension;
    }

    public String getRecordDeclination() {
        return recordDeclination;
    }

    public void setRecordDeclination(String recordDeclination) {
        this.recordDeclination = recordDeclination;
    }

    public String getRecordTimeReceived() {
        return Instant.ofEpochMilli(recordTimeReceived)
                      .atZone(ZoneOffset.UTC)
                      .toString();
    }



    public long dateAsInt() {
        return sent.toInstant().toEpochMilli();
    }

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n")
			.append("\t\"recordIdentifier\": \"").append(recordIdentifier).append("\",\n")
			.append("\t\"recordDescription\": \"").append(recordDescription).append("\",\n")
			.append("\t\"recordPayload\": \"").append(recordPayload).append("\",\n")
			.append("\t\"recordRightAscension\": \"").append(recordRightAscension).append("\",\n")
			.append("\t\"recordDeclination\": \"").append(recordDeclination).append("\",\n")
			.append("\t\"recordOwner\": \"").append(recordOwner).append("\",\n");

		if (observatory != null) {
			sb.append("\t\"observatory\": [\n")
				.append("\t\t{\n")
				.append("\t\t\t\"observatoryName\": \"").append(observatory.getObservatoryName()).append("\",\n")
				.append("\t\t\t\"latitude\": \"").append(observatory.getLatitude()).append("\",\n")
				.append("\t\t\t\"longitude\": \"").append(observatory.getLongitude()).append("\"\n")
				.append("\t\t}\n")
				.append("\t],\n");
		}

		sb.append("\t\"recordTimeReceived\": \"").append(getRecordTimeReceived()).append("\"\n");
		sb.append("}");
		return sb.toString();
	}


}

