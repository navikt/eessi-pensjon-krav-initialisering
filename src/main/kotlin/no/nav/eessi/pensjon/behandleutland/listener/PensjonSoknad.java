package no.nav.eessi.pensjon.behandleutland.listener;

public class PensjonSoknad {

    private String sakId;
    private String beskrivelse;
    private String bucId;
    private String hendelsesKode;

    public String getSakId() {
        return sakId;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public String getBucId() {
        return bucId;
    }

    public String getHendelsesKode() {
        return hendelsesKode;
    }

    public void setSakId(String sakId) {
        this.sakId = sakId;
    }

    public void setBeskrivelse(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    public void setBucId(String bucId) {
        this.bucId = bucId;
    }

    public void setHendelsesKode(String hendelsesKode) {
        this.hendelsesKode = hendelsesKode;
    }

    @Override
    public String toString() {
        return String.format("PensjonSoknad{sakId='%s', beskrivelse='%s', bucId='%s', hendelsescode='%s'}", sakId, beskrivelse, bucId, hendelsesKode);
    }
}
