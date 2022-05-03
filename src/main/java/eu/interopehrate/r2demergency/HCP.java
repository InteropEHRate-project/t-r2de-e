package eu.interopehrate.r2demergency;

public class HCP {
    private String citizenToken;
    private String emergencyToken;
    private String symKey;
    private String cloudUri;
    private String citizenHriId;
    private String hospitalID;
    private String hcpName;
    private String hriEmergencyToken;
    private String msg;
    private int status;


    private static HCP singleHCPInstance = null;

    private HCP (String citizenToken, String emergencyToken, String symKey, String cloudUri) {
        this.citizenToken = citizenToken;
        this.emergencyToken = emergencyToken;
        this.symKey = symKey;
        this.cloudUri = cloudUri;
    }
    private HCP(){this("", "", "", "");}

    public static HCP HCP() {
        if (singleHCPInstance == null) {
            singleHCPInstance = new HCP();
        }
        return  singleHCPInstance;
    }

    public String getHriEmergencyToken() {
        return hriEmergencyToken;
    }

    public void setHriEmergencyToken(String hriEmergencyToken) {
        this.hriEmergencyToken = hriEmergencyToken;
    }

    public String getCitizenToken() {
        return citizenToken;
    }

    public void setCitizenToken(String citizenToken) {
        this.citizenToken = citizenToken;
    }

    public String getEmergencyToken() {
        return emergencyToken;
    }

    public void setEmergencyToken(String emergencyToken) {
        this.emergencyToken = emergencyToken;
    }

    public String getSymKey() {
        return symKey;
    }

    public void setSymKey(String symKey) {
        this.symKey = symKey;
    }

    public String getCloudUri() {
        return cloudUri;
    }

    public void setCloudUri(String cloudUri) {
        this.cloudUri = cloudUri;
    }

    public String getHospitalID() {
        return hospitalID;
    }

    public void setHospitalID(String hospitalID) {
        this.hospitalID = hospitalID;
    }

    public String getHcpName() {
        return hcpName;
    }

    public void setHcpName(String hcpName) {
        this.hcpName = hcpName;
    }

    public String getCitizenHriId() {
        return citizenHriId;
    }

    public void setCitizenHriId(String citizenHriId) {
        this.citizenHriId = citizenHriId;
    }
}
