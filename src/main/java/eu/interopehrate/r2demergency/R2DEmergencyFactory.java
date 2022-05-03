package eu.interopehrate.r2demergency;

public class R2DEmergencyFactory {
    private R2DEmergencyFactory() {
    }
    /**
     * Factory method for creating an instance of R2DEmergency
     *
     * @return
     */

    public static R2DEmergencyImpl create(String structureDefinitionsPath) {
        return new R2DEmergencyImpl(structureDefinitionsPath);
    }
}
