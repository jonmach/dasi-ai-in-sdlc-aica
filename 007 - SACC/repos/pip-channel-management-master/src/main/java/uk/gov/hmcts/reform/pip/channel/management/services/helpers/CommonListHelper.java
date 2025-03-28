package uk.gov.hmcts.reform.pip.channel.management.services.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thymeleaf.context.Context;
import uk.gov.hmcts.reform.pip.channel.management.services.helpers.listmanipulation.FamilyMixedListHelper;
import uk.gov.hmcts.reform.pip.model.publication.Language;

import java.util.Map;

import static uk.gov.hmcts.reform.pip.model.publication.ListType.CIVIL_AND_FAMILY_DAILY_CAUSE_LIST;
import static uk.gov.hmcts.reform.pip.model.publication.ListType.FAMILY_DAILY_CAUSE_LIST;

public final class CommonListHelper {
    private static final String DOCUMENT = "document";
    private static final String VERSION = "version";
    private static final String VENUE = "venue";
    private static final String VENUE_CONTACT = "venueContact";
    private static final String COURT_HOUSE = "courtHouse";
    private static final String COURT_ROOM = "courtRoom";
    private static final String SESSION = "session";
    private static final String SITTINGS = "sittings";
    private static final String HEARING = "hearing";
    private static final String APPLICANT = "applicant";
    private static final String RESPONDENT = "respondent";
    private static final String TIME_FORMAT = "h:mma";

    private CommonListHelper() {
    }

    public static Context preprocessArtefactForThymeLeafConverter(JsonNode artefact, Map<String, String> metadata,
                                                                  Map<String, Object> languageResources,
                                                                  boolean initialised) {
        Context context = new Context();
        LocationHelper.formatCourtAddress(artefact, "|", false);
        context.setVariable("metadata", metadata);
        context.setVariable("i18n", languageResources);

        Language language = Language.valueOf(metadata.get("language"));
        String publicationDate = artefact.get(DOCUMENT).get("publicationDate").asText();
        context.setVariable("publicationDate", DateHelper.formatTimeStampToBst(publicationDate, language,
                                                                               false, false));
        context.setVariable("publicationTime", DateHelper.formatTimeStampToBst(publicationDate, language,
                                                                               true, false));
        context.setVariable("contentDate", metadata.get("contentDate"));
        context.setVariable("locationName", metadata.get("locationName"));
        context.setVariable("provenance", metadata.get("provenance"));
        context.setVariable("venueAddress", LocationHelper.formatFullVenueAddress(artefact));
        context.setVariable("artefact", artefact);
        if (artefact.get(DOCUMENT).has(VERSION)) {
            context.setVariable(VERSION, artefact.get(DOCUMENT).get(VERSION).asText());
        }

        if (artefact.get(VENUE).has(VENUE_CONTACT)) {
            context.setVariable("phone", artefact.get(VENUE).get(VENUE_CONTACT).get("venueTelephone").asText());
            context.setVariable("email", artefact.get(VENUE).get(VENUE_CONTACT).get("venueEmail").asText());
        } else {
            context.setVariable("phone", "");
            context.setVariable("email", "");
        }

        String listType = metadata.get("listType");
        if (FAMILY_DAILY_CAUSE_LIST.name().equals(listType)
            || CIVIL_AND_FAMILY_DAILY_CAUSE_LIST.name().equals(listType)) {
            FamilyMixedListHelper.manipulatedlistData(artefact, language);
        } else {
            manipulatedListData(artefact, language, initialised);
        }
        return context;
    }

    public static void manipulatedListData(JsonNode artefact, Language language, boolean initialised) {
        artefact.get("courtLists").forEach(
            courtList -> courtList.get(COURT_HOUSE).get(COURT_ROOM).forEach(
                courtRoom -> courtRoom.get(SESSION).forEach(session -> {
                    StringBuilder formattedJudiciary = new StringBuilder();
                    formattedJudiciary.append(JudiciaryHelper.findAndManipulateJudiciary(session, true));
                    session.get(SITTINGS).forEach(sitting -> {
                        DateHelper.calculateDuration(sitting, language);
                        DateHelper.formatStartTime(sitting, TIME_FORMAT, true);
                        SittingHelper.findAndConcatenateHearingPlatform(sitting, session);

                        sitting.get(HEARING).forEach(hearing -> {
                            if (hearing.has("party")) {
                                PartyRoleHelper.findAndManipulatePartyInformation(hearing, initialised);
                            } else {
                                ObjectNode hearingObj = (ObjectNode) hearing;
                                hearingObj.put(APPLICANT, "");
                                hearingObj.put(RESPONDENT, "");
                            }
                            hearing.get("case").forEach(
                                hearingCase -> CaseHelper.manipulateCaseInformation((ObjectNode) hearingCase)
                            );
                        });
                    });
                    LocationHelper.formattedCourtRoomName(courtRoom, session, formattedJudiciary);
                })
            )
        );
    }
}
