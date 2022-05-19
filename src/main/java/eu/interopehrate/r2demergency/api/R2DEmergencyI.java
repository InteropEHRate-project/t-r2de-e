package eu.interopehrate.r2demergency.api;

import eu.interopehrate.protocols.common.ResourceCategory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.text.ParseException;

public interface R2DEmergencyI {

    String requestAccess(String qrCodeContent, String hospitalID, String hcoCertificate, String hcpName) throws Exception;

    JSONArray listBuckets (String emergencyToken) throws Exception;

    JSONObject listObjects (String emergencyToken, String bucketName) throws Exception;

    JSONObject getBundlesInfo (String emergencyToken, String bucketName, ResourceCategory rc) throws Exception;

    String get (String emergencyToken, String bucketName, ResourceCategory rc) throws Exception;

    String create (String emergencyToken, ResourceCategory rc, String healthRecord) throws Exception;

    boolean checkProvenance(String resource) throws Exception;

    boolean checkCompliance (String resource);

}
