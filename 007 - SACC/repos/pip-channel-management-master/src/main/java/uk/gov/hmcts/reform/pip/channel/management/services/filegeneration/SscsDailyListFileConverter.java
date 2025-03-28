package uk.gov.hmcts.reform.pip.channel.management.services.filegeneration;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import uk.gov.hmcts.reform.pip.channel.management.models.templatemodels.sscsdailylist.CourtHouse;
import uk.gov.hmcts.reform.pip.channel.management.services.helpers.DateHelper;
import uk.gov.hmcts.reform.pip.channel.management.services.helpers.GeneralHelper;
import uk.gov.hmcts.reform.pip.channel.management.services.helpers.LanguageResourceHelper;
import uk.gov.hmcts.reform.pip.channel.management.services.helpers.listmanipulation.SscsListHelper;
import uk.gov.hmcts.reform.pip.model.publication.Language;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SscsDailyListFileConverter implements FileConverter {

    @Override
    public String convert(JsonNode highestLevelNode, Map<String, String> metadata,
                          Map<String, Object> languageResources) throws IOException {
        Context context = new Context();
        Language language = Language.valueOf(metadata.get("language"));
        languageResources.putAll(LanguageResourceHelper.readResourcesFromPath("openJusticeStatement", language));

        context.setVariable("i18n", languageResources);
        context.setVariable("metadata", metadata);
        context.setVariable("telephone", GeneralHelper.safeGet("venue.venueContact.venueTelephone", highestLevelNode));
        context.setVariable("email", GeneralHelper.safeGet("venue.venueContact.venueEmail", highestLevelNode));

        context.setVariable("publishedDate", DateHelper.formatTimeStampToBst(
            GeneralHelper.safeGet("document.publicationDate", highestLevelNode), Language.ENGLISH,
            false, true)
        );

        List<CourtHouse> listOfCourtHouses = new ArrayList<>();
        for (JsonNode courtHouse : highestLevelNode.get("courtLists")) {
            listOfCourtHouses.add(SscsListHelper.courtHouseBuilder(courtHouse));
        }
        context.setVariable("courtList", listOfCourtHouses);
        return TemplateEngine.processTemplate(metadata.get("listType"), context);
    }
}



