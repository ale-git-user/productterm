package com.termmed.util;

import com.termmed.Data.DataProvider;
import com.termmed.model.Concept;
import com.termmed.model.Description;
import com.termmed.model.RF2Constants;
import com.termmed.model.Relationship;

import java.util.*;
import java.util.stream.Collectors;

public class SnomedUtils implements RF2Constants {


    public static String translateAcceptability (Acceptability a) {
        if (a.equals(Acceptability.PREFERRED)) {
            return "P";
        }

        if (a.equals(Acceptability.ACCEPTABLE)) {
            return "A";
        }
        return "A";
    }

    public static Acceptability translateAcceptability(String sctid) {
        if (sctid.equals(SCTID_ACCEPTABLE_TERM)) {
            return Acceptability.ACCEPTABLE;
        }

        if (sctid.equals(SCTID_PREFERRED_TERM)) {
            return Acceptability.PREFERRED;
        }
        return Acceptability.ACCEPTABLE;
    }

    public static String translateDescType(DescriptionType type){
        switch (type) {
            case FSN : return FSN;
            case SYNONYM : return SYN;
            case TEXT_DEFINITION : return DEF;
        }
        return SYN;
    }

    public static DescriptionType translateDescType(String descTypeId)  {
        if (descTypeId.equals(FSN)){return DescriptionType.FSN;}
        if (descTypeId.equals(SYN)){ return DescriptionType.SYNONYM;}
        if (descTypeId.equals(DEF)) {return DescriptionType.TEXT_DEFINITION;}
        return DescriptionType.SYNONYM;
    }


    public static String translateCaseSignificanceFromEnum(
            CaseSignificance caseSignificance) {
        switch (caseSignificance) {
            case ENTIRE_TERM_CASE_SENSITIVE: return "CS";
            case CASE_INSENSITIVE: return "ci";
            case INITIAL_CHARACTER_CASE_INSENSITIVE : return "cI";
            default :
        }
        return "ci";
    }

    public static CaseSignificance translateCaseSignificanceToEnum(
            String caseSignificanceSctId) {
        if (caseSignificanceSctId.equals(SCTID_ENTIRE_TERM_CASE_SENSITIVE)) {
            return CaseSignificance.ENTIRE_TERM_CASE_SENSITIVE;
        }
        if (caseSignificanceSctId.equals(SCTID_ENTIRE_TERM_CASE_INSENSITIVE)) {
            return CaseSignificance.CASE_INSENSITIVE;
        }
        if (caseSignificanceSctId.equals(SCTID_ONLY_INITIAL_CHAR_CASE_INSENSITIVE)) {
            return CaseSignificance.INITIAL_CHARACTER_CASE_INSENSITIVE;
        }
        return CaseSignificance.CASE_INSENSITIVE;
    }

    public static boolean translateActive(ActiveState active)  {
        switch (active) {
            case ACTIVE : return true;
            case INACTIVE : return false;
            default: return false;
        }
    }

    public static DefinitionStatus translateDefnStatus(String defnStatusSctId) {
        switch (defnStatusSctId) {
            case SCTID_PRIMITIVE : return DefinitionStatus.PRIMITIVE;
            case SCTID_FULLY_DEFINED: return DefinitionStatus.FULLY_DEFINED;
            default:
        }
        return null;
    }
    public static boolean descriptionHasActiveState(Description d, ActiveState a) {
        boolean hasActiveState = false;
        if (a.equals(ActiveState.BOTH) ||
                (a.equals(ActiveState.ACTIVE) && d.isActive()) ||
                (a.equals(ActiveState.INACTIVE) && !d.isActive())) {
            hasActiveState = true;
        }
        return hasActiveState;
    }

    public static boolean isCaseSensitive(String term) {
        String afterFirst = term.substring(1);
        boolean allLowerCase = afterFirst.equals(afterFirst.toLowerCase());
        return !allLowerCase;
    }

    public static String[] deconstructFSN(String fsn) {
        String[] elements = new String[2];
        int cutPoint = fsn.lastIndexOf(SEMANTIC_TAG_START);
        if (cutPoint == -1) {
            System.out.println("'" + fsn + "' does not contain a semantic tag!");
            elements[0] = fsn;
        } else {
            elements[0] = fsn.substring(0, cutPoint).trim();
            elements[1] = fsn.substring(cutPoint);
        }
        return elements;
    }

    public static String capitalize (String str) {
        if (str == null || str.isEmpty() || str.length() < 2) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static String initialCapitalOnly (String str) {
        if (str == null || str.isEmpty() || str.length() < 2) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    public static String deCapitalize (String str) {
        if (str == null || str.isEmpty() || str.length() < 2) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    public static void populateConceptType(Concept c) {
        if (c.getFsnSource() == null) {
            determineConceptTypeFromAttributes(c, CharacteristicType.STATED_RELATIONSHIP);
        } else {
            String semTag = SnomedUtils.deconstructFSN(c.getFsnSource())[1];
            switch (semTag) {
                case DrugUtils.MP : c.setConceptType(ConceptType.MEDICINAL_PRODUCT);
                    break;
                case DrugUtils.MPF : c.setConceptType(ConceptType.MEDICINAL_PRODUCT_FORM);
                    break;
                case DrugUtils.CD : c.setConceptType(ConceptType.CLINICAL_DRUG);
                    break;
                case DrugUtils.PRODUCT : c.setConceptType(ConceptType.PRODUCT);
                    break;
                default : c.setConceptType(ConceptType.UNKNOWN);
            }
        }
    }

    public static Map<String, Acceptability> createPreferredAcceptableMap(String refset) {
        Map<String, Acceptability> aMap = new HashMap<String, Acceptability>();
        aMap.put(refset, Acceptability.PREFERRED);
        return aMap;
    }

    private static void determineConceptTypeFromAttributes(Concept c, CharacteristicType charType) {
        try {
            //Do we have ingredients?  We're at least an MP
            if (getTargets(c, new Concept[] { HAS_ACTIVE_INGRED, HAS_PRECISE_INGRED }, charType).size() > 0) {
                c.setConceptType(ConceptType.MEDICINAL_PRODUCT);
                //Do we also have dose form?  If so, MPF
                if (getTargets(c, new Concept[] { HAS_MANUFACTURED_DOSE_FORM }, charType).size() > 0) {
                    c.setConceptType(ConceptType.MEDICINAL_PRODUCT_FORM);
                    //And if we have strength, CD
                    if (getTargets(c, new Concept[] { HAS_CONC_STRENGTH_DENOM_UNIT, HAS_PRES_STRENGTH_UNIT }, charType).size() > 0) {
                        c.setConceptType(ConceptType.CLINICAL_DRUG);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to determine concept type of " + c + " due to " + e);
        }
    }
    //order specified by the array
    public static Concept getTarget(Concept c, Concept[] types, int groupId, CharacteristicType charType){
        for (Concept type : types) {
            List<Relationship> rels = c.getRelationships(charType, type, groupId);
            if (rels.size() > 1) {
                System.out.println(c + " has multiple " + type + " in group " + groupId);
            } else if (rels.size() == 1) {
                //This might not be the full concept, so recover it fully from our loaded cache
                return DataProvider.get().getConcept(Long.parseLong(rels.get(0).getTarget().getConceptId()));
            }
        }
        return null;
    }
    public static Set<Concept> getTargets(Concept c, Concept[] types, CharacteristicType charType){
        Set<Concept> targets = new HashSet<>();
        for (Concept type : types) {
            List<Relationship> rels = c.getRelationships(charType, type, ActiveState.ACTIVE);
            targets.addAll(rels.stream().map(r -> r.getTarget()).collect(Collectors.toSet()));
        }
        return targets;
    }

    public static boolean isSomeLetterCaseSensitive(String term) {
        int len=term.length();
        for(int pos=0;pos<len;pos++) {
            String letter = term.substring(pos, pos + 1);
            if (!letter.equals(letter.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
