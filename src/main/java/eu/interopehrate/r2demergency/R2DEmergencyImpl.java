package eu.interopehrate.r2demergency;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.google.gson.JsonParseException;
import eu.interopehrate.encryptedCommunication.EncryptedCommunicationFactory;
import eu.interopehrate.encryptedCommunication.api.EncryptedCommunication;
import eu.interopehrate.hri.thri.HealthRecordIndexFactory;
import eu.interopehrate.hri.thri.api.HealthRecordIndexI;
import eu.interopehrate.protocols.common.ResourceCategory;
import eu.interopehrate.r2demergency.api.R2DEmergencyI;
import okhttp3.*;
import org.hl7.fhir.r4.model.Bundle;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

public class R2DEmergencyImpl implements R2DEmergencyI {
    private String structureDefinitionsPath;
    private HealthRecordIndexI thri = HealthRecordIndexFactory.create();
    public R2DEmergencyImpl(String structureDefinitionsPath){
        this.structureDefinitionsPath = structureDefinitionsPath;
        check();
    }

    private final HCP hcp = HCP.HCP();
    EncryptedCommunication encryptedCommunication = EncryptedCommunicationFactory.create();
    private OkHttpClient client = new OkHttpClient();
    private HttpURLConnection connection;
    private String decrypted;
    private int status;

    FhirContext ctx = FhirContext.forR4();
    IParser parser = FhirContext.forR4().newJsonParser();
    private Checker checker;

    private void check() {
        {
            try {
                checker = new Checker(ctx, this.structureDefinitionsPath);
            } catch (FileNotFoundException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }
	
    String line;
    StringBuffer responseContent = new StringBuffer();

    @Override
    public String requestAccess(String qrCodeContent, String hcoAttr, String hcoCertificate, String hcpName) throws Exception {
        try {
            readQrCodeContent(qrCodeContent);
        } catch (org.json.simple.parser.ParseException e) {
            e.printStackTrace();
            return "Error scanning the QR code";
        }

		//        Communicate with the Health Record Index to receive the citizen's Cloud information
        String hriResponse = thri.getCloud(hcp.getHriEmergencyToken(), hcp.getCitizenHriId()).toString();
        System.out.println("HRI RESPONSE:\t" + hriResponse);
        JSONObject thriResponse = stringToJson(hriResponse);
        if (thriResponse.get("msg").equals("Citizen found")) {
            JSONArray thriResponseData = (JSONArray) thriResponse.get("data");
            JSONObject thriResponseDataCitizen = (JSONObject) thriResponseData.get(0);

            //        Setting Cloud URL received from the Health Record Index service
            String cloudUri = encryptedCommunication.decrypt(thriResponseDataCitizen.get("cloudUri").toString().replace("DASHREPLACEDASH", "/"), hcp.getSymKey());
            hcp.setCloudUri(cloudUri);
            System.out.println("CloudURI: \t" + cloudUri);

            hcp.setHcpName(hcpName);
            System.out.println("HCP Name: \t" + hcpName);

            String citizenUsername = encryptedCommunication.decrypt(thriResponseDataCitizen.get("citizenUsername").toString().replace("DASHREPLACEDASH", "/"), hcp.getSymKey());
            System.out.println("Citizen Username: \t" + citizenUsername);

            String endpoint = "/hcp/requestaccess";
            String urlParams = "";
            try {
                URL url = new URL(hcp.getCloudUri() + endpoint + urlParams);
                System.out.println(url.toString());
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Authorization", hcp.getCitizenToken());
                connection.setRequestProperty("HCO-attrs", hcoAttr);
                connection.setRequestProperty("HCO-certificate", hcoCertificate);
                connection.setRequestProperty("hcp_name", hcpName);
                // Request setup
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                status = connection.getResponseCode();

                if (status == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                    while ((line = reader.readLine()) != null) {
                        responseContent.append(line);
                    }
                    reader.close();
                } else {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    while ((line = errorReader.readLine()) != null) {
                        responseContent.append(line);

                    }
                    errorReader.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                connection.disconnect();
            }
            System.out.println(responseContent);
            if (responseContent != null || responseContent.toString() != "") {
                JSONObject jsonObject = stringToJson(responseContent.toString());
                assert jsonObject != null;
                System.out.println(jsonObject.get("status"));
                if (jsonObject.get("status").equals(403) || jsonObject.get("status").equals(409)) {
                    hcp.setEmergencyToken(null);
                } else {
                    hcp.setEmergencyToken((String) jsonObject.get("token"));
                }
            }
			
            // Empty string buffer every time a request is done
            responseContent.setLength(0);
            return hcp.getEmergencyToken();
        } else {
            hcp.setEmergencyToken(null);
            return hcp.getEmergencyToken();
        }
    }

    @Override
    public JSONArray listBuckets(String emergencyToken) throws Exception {
        responseContent.delete(0, responseContent.length());
        String endpoint = "/hcp/buckets";
        String urlParams = "";
        try {
            URL url = new URL(hcp.getCloudUri() + endpoint + urlParams);
            System.out.println(url.toString());
            connection = (HttpURLConnection) url.openConnection();

            // Request setup
            connection.setRequestProperty("Authorization", hcp.getEmergencyToken());
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            status = connection.getResponseCode();

            if(status == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                while ((line = reader.readLine()) != null) {
                    responseContent.append(line);
                }
                reader.close();
            } else {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                while ((line = errorReader.readLine()) != null) {
                    responseContent.append(line);

                }
                errorReader.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connection.disconnect();
        }

        if (responseContent != null || responseContent.toString() != ""){
            JSONObject jsonObject = stringToJson(responseContent.toString());
            JSONArray buckets = (JSONArray) jsonObject.get("buckets");
            return buckets;
        }

        // Empty string buffer every time a request is done
        responseContent.setLength(0);
        return null;
    }

    @Override
    public JSONObject listObjects(String emergencyToken, String bucketName) throws Exception {
        responseContent.delete(0, responseContent.length());

        String endpoint = "/hcp/buckets/"+bucketName;
        String urlParams = "";
        try {
            URL url = new URL(hcp.getCloudUri() + endpoint + urlParams);
            System.out.println(url.toString());
            connection = (HttpURLConnection) url.openConnection();


            // Request setup
            connection.setRequestProperty("Authorization", hcp.getEmergencyToken());
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            status = connection.getResponseCode();

            if(status == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                while ((line = reader.readLine()) != null) {
                    responseContent.append(line);
                }
                reader.close();
            } else {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                while ((line = errorReader.readLine()) != null) {
                    responseContent.append(line);

                }
                errorReader.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connection.disconnect();
        }
        if (responseContent != null || responseContent.toString() != ""){
            JSONObject objects = new JSONObject();

            List<String> objectsList = new ArrayList<>();

            objects.put("bucket", bucketName);
            JSONObject response = stringToJson(responseContent.toString());
            if (!response.containsKey("err")){
                Iterator<JSONObject> object = response.values().iterator();
                String currentObject;
                while(object.hasNext()){
                    currentObject = String.valueOf(object.next());
                    currentObject = currentObject.replace("DASHREPLACEDASH", "/");
                    try {
                        decrypted = encryptedCommunication.decrypt(currentObject, hcp.getSymKey());
                        objectsList.add(decrypted);
                    } catch (Exception e) {
                        objectsList.add(currentObject);
                    }
                }
                objects.put("objects", objectsList);
            }
            if(objects.containsKey("err")){
                objects.put("status", 400);
            } else {
                objects.put("status", 200);
            }
            return objects;
        }

        // Empty string buffer every time a request is done
        responseContent.setLength(0);
        return null;
    }

    @Override
    public JSONObject getBundlesInfo(String emergencyToken, String bucketName, ResourceCategory rc) throws Exception {
        responseContent.delete(0, responseContent.length());
        String hrType = rc.toString();

        String encryptedHRType = encryptedCommunication.encrypt(hrType, hcp.getSymKey());
        encryptedHRType = encryptedHRType.replace("\n", "");
        encryptedHRType = encryptedHRType.replace("/", "DASHREPLACEDASH");
        encryptedHRType = encryptedHRType.replace("+", "%2B");
        encryptedHRType += ".txt";
        System.out.println("Encrypted EHR: \t" + encryptedHRType);

        String endpoint = "/hcp/"+bucketName+"/"+encryptedHRType+"/metadata";
        String urlParams = "";
        try {
            URL url = new URL(hcp.getCloudUri() + endpoint + urlParams);
            System.out.println(url.toString());
            connection = (HttpURLConnection) url.openConnection();


            // Request setup
            connection.setRequestProperty("Authorization", hcp.getEmergencyToken());
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            status = connection.getResponseCode();

            if(status == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                while ((line = reader.readLine()) != null) {
                    responseContent.append(line);
                }
                reader.close();
            } else {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                while ((line = errorReader.readLine()) != null) {
                    responseContent.append(line);

                }
                errorReader.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connection.disconnect();
        }

        if (responseContent != null || responseContent.toString() != ""){
            JSONObject metadata = new JSONObject();

            metadata.put("bucket", bucketName);
            metadata.put("object", rc.toString());

            JSONObject response = stringToJson(responseContent.toString());
            if (!response.containsKey("err")){
                JSONArray metadataArray = (JSONArray) response.get("metadata");
                JSONObject metadataObject = (JSONObject) metadataArray.get(0);
                metadataObject.put("size", response.get("size"));
                metadata.put("metadata", metadataObject);
            }
            if(response.containsKey("err")){
                metadata.put("status", 400);
            } else {
                metadata.put("status", 200);
            }
            return metadata;
        }

        // Empty string buffer every time a request is done
        responseContent.setLength(0);
        return null;
    }

    @Override
    public String get(String emergencyToken, String bucketName, ResourceCategory rc) throws Exception {
        responseContent.delete(0, responseContent.length());
        String hrType = rc.toString();

        String encryptedHRType = encryptedCommunication.encrypt(hrType, hcp.getSymKey());
        encryptedHRType = encryptedHRType.replace("\n", "");
        encryptedHRType = encryptedHRType.replace("/", "DASHREPLACEDASH");
        encryptedHRType = encryptedHRType.replace("+", "%2B");
        encryptedHRType += ".txt";
        System.out.println("Encrypted EHR: \t" + encryptedHRType);

        String endpoint = "/hcp/"+bucketName+"/"+encryptedHRType;

        String urlParams = "?token=" + emergencyToken;

        try {
            URL url = new URL(hcp.getCloudUri() + endpoint + urlParams);
            System.out.println(url.toString());
            connection = (HttpURLConnection) url.openConnection();


            // Request setup
            connection.setRequestProperty("Authorization", hcp.getEmergencyToken());
            connection.setRequestProperty("hcp_name", hcp.getHcpName());
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            status = connection.getResponseCode();

            if(status == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                while ((line = reader.readLine()) != null) {
                    responseContent.append(line);
                }
                reader.close();
            } else {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                while ((line = errorReader.readLine()) != null) {
                    responseContent.append(line);

                }
                errorReader.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connection.disconnect();
        }

        boolean isJsonValid = isJSONValid(responseContent.toString());
        if (isJsonValid) {
            JSONObject errorResponse = stringToJson(responseContent.toString());
            return errorResponse.get("msg").toString();
        } else {
            if (status == 200) {
                try {
                    decrypted = encryptedCommunication.decrypt(responseContent.toString(), hcp.getSymKey());
                    boolean isCompliant = true;
                    if (isCompliant) {
                        return decrypted;
                    } else {
                        return "ERROR: " +rc.toString()+ " is not compliant";
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return "Something went wrong";
    }

    @Override
    public String create(String emergencyToken, ResourceCategory rc, String healthRecord) throws Exception {
        responseContent.delete(0, responseContent.length());
        String hrType = rc.toString();

        boolean isCompliant = true;

        if (isCompliant) {
            String encryptedHealthRecord = encryptedCommunication.encrypt(healthRecord, hcp.getSymKey());
            String encryptedHRType = encryptedCommunication.encrypt(hrType, hcp.getSymKey());
            encryptedHRType = encryptedHRType.replace("\n", "");
            encryptedHRType = encryptedHRType.replace("/", "DASHREPLACEDASH");
            encryptedHRType = encryptedHRType.replace("+", "%2B");
            String endpoint = "/hcp/upload/hr";
            String metadata = "{\"hr-type\":\"" + encryptedHRType + "\",\"file-type\":\"txt\"}";

            String urlParams = "?metadata=" + metadata;

            MediaType mediaType = MediaType.parse("text/plain");

            RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("hr_file",encryptedHealthRecord)
                    .build();

            Request request = new Request.Builder()
                    .url(hcp.getCloudUri()+endpoint+urlParams)
                    .method("POST", body)
                    .addHeader("Authorization", hcp.getEmergencyToken())
                    .addHeader("hcp_name", hcp.getHcpName())
                    .build();

            Response response = client.newCall(request).execute();
            assert response.body() != null;

            InputStream inputStream = response.body().byteStream();
            Scanner sc = new Scanner(inputStream);

            while (sc.hasNext()) {
                responseContent.append(sc.nextLine());
            }

            return responseContent.toString();

        } else {
            return "ERROR: " +rc.toString()+ " is not compliant";
        }
    }

    public boolean isJSONValid(String str) {
        JSONObject  jsonObject = new JSONObject();
        JSONParser jsonParser = new  JSONParser();
        try {
            jsonObject = (JSONObject) jsonParser.parse(str);
            return true;
        }catch (JsonParseException | org.json.simple.parser.ParseException err){
            return false;
        }
    }

    private void readQrCodeContent(String qrCodeContent) throws org.json.simple.parser.ParseException {
        JSONParser parser = new JSONParser();
        JSONObject qrContent = (JSONObject) parser.parse(qrCodeContent);

        hcp.setCitizenToken((String) qrContent.get("emergencyToken"));
        hcp.setCitizenHriId((String) qrContent.get("citizenId"));
        hcp.setSymKey((String) qrContent.get("symKey"));
        hcp.setHriEmergencyToken((String) qrContent.get("hriEmergencyToken"));
    }

    private JSONObject stringToJson (String str) {
        JSONObject  jsonObject;
        JSONParser jsonParser = new  JSONParser();
        try {
            jsonObject = (JSONObject) jsonParser.parse(str);
            return jsonObject;
        }catch (JsonParseException | org.json.simple.parser.ParseException err){
            return null;
        }

    }

    private boolean validateEHR(String ehr){
        System.out.println("validating file...");
        Bundle bundle = parser.parseResource(Bundle.class, ehr);
        return checker.validateProfile(bundle);
    }

}
