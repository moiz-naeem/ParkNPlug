package server;

class ObservationRecord {
    private String recordIdentifier;
    private String recordDescription;
    private String recordPayload;
    private String recordRightAscension;
    private String recordDeclination;
    private String recordTimeReceived;

    public ObservationRecord(String recordIdentifier,
                             String recordDescription,
                             String recordPayload,
                             String recordRightAscension,
                             String recordDeclination,
                             String recordTimeReceived) {
        this.recordIdentifier = recordIdentifier;
        this.recordDescription = recordDescription;
        this.recordPayload = recordPayload;
        this.recordRightAscension = recordRightAscension;
        this.recordDeclination = recordDeclination;
        this.recordTimeReceived = recordTimeReceived;
    }

    public String getRecordIdentifier() {
        return recordIdentifier;
    }

    public String getRecordDescription() {
        return recordDescription;
    }

    public String getRecordPayload() {
        return recordPayload;
    }

    public String getRecordRightAscension() {
        return recordRightAscension;
    }

    public String getRecordDeclination() {
        return recordDeclination;
    }
    public String getRecordTimeReceived(){
        return recordTimeReceived;
    }

    public void setRecordIdentifier(String recordIdentifier) {
        this.recordIdentifier = recordIdentifier;
    }

    public void setRecordDescription(String recordDescription) {
        this.recordDescription = recordDescription;
    }

    public void setRecordPayload(String recordPayload) {
        this.recordPayload = recordPayload;
    }

    public void setRecordRightAscension(String recordRightAscension) {
        this.recordRightAscension = recordRightAscension;
    }

    public void setRecordDeclination(String recordDeclination) {
        this.recordDeclination = recordDeclination;
    }
    public void setRecordTimeReceived(String recordTimeRecieved){
        this.recordTimeReceived = recordTimeReceived;
    }
}
