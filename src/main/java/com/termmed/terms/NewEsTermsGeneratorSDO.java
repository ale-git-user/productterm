package com.termmed.terms;

import org.apache.commons.lang.StringUtils;
import com.termmed.Data.I_dataProvider;
import com.termmed.model.Concept;
import com.termmed.model.Description;
import com.termmed.model.RF2Constants;
import com.termmed.model.Relationship;
import com.termmed.util.DrugUtils;
import com.termmed.util.SnomedUtils;

import javax.management.AttributeList;
import java.io.*;
import java.util.*;

public class NewEsTermsGeneratorSDO implements RF2Constants {

    private final I_dataProvider dataProvider;
    private final String langRefset;
    private final HashMap<String, String> plurals;
    private final HashSet<String> forEachAcceptance;
    private boolean quiet = false;
    private boolean useEach = false;
    private boolean ptOnly = false;
    private boolean specifyDenominator = false;
    private boolean includeUnitOfPresentation = false;
    private String [] forceCS = new String[] { "N-" };
    private String[] vitamins = new String[] {" A ", " B ", " C ", " D ", " E ", " G "};

    private List<Concept> neverAbbrev = new ArrayList<>();
    private int descCont;
    private HashSet<String> hashControl;
    private HashSet<String> hashPresDose;
    private boolean existsSyn;
    private HashSet<String> HashDoseF;


    public NewEsTermsGeneratorSDO(I_dataProvider dp, String langRefset) {
        this.dataProvider=dp;
        neverAbbrev.add(MICROGRAM);
        neverAbbrev.add(INTERNATIONAL_UNIT);
        neverAbbrev.add(NANOGRAM);
        this.langRefset=langRefset;
        hashControl=new HashSet<String>();
        plurals=new HashMap<String,String>();
        forEachAcceptance=new HashSet<String>();
        hashPresDose=new HashSet<String>();
        descCont=1;
        HashDoseF=new HashSet<String>();
        try {
            loadPlurals();
            loadForEachAcceptance();
        } catch (IOException e) {
            System.out.println("Plurals file doesn't exist.");
        }
    }

    private void loadPlurals() throws IOException {
        File pluralsFile = new File("plurals.txt");
        if (!pluralsFile.exists()){
            return;
        }
        BufferedReader br = getReader(pluralsFile);
        br.readLine();
        String[] spl;
        String line;
        while((line=br.readLine())!=null){
            spl=line.split("\t",-1);
            plurals.put(spl[0],spl[1]);
        }
        br.close();
    }
    private void loadForEachAcceptance() throws IOException {
        File forEachFile = new File("forEachAcceptance.txt");
        if (!forEachFile.exists()){
            return;
        }
        BufferedReader br = getReader(forEachFile);
        br.readLine();
        String[] spl;
        String line;
        while((line=br.readLine())!=null){
            spl=line.split("\t",-1);
            if (!spl[1].toLowerCase().equals("no")) {
                forEachAcceptance.add(spl[0].toLowerCase());
            }
        }
        br.close();
    }

    private boolean ensureDrugTermConforms(Concept c, boolean isFSN, CharacteristicType charType, String newDescriptionModule, String newDescriptionEffectiveTime, String langCode) {
//        boolean changesMade = false;
//        boolean isPT = !isFSN;

        //If it's not the PT or FSN, skip it.   We'll delete later if it's not the FSN counterpart
//        if (!isPT && !isFSN ) {
//            return NO_CHANGES_MADE;
//        }

        //Skip FSNs that contain the word vitamin
		/*if (isFSN && d.getTerm().toLowerCase().contains("vitamin")) {
			report(t, c, Severity.MEDIUM, ReportActionType.NO_CHANGE, "Skipped vitamin FSN");
			return NO_CHANGES_MADE;
		}*/

        //Check the existing term has correct capitalization
//        if (d.getTerm() != null) {
//            ensureCaptialization(d);
//        }
        String replacementTerms[] = calculateTermFromIngredients(c, isFSN, langRefset, charType);
        if (replacementTerms.length<1){
            return false;
        }
        String replacementTerm=replacementTerms[0];
        if (replacementTerm.equals("")){
            return false;
        }
//        if (d.getTerm() != null) {
            replacementTerm = checkForVitamins(replacementTerm, c.getFsnSource());
//        }
        Description replacement;
        Description syn=null;
        String prevTerm;
        if (isFSN) {
            Description descFSN = c.getFSNDescription();
            if (descFSN==null){
                boolean btop=true;
                prevTerm="";
                replacement=createNewDescription(c.getConceptId(), replacementTerm,newDescriptionEffectiveTime, newDescriptionModule, DescriptionType.FSN,"es");
                replacement.setAcceptablity(langRefset,Acceptability.PREFERRED);
            }else {
                prevTerm = descFSN.getTerm();
                replacement = descFSN.clone(null);
                replacement.setTerm(replacementTerm);
            }
            //add syn same as fsn wo semtag
            if (!replacementTerms[1].equals("")) {
                syn = createNewDescription(c.getConceptId(), replacementTerms[1], newDescriptionEffectiveTime, newDescriptionModule, DescriptionType.SYNONYM, "es");
                syn.setAcceptablity(langRefset, Acceptability.ACCEPTABLE);

            }
        }else{
            Description descPref = c.getPreferredSynonym(langRefset);
            if (descPref==null){
                boolean bstop=true;
                replacement=createNewDescription(c.getConceptId(), replacementTerm,newDescriptionEffectiveTime, newDescriptionModule, DescriptionType.SYNONYM,langCode);
                replacement.setAcceptablity(langRefset,Acceptability.PREFERRED);
                prevTerm="";
            }else {
                prevTerm = descPref.getTerm();
                replacement = descPref.clone(null);
                replacement.setTerm(replacementTerm);
            }

        }
        replacement.setAcceptabilityMap(SnomedUtils.createPreferredAcceptableMap(langRefset));

        //Does the case significance of the ingredients suggest a need to modify the term?
        if ( SnomedUtils.isSomeLetterCaseSensitive(replacementTerm)) {
            replacement.setCaseSignificance(CaseSignificance.ENTIRE_TERM_CASE_SENSITIVE);
        } else {
            replacement.setCaseSignificance(CaseSignificance.CASE_INSENSITIVE);
        }

        //We might have a CS ingredient that starts the term.  Check for this, set and warn
//        checkCaseSensitiveOfIngredients( c, replacement, isFSN, charType, langRefset);

        boolean sameFsn=false;
        if (isFSN && prevTerm.equals(replacement.getTerm())){
            sameFsn=true;
        }
        if (isFSN) {
            if (!sameFsn) {
                c.setNewFSNDescription(replacement);
            }
            existsSyn=false;
            if (syn != null) {
                List<Description> descs=c.getDescriptions();
                for(Description descCtrl:descs){
                    if (descCtrl.getTerm().equals(syn.getTerm())){
                        existsSyn=true;
                        break;
                    }
                }
                if (!existsSyn) {
                    syn.setCaseSignificance(replacement.getCaseSignificance());
                    c.setNewSyn(syn);
                }
            }
        }else{
            if (!prevTerm.equals(replacement.getTerm())) {
                c.setNewPreferredDescription(replacement);
            }
        }
        

        //If this is the FSN, then we should have another description without the semantic tag as an acceptable term
        //Update: We're not doing FSN Counterparts now, because of issues with US / GB Variants
		/*if (isFSN) {
			Description fsnCounterpart = replacement.clone(null);
			String counterpartTerm = SnomedUtils.deconstructFSN(fsnCounterpart.getTerm())[0];

			if (!SnomedUtils.termAlreadyExists(c, counterpartTerm)) {
				fsnCounterpart.setTerm(counterpartTerm);
				report(t, c, Severity.LOW, ReportActionType.DESCRIPTION_ADDED, "FSN Counterpart added: " + counterpartTerm);
				fsnCounterpart.setType(DescriptionType.SYNONYM);
				fsnCounterpart.setAcceptabilityMap(SnomedUtils.createAcceptabilityMap(AcceptabilityMode.ACCEPTABLE_BOTH));
				c.addDescription(fsnCounterpart);
			}
		}*/
        return true;
    }

    private Description createNewDescription(String conceptId, String term, String newDescriptionEffectiveTime, String newDescriptionModule, DescriptionType type, String lang) {
        Description d=new Description();
        d.setDescriptionId(String.valueOf(descCont));
        d.setActive(true);
        //Set effective time after active, since changing activate state resets effectiveTime
        d.setEffectiveTime(newDescriptionEffectiveTime);
            d.setReleased(false);
        d.setModuleId(newDescriptionModule);
        d.setCaseSignificance(CaseSignificance.CASE_INSENSITIVE);
        d.setConceptId(conceptId);
        d.setLang(lang);
        d.setTerm(term);
        d.setType(type);

        descCont++;

        return d;
    }

    //    private int replaceTerm( Concept c, Description removing, Description replacement) {
//        int changesMade = 0;
//        boolean doReplacement = true;
//        if (SnomedUtils.termAlreadyExists(c, replacement.getTerm())) {
//            //But does it exist inactive?
//            if (SnomedUtils.termAlreadyExists(c, replacement.getTerm(), ActiveState.INACTIVE)) {
//                reactivateMatchingTerm(t, c, replacement);
//            } else {
//                report(t, c, Severity.MEDIUM, ReportActionType.VALIDATION_CHECK, "Replacement term already exists: '" + replacement.getTerm() + "' inactivating unwanted term only.");
//            }
//            //If we're removing a PT, merge the acceptability into the existing term, also any from the replacement
//            if (removing.isPreferred()) {
//                mergeAcceptability(t, c, removing, replacement);
//            }
//            doReplacement = false;
//        }
//
//        //Has our description been published?  Remove entirely if not
//        boolean isInactivated = removeDescription(c,removing);
//        String msg = (isInactivated?"Inactivated desc ":"Deleted desc ") +  removing;
//        changesMade++;
//
//        if (doReplacement) {
//            c.addDescription(replacement);
//        }
//
//        return changesMade;
//    }
    public NewEsTermsGeneratorSDO includeUnitOfPresentation(Boolean state) {
        includeUnitOfPresentation = state;
        return this;
    }

    public boolean includeUnitOfPresentation() {
        return includeUnitOfPresentation;
    }

    public void ensureCaptialization(Description d) {
        String term = d.getTerm();
        for (String checkStr : forceCS) {
            if (term.startsWith(checkStr)) {
                d.setCaseSignificance(CaseSignificance.ENTIRE_TERM_CASE_SENSITIVE);
            }
        }
    }

    public String checkForVitamins(String term, String origTerm) {
        //See if we've accidentally made vitamin letters lower case and switch back
        for (String vitamin : vitamins) {
            if (origTerm.contains(vitamin)) {
                term = term.replace(vitamin.toLowerCase(), vitamin);
            } else if (origTerm.endsWith(vitamin.substring(0, 2))) {
                //Is it still at the end?
                if (term.endsWith(vitamin.toLowerCase().substring(0, 2))) {
                    term = term.replace(vitamin.toLowerCase().substring(0, 2), vitamin.substring(0, 2));
                } else {
                    //It should now have a space around it
                    term = term.replace(vitamin.toLowerCase(), vitamin);
                }
            }
        }
        return term;
    }
    private void checkCaseSensitiveOfIngredients( Concept c, Description d, boolean isFSN,
                                                 CharacteristicType charType, String langRefsetId) {
        if (d.getCaseSignificance().equals(CaseSignificance.CASE_INSENSITIVE)) {
            for (Concept ingred : DrugUtils.getIngredients(c, charType)) {
                ingred = dataProvider.getConcept(Long.parseLong(ingred.getConceptId()));
                Description ingredDesc = isFSN ? ingred.getFSNDescription() : ingred.getPreferredSynonym(langRefsetId);
                if (ingredDesc==null){
                    boolean bstop=true;
                }
                if (ingredDesc.getCaseSignificance()==null){
                    boolean bstop=true;
                }
                if (ingredDesc.getCaseSignificance().equals(CaseSignificance.ENTIRE_TERM_CASE_SENSITIVE)) {
                    d.setCaseSignificance(CaseSignificance.ENTIRE_TERM_CASE_SENSITIVE);
                }
            }
        }
    }

    public String[] calculateTermFromIngredients(Concept c, boolean isFSN, String langRefset, CharacteristicType charType) {
        String proposedTerm = "";
        String semTag = "";
        boolean ptContaining = false;
//        if (isFSN && c.getFsn() != null) {
//            semTag = SnomedUtils.deconstructFSN(c.getFsn())[1];
//        }
        //Get all the ingredients in order
        Set<String> ingredients = getIngredientsWithStrengths(c, isFSN, langRefset, charType);

        if (ingredients.size()==0){
            return new String[]{};
        }
        //What prefixes and suffixes do we need?
        String prefix = "";
        String suffix = "";
        boolean addUnicoIngred=false;
        if (c.getConceptType() == null) {
            SnomedUtils.populateConceptType(c);
        }

        if (isFSN) {
            ;
            switch (c.getConceptType()) {
                case MEDICINAL_PRODUCT:
                    prefix = "producto que contiene ";

                    if (c.getFsnSource().contains(" only ")) {
                        addUnicoIngred=true ;
                    }else if (c.getFsnSource().contains(" precisely ")){
                        prefix+="exactamente ";
                    }
                    semTag = "(producto medicinal)";
                    break;
                case MEDICINAL_PRODUCT_FORM :
                    prefix = "producto que contiene ";
                    if (c.getFsnSource().contains(" only ")) {
                        addUnicoIngred=true ;
                    }else if (c.getFsnSource().contains(" precisely ")){
                        prefix+="exactamente ";
                    }
                    suffix =  " en " + DrugUtils.getDosageForm(c, isFSN, langRefset);
                    semTag = "(forma farmacéutica de producto medicinal)";
                    break;
                case CLINICAL_DRUG :
                    prefix = "producto que contiene exactamente ";
                    //TODO Check that presentation is solid before adding 1 each
                    suffix =  getCdSuffix(c, isFSN, langRefset);
                    semTag = "(fármaco de uso clínico)";
                    break;
                case PRODUCT:
                    prefix = "producto que contiene ";
                    semTag = "(producto)";
                    break;
                default:
                    return new String[]{};

            }
        } else {
            switch (c.getConceptType()) {
                case MEDICINAL_PRODUCT:
                    prefix = "producto ";

                    if (c.getFsnSource().contains(" only ")) {
                        addUnicoIngred=true ;
                    }else if (c.getFsnSource().contains(" precisely ")) {
                        prefix += "exactamente ";
                    }
                    prefix += "con ";
                    ptContaining = true;
                    break;
                case MEDICINAL_PRODUCT_FORM:
                    prefix = "producto ";
                    String doseForm = DrugUtils.getDosageForm(c, isFSN, langRefset);
                    if (!HashDoseF.contains(doseForm)) {
                        HashDoseF.add(doseForm);

                        System.out.println(doseForm);
                    }
                    if (doseForm.toLowerCase().startsWith("forma farmacéutica")){
                        suffix = doseForm.replace("forma farmacéutica","");
                    }else {
                        suffix = " en " + doseForm;
                    }
                    if (c.getFsnSource().contains(" only ")){
                        addUnicoIngred=true ;
                    }else if (c.getFsnSource().contains(" precisely ")){
                        prefix+="exactamente ";
                    }
                    prefix += "con ";
                    ptContaining = true;
                    break;
                case CLINICAL_DRUG :
                    suffix = getCdSuffix(c, isFSN, langRefset);
                    break;
                case PRODUCT:
                    prefix = "producto con ";
                    break;
                default:
                    return new String[]{};
            }
        }

        //Are we adding "-" to every ingredient to indicate they're all containing?
//        if (ptContaining) {
//            Set<String> tempSet = new HashSet<>();
//            for (String ingredient : ingredients) {
//                tempSet.add(ingredient + "-");
//            }
//            ingredients.clear();
//            ingredients.addAll(tempSet);
//        }

        //Form the term from the ingredients with prefixes and suffixes as required.
//        proposedTerm = prefix + StringUtils.join(ingredients, " y ") + suffix;

        proposedTerm= prefix + joinIngredients(ingredients) + suffix;
        if (addUnicoIngred){
            if (ingredients.size()>1){
                proposedTerm+=" como únicos ingredientes";
            }else{
                proposedTerm+=" como único ingrediente";
            }
        }
        if (isFSN) {
//            proposedTerm += " " + semTag;
            return new String[]{proposedTerm + " " + semTag, proposedTerm };
        }
//        proposedTerm = SnomedUtils.capitalize(proposedTerm);
        return new String[]{proposedTerm, ""};
    }

    private String joinIngredients(Set<String> ingredients) {
        StringBuffer sb=new StringBuffer();
        int lastIngred=ingredients.size();
        int cont=1;
        for(String ingredient: ingredients){
//            sb.append( ingredient);
            if (cont>1 && cont<lastIngred) {
                sb.append(", ");
            }else if (cont>1 && cont==lastIngred) {
                if (ingredient.toLowerCase().startsWith("i") || ingredient.toLowerCase().startsWith("hi")) {
                    sb.append(" e ");
                }else {
                    sb.append(" y ");
                }
            }
            sb.append(ingredient);
            cont++;
        }
        return sb.toString();
    }

    private Set<String> getIngredientsWithStrengths(Concept c, boolean isFSN, String langRefset, CharacteristicType charType) {
        List<Relationship> ingredientRels = c.getRelationships(charType, HAS_ACTIVE_INGRED, ActiveState.ACTIVE);
        ingredientRels.addAll(c.getRelationships(charType, HAS_PRECISE_INGRED, ActiveState.ACTIVE));
        Set<String> ingredients = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);  //Will naturally sort in alphabetical order

        for (Relationship r : ingredientRels) {
            //Need to recover the full concept to have all descriptions, not the partial one stored as the target.

            Concept ingredient = dataProvider.getConcept(Long.parseLong(r.getTarget().getConceptId()));

            //Do we have a BoSS in the same group?
            Concept boSS = SnomedUtils.getTarget(c, new Concept[] {HAS_BOSS}, r.getGroupId(), charType);

            //Are we adding the strength?
            Concept strength = SnomedUtils.getTarget (c, new Concept[] {HAS_PRES_STRENGTH_VALUE, HAS_CONC_STRENGTH_VALUE}, r.getGroupId(), charType);

            //Are we adding the denominator strength and units?
            String denominatorStr = "";
//            if (specifyDenominator || hasAttribute(c, HAS_PRES_STRENGTH_DENOM_VALUE) || hasAttribute(c, HAS_CONC_STRENGTH_DENOM_VALUE)) {
            if (specifyDenominator || hasAttribute(c, HAS_CONC_STRENGTH_DENOM_VALUE)) {
                denominatorStr = "/";
                Concept denStren = SnomedUtils.getTarget (c, new Concept[] {HAS_PRES_STRENGTH_DENOM_VALUE, HAS_CONC_STRENGTH_DENOM_VALUE}, r.getGroupId(), charType);
                String denStrenStr = SnomedUtils.deconstructFSN(denStren.getFsn())[0];
                boolean bsingl=true;
                if (!denStrenStr.equals("1") || isFSN) {
                    denominatorStr += denStrenStr + " ";
                    if (!denStrenStr.equals("1")) {
                        bsingl = false;
                    }
                }
                Concept denUnit = SnomedUtils.getTarget (c, new Concept[] {HAS_PRES_STRENGTH_DENOM_UNIT, HAS_CONC_STRENGTH_DENOM_UNIT}, r.getGroupId(), charType);
                String denUnitStr = getTermForConcat(denUnit, isFSN || neverAbbrev.contains(denUnit), langRefset);
                if (bsingl ){
                    denominatorStr += denUnitStr;
                }else {
                    denominatorStr += getPlural(denUnitStr);
                }
            }

            //And the unit
            Concept unit = SnomedUtils.getTarget(c, new Concept[] {HAS_PRES_STRENGTH_UNIT, HAS_CONC_STRENGTH_UNIT}, r.getGroupId(), charType);

            String ingredientWithStrengthTerm = formIngredientWithStrengthTerm (ingredient, boSS, strength, unit, denominatorStr, isFSN, langRefset);
            ingredients.add(ingredientWithStrengthTerm);
        }
        return ingredients;
    }

    private String formIngredientWithStrengthTerm(Concept ingredient, Concept boSS, Concept strength, Concept unit, String denominatorStr, boolean isFSN, String langRefset)  {
        boolean separateBoSS = (boSS!= null && !boSS.equals(ingredient));
        String ingredientTerm="";

        //First the ingredient, with the BoSS first if different
        if (separateBoSS) {
            ingredientTerm = getTermForConcat(boSS, isFSN, langRefset);
            ingredientTerm += " (como ";
        }

        ingredientTerm += getTermForConcat(ingredient, isFSN, langRefset);

        if (separateBoSS) {
            ingredientTerm += ")";
        }

        boolean bSingl=true;
        //Now add the Strength
        if (strength != null) {

            String strengthTerm=SnomedUtils.deconstructFSN(strength.getFsn())[0];
            try{
                float strengthNumber=Float.parseFloat(strengthTerm);


                if (strengthNumber!= 1f){
                    bSingl=false;
                }
            }catch (Exception e){
                bSingl=false;
//                System.out.println("Not as number:" + strengthTerm);
            }
            ingredientTerm += " " + strengthTerm;
        }

        if (unit != null) {
            String unitTerm=getTermForConcat(unit, isFSN || neverAbbrev.contains(unit), langRefset);
            if (bSingl ) {
                ingredientTerm += " " + unitTerm;
            }else{
                ingredientTerm += " " + getPlural(unitTerm);
            }
        }

        ingredientTerm += denominatorStr;

        return ingredientTerm;
    }

    private String getPlural(String unitTerm) {
        if (!hashControl.contains(unitTerm)) {
//            System.out.println(unitTerm);
            hashControl.add(unitTerm);
        }
        String plural=plurals.get(unitTerm);
        if (plural!=null){
            return plural;
        }
        return unitTerm;
    }

    private String getTermForConcat(Concept c, boolean useFSN, String langRefset)  {
        Description desc;
        String term;
        if (useFSN) {
            desc = c.getFSNDescription();
            if (desc==null){
                System.out.println(c.getConceptId());

            }
            term = SnomedUtils.deconstructFSN(desc.getTerm())[0];
        } else {
            desc = c.getPreferredSynonym(langRefset);
            if (desc==null){
                boolean bstop=true;
            }
            term = desc.getTerm();
        }
        if (!desc.getCaseSignificance().equals(CaseSignificance.ENTIRE_TERM_CASE_SENSITIVE)) {
            term = SnomedUtils.deCapitalize(term);
        }
        return term;
    }

    private boolean hasAttribute(Concept c, Concept attrib) {
        //Dose this concept specify a concentration denominator?
        return c.getRelationships(CharacteristicType.STATED_RELATIONSHIP, attrib, ActiveState.ACTIVE).size() > 0;
    }

    private String getCdSuffix(Concept c, boolean isFSN, String langRefset) {
        String suffix="";
        if ( c.getConceptId().equals("331164004")){
            boolean bstop=true;
        }
        String unitOfPresentation = DrugUtils.getAttributeType(c, HAS_UNIT_OF_PRESENTATION, isFSN, langRefset);
        String doseForm = DrugUtils.getDosageForm(c, isFSN, langRefset);
        boolean isActuation = unitOfPresentation.equals("disparo");

        if (includeUnitOfPresentation ||
                (hasAttribute(c, HAS_UNIT_OF_PRESENTATION)
                        && !doseForm.endsWith(unitOfPresentation)
                        && !doseForm.startsWith(unitOfPresentation)
                        && !matchsBetweenSimilar(doseForm,unitOfPresentation))) {
            hashPresDose.add((unitOfPresentation + " de "  + doseForm).substring(0,(unitOfPresentation + " de "  + doseForm).indexOf(" ")));
            if ((isFSN  && !specifyDenominator) || isActuation) {
                suffix =  unitOfPresentation + " de "  + doseForm;

            } else {
//                suffix = " " + doseForm + " "  + unitOfPresentation;
                suffix =  unitOfPresentation + " de "  + doseForm;
            }
        } else {
            if (doseForm.indexOf(" ")>0){

                hashPresDose.add(doseForm.substring(0, doseForm.indexOf(" ")));
            }else {
                hashPresDose.add(doseForm);
            }
            if (isFSN && !specifyDenominator && !hasAttribute(c, HAS_CONC_STRENGTH_DENOM_VALUE)) {
                suffix =  doseForm;
            } else {
                suffix =  doseForm;
            }
        }
        suffix=getForEachOrComma(suffix);
        return suffix;
    }

    private boolean matchsBetweenSimilar(String doseForm, String unitOfPresentation) {
        String[] splDosage=doseForm.split(" ",-1);
        String[] splPres=unitOfPresentation.split(" ",-1);
        if (splDosage[0].toLowerCase().equals("pastilla")
                && splPres[0].toLowerCase().equals("comprimido")){
            return true;
        }
        return splDosage[0].toLowerCase().equals(splPres[0].toLowerCase());
    }

    private String getForEachOrComma(String suffix) {
        String[] spl=suffix.split(" ");
        String ret="";
        if (forEachAcceptance.contains(spl[0].toLowerCase())){
            ret=" por cada " + suffix;
        }else{
            ret=", " + suffix;
        }
        return ret;
    }

    public void execute(String outputFile, String newDescriptionModule, String newDescriptionEffectiveTime, String langCode) throws IOException {
        BufferedWriter bw = getWriter(outputFile);
        addHeader(bw);
        boolean change;
        for(Long cid:dataProvider.getConceptToProcess()) {
            Concept concept = dataProvider.getConcept(cid);
            if (!concept.getFsnSource().startsWith("concept code")) {
                System.out.println("NO terms for because of:" + concept.getFsnSource() + ", cid:" + cid);
            }
            if (!concept.getFsnSource().contains("(medicinal product)") && !concept.getFsnSource().contains("(medicinal product form)")
                    && !concept.getFsnSource().contains("(clinical drug)")
            ){
                System.out.println("NO terms for:" + concept.getFsnSource() + ", cid:" + cid);
                continue;
            }
            change = ensureDrugTermConforms(concept, true, CharacteristicType.STATED_RELATIONSHIP, newDescriptionModule, newDescriptionEffectiveTime, langCode);
            if (change) {
                change=ensureDrugTermConforms(concept, false, CharacteristicType.STATED_RELATIONSHIP, newDescriptionModule, newDescriptionEffectiveTime, langCode);
                if (concept.getNewPreferredDescription() != null || concept.getNewFSNDescription() !=null) {
                    addConceptToFile(bw, concept);
                }else{
                    System.out.println("NO generated terms for:" + cid.toString());
                }
            }else{
                System.out.println("NO changes generated terms for:" + cid.toString());
            }
        }
        bw.close();
//        for (String presDose:hashPresDose){
//            System.out.println(presDose);
//        }
    }

    private void addHeader(BufferedWriter bw) throws IOException {
        bw.append("conceptId\tSource_FSN\tOld_FSN\tNew_FSN\tFSN_Case_Significance\tNew_Preferred\tPreferred_Case_Significance\tNew_Synonym\tSyn_Case_Significance\r\n");
    }

    private void addConceptToFile(BufferedWriter bw, Concept concept) throws IOException {
        bw.append(concept.getConceptId());
        bw.append("\t");
        bw.append(concept.getFsnSource());
        bw.append("\t");
        if (concept.getFSNDescription()!=null) {
            bw.append(concept.getFSNDescription().getTerm());
        }else {
            bw.append("");
        }
        bw.append("\t");
        if (concept.getNewFSNDescription()!=null) {
            bw.append(concept.getNewFSNDescription().getTerm());
            bw.append("\t");
            bw.append(concept.getNewFSNDescription().getCaseSignificance().name());
        }else {
            bw.append("");
            bw.append("\t");
            bw.append("");
        }
        bw.append("\t");
        if (concept.getNewPreferredDescription()!=null) {
            bw.append(concept.getNewPreferredDescription().getTerm());
            bw.append("\t");
            bw.append(concept.getNewPreferredDescription().getCaseSignificance().name());
        }else {
            bw.append("");
            bw.append("\t");
            bw.append("");
        }
        bw.append("\t");
        if (concept.getNewSyn()!=null) {
            bw.append(concept.getNewSyn().getTerm());
            bw.append("\t");
            bw.append(concept.getNewSyn().getCaseSignificance().name());
        }else {
            bw.append("");
            bw.append("\t");
            bw.append("");
        }
        bw.append("\r\n");

    }

    public BufferedWriter getWriter(String outFile) throws UnsupportedEncodingException, FileNotFoundException {

        FileOutputStream tfos = new FileOutputStream( outFile);
        OutputStreamWriter tfosw = new OutputStreamWriter(tfos,"UTF-8");
        return new BufferedWriter(tfosw);

    }


    private BufferedReader getReader(File inFile) throws UnsupportedEncodingException, FileNotFoundException {

        FileInputStream rfis = new FileInputStream(inFile);
        InputStreamReader risr = new InputStreamReader(rfis,"UTF-8");
        BufferedReader rbr = new BufferedReader(risr);
        return rbr;

    }
}
