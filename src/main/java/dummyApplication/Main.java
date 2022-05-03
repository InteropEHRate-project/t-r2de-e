package dummyApplication;

import ca.uhn.fhir.util.StopWatch;
import com.google.common.base.Stopwatch;
import eu.interopehrate.protocols.common.DocumentCategory;
import eu.interopehrate.protocols.common.FHIRResourceCategory;
import eu.interopehrate.protocols.common.ResourceCategory;
import eu.interopehrate.r2demergency.R2DEmergencyFactory;
import eu.interopehrate.r2demergency.api.R2DEmergencyI;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;

public class Main {
    public static void main(String[] args) throws Exception {
//        R2DEmergencyI r2de = R2DEmergencyFactory.create("C:\\\\InterOP\\\\IPSValidatorPack");
        R2DEmergencyI r2de = R2DEmergencyFactory.create("..\\r2dEmergencyMaven\\src\\main\\resources");


//      Simple test
        String qrCodeContent = "$QR_CODE_CONTENT$";

        String hospitalID = "hco";
        String certificate = "$CERTIFICATE$";
        String hcpName = "Shayan";

//      Request access to S-EHR Cloud
        String emergencyToken = r2de.requestAccess(qrCodeContent, hospitalID, certificate, hcpName);
        System.out.println("emergencyToken: \t" + emergencyToken);

//      Retrieving list of buckets that the account has access
        JSONArray buckets = r2de.listBuckets(emergencyToken);
        System.out.println(buckets);

//      Retrieving list of objects within a specific bucket
        Iterator bucket = buckets.iterator();
        JSONObject objects = null;
        while(bucket.hasNext()){
            String currentBucket = (String) bucket.next();
            objects = r2de.listObjects(emergencyToken, currentBucket);
            System.out.println("BUCKET: \t"+currentBucket);
            System.out.println("OBJECTS: \t"+objects);
            System.out.println();
        }

//        Retrieving metadata of an object in a specific bucket
        JSONObject metadata = r2de.getBundlesInfo(emergencyToken, (String) buckets.get(0), FHIRResourceCategory.PATIENT);
        System.out.println("METADATA: \t" + metadata.toJSONString());

//        Retrieving Patient Summary from S-EHR Cloud
        String patientSummary = r2de.get(emergencyToken, (String) buckets.get(0), DocumentCategory.PATIENT_SUMMARY);
        System.out.println(patientSummary);

        String dr = r2de.get(emergencyToken, (String) buckets.get(0), FHIRResourceCategory.ENCOUNTER);
        System.out.println(dr);

//        Uploading Patient Summary to Emergency Bucket
        String uploadResponse = r2de.create(emergencyToken, FHIRResourceCategory.DOCUMENT_REFERENCE, patientSummary);
        System.out.println("UPLOAD RESPONSE: \t"+uploadResponse);

    }
}
