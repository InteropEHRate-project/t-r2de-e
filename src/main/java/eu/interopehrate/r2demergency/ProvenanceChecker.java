package eu.interopehrate.r2demergency;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import eu.interopehrate.fhir.provenance.BundleProvenanceBuilder;
import eu.interopehrate.fhir.provenance.NodeFactory;
import eu.interopehrate.fhir.provenance.ProvenanceValidationRecord;
import eu.interopehrate.fhir.provenance.ProvenanceValidationResults;
import eu.interopehrate.fhir.provenance.ProvenanceValidator;
import eu.interopehrate.fhir.provenance.ResourceNode;
import eu.interopehrate.fhir.provenance.ResourceSigner;
import eu.interopehrate.fhir.provenance.test.SuccessfullValidation;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProvenanceChecker {
    Logger logger = LoggerFactory.getLogger(SuccessfullValidation.class);
    private static final String KEY_STORE = "keystore.p12";
    IParser parser = FhirContext.forR4().newJsonParser();

    public boolean check(Bundle bundle) throws Exception {
        ProvenanceValidator validator = new ProvenanceValidator(KEY_STORE, parser);
        ProvenanceValidationResults res = validator.validateBundle(bundle);
        logger.info("Validation outcome: " + res.isSuccessful());
        for (ProvenanceValidationRecord r : res.getValidationResult()) {
            logger.info(r.toString());
        }
        return res.isSuccessful();
    }
}
