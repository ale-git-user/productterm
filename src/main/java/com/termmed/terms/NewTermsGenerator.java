package com.termmed.terms;

import com.termmed.util.TClosure;

import java.io.*;
import java.util.*;

/**
 *
 */
public class NewTermsGenerator {

    private static final String RELEASE_DATE = "20180731" ;
    private static final int CONCEPTID = 0;
    private static final int FSN =1;
    private static final int DOSE_FORM = 4;
    private static final int INGREDIENT = 5;
    private static final int BOSS =6 ;
    private static final int NMRTOR_VAL = 7;
    private static final int NMRTOR_UNIT = 8 ;

    private static final String SYNONYM_TYPE = "900000000000013009";
    private static final String PREFERRED_ACCEPTABILITY = "900000000000548007";
    private static final String US_REFSETID = "900000000000509007";
    private static final String ACCEPTABLE_ACCEPTABILITY ="900000000000549004" ;
    private static final String GB_REFSETID = "900000000000508004";
    private static final String FSN_TYPE = "900000000000003001";
    private static final String CORE_MODULE = "900000000000207008";
    private static final String ENTIRE_TERM_INSENSITIVE = "900000000000448009" ;
    private static final String DOSE_PHARMACEUTICAL="736542009";
    private static final String UNIT ="258666001" ;
    private final String rels;
    private HashMap<String, List<ExtendedLineString>> hCptList;
    private HashMap<String, String> hCptFSN;
    private String outDescFile;
    private String FileBase;
    private String descriptions;
    private String language;
    private HashMap<Long, Preferreds> hCptPref;
    private static final int FSN_IX=0;
    private static final int USPREF_IX=1;
    private static final int GBPREF_IX=2;
    private static final int SYN_IX=3;
    private String outLangFile;
    private Long gDescriptionId;
    private HashMap<String, String> dosePharmaTerms;
    private HashMap<String, String> unitTerms;
    private String outTermsReview;

    public static void main(String[] args){

        String FileBase="{path to file with product data -csv or txt-}";
        String descriptions="{path to Snomed description file in RF2}";
        String language="{path to Snomed language file in RF2}";
        String rels="{path to Snomed relationships file in RF2}";

        String outDescFile ="{output for new RF2 descriptions}";
        String outLangFile="{output for new RF2 language}";
        String outTermsReview="{output for term denormalized list for review}";

        NewTermsGenerator fg=new NewTermsGenerator(
                FileBase, descriptions,language, rels, outDescFile, outLangFile, outTermsReview);

        try {
            fg.execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param fileBase: path to file with product data -csv or txt-
     * @param descriptions: path to Snomed description file in RF2
     * @param language: path to Snomed language file in RF2
     * @param rels: output for new RF2 descriptions file
     * @param outDescFile: output for new RF2 language file
     * @param outLangFile: output for term denormalized list file for review
     * @param outTermsReview: path to Snomed relationships file in RF2
     */
    public NewTermsGenerator(String fileBase, String descriptions, String language, String rels, String outDescFile, String outLangFile, String outTermsReview) {
        this.FileBase=fileBase;
        this.descriptions=descriptions;
        this.language=language;
        this.outDescFile =outDescFile;
        this.outLangFile=outLangFile;
        this.outTermsReview=outTermsReview;
        this.rels=rels;
    }

    public void execute() throws IOException {
         gDescriptionId=0l;

        TClosure tClos=new TClosure(rels);
        dosePharmaTerms =getDescendantDescriptions(tClos,descriptions,DOSE_PHARMACEUTICAL);
        unitTerms=getDescendantDescriptions(tClos,descriptions,UNIT);
        dataRetrieve();

        sendToRF2();

    }


    private void dataRetrieve() throws IOException {

        BufferedReader br = getReader(FileBase);

        String header = br.readLine();
        header = getTabuledLine(header);

        String line;
        String[] spl;
        hCptList = new HashMap<String, List<ExtendedLineString>>();
        hCptFSN = new HashMap<String, String>();
        hCptPref=new HashMap<Long,Preferreds>();
        String boss;
        String ingrd;
        String nmrtorUnit;
        String doseForm;
        while ((line = br.readLine()) != null) {

            line = getTabuledLine(line);
            spl = line.split("\t", -1);
            String cid = spl[CONCEPTID];

            List<ExtendedLineString> lst = hCptList.get(cid);
            if (lst == null) {
                lst = new ArrayList<ExtendedLineString>();
            }

            lst.add(new ExtendedLineString(line,"fsn"));
            hCptList.put(cid, lst);

            hCptFSN.put(cid, spl[FSN]);

            boss=getId(spl[BOSS]);
            if (boss!=null) {
                hCptPref.put(Long.parseLong(boss), null);
            }
            ingrd=getId(spl[INGREDIENT]);
            if (ingrd!=null) {
                hCptPref.put(Long.parseLong(ingrd), null);
            }
            if (spl[NMRTOR_UNIT]!=null && !spl[NMRTOR_UNIT].trim().toLowerCase().contains("microgram")) {
                nmrtorUnit = getId(spl[NMRTOR_UNIT]);
                if (nmrtorUnit != null) {
                    hCptPref.put(Long.parseLong(nmrtorUnit), null);
                } else {
                    nmrtorUnit = getUnitData(spl[NMRTOR_UNIT]);
                    if (nmrtorUnit != null) {
                        hCptPref.put(Long.parseLong(nmrtorUnit), null);
                    } else {
                        System.out.println("Unit not found:" + spl[NMRTOR_UNIT]);
                    }
                }
            }
            doseForm=getId(spl[DOSE_FORM]);
            if (doseForm!=null) {
                hCptPref.put(Long.parseLong(doseForm), null);
            }else{
                doseForm=getDoseData(spl[DOSE_FORM]);
                if (doseForm!=null) {
                    hCptPref.put(Long.parseLong(doseForm), null);
                }else{
                    System.out.println("Pharma dose not found:" + spl[DOSE_FORM]);
                }
            }
        }
        br.close();

        getPreferredTermList();

    }

    private String getDoseData(String term) {
        String doseId=null;
        if (!term.trim().equals("")){
            term=term.toLowerCase().trim();
            doseId=dosePharmaTerms.get(term);

        }
        return doseId;
    }
    private String getUnitData(String term) {
        String unitId=null;
        if (!term.trim().equals("")){
            term=term.toLowerCase().trim();
            unitId=unitTerms.get(term);

        }
        return unitId;
    }

    private void sendToRF2() throws IOException {

        BufferedWriter bw = getWriter(outDescFile);
        bw.append("id\teffectiveTime\tactive\tmoduleId\tconceptId\tlanguageCode\ttypeId\tterm\tcaseSignificanceId\r\n");

        BufferedWriter bwl = getWriter(outLangFile);
        bwl.append("id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\tacceptabilityId\r\n");

        BufferedWriter bwgt = getWriter(outTermsReview);
        bwgt.append("id\tfsn\tnew_FSN\tnew_us_preferred\tnew_synonym\tnew_gb_preferred\r\n");

        Long newDescId;
        for (String cid:hCptList.keySet()){
            List<ExtendedLineString> lst=hCptList.get(cid);

            String[] terms= getNewTerms(lst);

            newDescId=addDescriptionLine(bw, cid,terms[FSN_IX],FSN_TYPE);
            addLanguageLine(bwl,newDescId,US_REFSETID,PREFERRED_ACCEPTABILITY);
            addLanguageLine(bwl,newDescId,GB_REFSETID,PREFERRED_ACCEPTABILITY);

            newDescId=addDescriptionLine(bw, cid,terms[USPREF_IX],SYNONYM_TYPE);
            addLanguageLine(bwl,newDescId,US_REFSETID,PREFERRED_ACCEPTABILITY);

            if (terms[GBPREF_IX]!=null) {
                newDescId=addDescriptionLine(bw, cid, terms[GBPREF_IX], SYNONYM_TYPE);
            }
            addLanguageLine(bwl,newDescId,GB_REFSETID,PREFERRED_ACCEPTABILITY);

            newDescId=addDescriptionLine(bw, cid,terms[SYN_IX],SYNONYM_TYPE);
            addLanguageLine(bwl,newDescId,US_REFSETID,ACCEPTABLE_ACCEPTABILITY);
            addLanguageLine(bwl,newDescId,GB_REFSETID,ACCEPTABLE_ACCEPTABILITY);

            sendToTabularList(bwgt, cid, terms);
        }
        bw.close();
        bwl.close();
        bwgt.close();

    }

    private void sendToTabularList(BufferedWriter bwgt, String cid, String[] terms) throws IOException {

        bwgt.append(cid);
        bwgt.append("\t");
        bwgt.append(hCptFSN.get(cid));
        bwgt.append("\t");
        bwgt.append(terms[FSN_IX]);
        bwgt.append("\t");
        bwgt.append(terms[USPREF_IX]);
        bwgt.append("\t");
        bwgt.append(terms[SYN_IX]);
        bwgt.append("\t");
        if (terms[GBPREF_IX]!=null) {
            bwgt.append(terms[GBPREF_IX]);
        }else{
            bwgt.append(terms[USPREF_IX]);
        }
        bwgt.append("\r\n");

    }
    private void addLanguageLine(BufferedWriter bwl, Long newDescId, String refsetId, String acceptability) throws IOException {
        bwl.append(UUID.randomUUID().toString());
        bwl.append("\t");
        bwl.append(RELEASE_DATE);
        bwl.append("\t");
        bwl.append("1");
        bwl.append("\t");
        bwl.append(CORE_MODULE);
        bwl.append("\t");
        bwl.append(refsetId);
        bwl.append("\t");
        bwl.append(newDescId.toString());
        bwl.append("\t");
        bwl.append(acceptability);
        bwl.append("\r\n");
    }

    private Long addDescriptionLine(BufferedWriter bw, String cid, String term, String type) throws IOException {
        gDescriptionId++;
        bw.append(String.valueOf(gDescriptionId));
        bw.append("\t");
        bw.append(RELEASE_DATE);
        bw.append("\t");
        bw.append("1");
        bw.append("\t");
        bw.append(CORE_MODULE);
        bw.append("\t");
        bw.append(cid);
        bw.append("\t");
        bw.append("en");
        bw.append("\t");
        bw.append(type);
        bw.append("\t");
        bw.append(term);
        bw.append("\t");
        bw.append(ENTIRE_TERM_INSENSITIVE);
        bw.append("\r\n");

        return gDescriptionId;
    }

    private String[] getNewTerms(List<ExtendedLineString> lst) {
        String[] retTerms=new String[4];
        String fsn=null;
        String prefUS=null;
        String prefGB=null;
        String syn=null;
        String[] spl;

        if (lst.size()==1){
            spl=lst.get(0).getLine().split("\t",-1);

            if (spl[INGREDIENT].equals(spl[BOSS])) {
                fsn = "Product containing only " + unCapitalize(removeSemtag(getTerm(spl[BOSS]))) + " " +
                        spl[NMRTOR_VAL] + " " + unCapitalize(removeSemtag(getTerm(spl[NMRTOR_UNIT]))) + "/1 each " + unCapitalize(removeSemtag(getTerm(spl[DOSE_FORM])));
                prefUS = capitalize(getPreferred(spl[BOSS],"us")) + " " +
                        spl[NMRTOR_VAL] + " " + unCapitalize(getPreferred(spl[NMRTOR_UNIT],"us")) + " " + unCapitalize(getPreferred(spl[DOSE_FORM],"us"));
                prefGB = capitalize(getPreferred(spl[BOSS],"gb")) + " " +
                        spl[NMRTOR_VAL] + " " + unCapitalize(getPreferred(spl[NMRTOR_UNIT],"gb")) + " " + unCapitalize(getPreferred(spl[DOSE_FORM],"gb"));
            } else {
                fsn = "Product containing only " + unCapitalize(removeSemtag(getTerm(spl[BOSS]))) + " (as " + unCapitalize(removeSemtag(getTerm(spl[INGREDIENT]))) + ") " +
                        spl[NMRTOR_VAL] + " " + unCapitalize(removeSemtag(getTerm(spl[NMRTOR_UNIT]))) + "/1 each " + unCapitalize(removeSemtag(getTerm(spl[DOSE_FORM])));
                prefUS = capitalize(getPreferred(spl[BOSS],"us")) + " (as " + unCapitalize(getPreferred(spl[INGREDIENT],"us")) + ") " +
                        spl[NMRTOR_VAL] + " " + unCapitalize(getPreferred(spl[NMRTOR_UNIT],"us")) + " " + unCapitalize(getPreferred(spl[DOSE_FORM],"us"));
                prefGB = capitalize(getPreferred(spl[BOSS],"gb")) + " (as " + unCapitalize(getPreferred(spl[INGREDIENT],"gb")) + ") " +
                        spl[NMRTOR_VAL] + " " + unCapitalize(getPreferred(spl[NMRTOR_UNIT],"gb")) + " " + unCapitalize(getPreferred(spl[DOSE_FORM],"gb"));

            }


        }else {
            boolean first=true;
            String doseform="";
            Collections.sort(lst);
            String cid="";
            for (ExtendedLineString extendedLine : lst) {
                String line=extendedLine.getLine();
                spl = line.split("\t", -1);
                cid=spl[CONCEPTID].trim();
                if (first){
                    fsn = "Product containing only " + unCapitalize(removeSemtag(getTerm(spl[BOSS]))) + " " ;
                    doseform=unCapitalize(removeSemtag(getTerm(spl[DOSE_FORM])));
                    first=false;
                }else{
                    fsn +=" and " + unCapitalize(removeSemtag(getTerm(spl[BOSS]))) + " " ;
                }
                if (!spl[INGREDIENT].equals(spl[BOSS])) {
                    fsn += "(as " + unCapitalize(removeSemtag(getTerm(spl[INGREDIENT]))) + ") " ;
                }
                fsn += spl[NMRTOR_VAL] + " " + unCapitalize(removeSemtag(getTerm(spl[NMRTOR_UNIT])));
            }

            List<ExtendedLineString>sortedUSPrf=getSortedByTermType(lst, "us");
            first=true;
            for (ExtendedLineString extendedLine : sortedUSPrf) {
                String line=extendedLine.getLine();
                spl = line.split("\t", -1);
                if (first){
                    prefUS = capitalize(getPreferred(spl[BOSS], "us")) + " ";
                    first=false;
                }else{
                    prefUS +=" and " + unCapitalize(getPreferred(spl[BOSS], "us")) + " ";
                }
                if (!spl[INGREDIENT].equals(spl[BOSS])) {
                    prefUS += "(as " + unCapitalize(getPreferred(spl[INGREDIENT],"us")) + ") " ;
                }
                prefUS += spl[NMRTOR_VAL] + " " + unCapitalize(getPreferred(spl[NMRTOR_UNIT],"us")) ;
            }
            List<ExtendedLineString>sortedGBPrf=getSortedByTermType(lst, "gb");
            first=true;
            for (ExtendedLineString extendedLine : sortedGBPrf) {
                String line=extendedLine.getLine();
                spl = line.split("\t", -1);
                if (first){
                    prefGB = capitalize(getPreferred(spl[BOSS], "gb")) + " ";
                    first=false;
                }else{
                    prefGB +=" and " + unCapitalize(getPreferred(spl[BOSS], "gb")) + " ";
                }
                if (!spl[INGREDIENT].equals(spl[BOSS])) {
                    prefGB += "(as " + unCapitalize(getPreferred(spl[INGREDIENT],"gb")) + ") " ;
                }
                prefGB += spl[NMRTOR_VAL] + " " + unCapitalize(getPreferred(spl[NMRTOR_UNIT],"gb")) ;
            }
            fsn +=  "/1 each " + doseform;
            prefUS +=  " " + doseform;
            prefGB +=  " " + doseform;
        }
        fsn=fsn.trim();
        prefUS=prefUS.trim();
        prefGB=prefGB.trim();
        syn=fsn;
        fsn+= " (clinical drug)";
        retTerms[FSN_IX]=fsn;
        retTerms[USPREF_IX]=prefUS;
        if (!prefGB.equals(prefUS)) {
            retTerms[GBPREF_IX] = prefGB;
        }
        retTerms[SYN_IX]=syn;
        return retTerms;
    }

    private List<ExtendedLineString> getSortedByTermType(List<ExtendedLineString> lst, String termType) {
        for (ExtendedLineString extendedLine: lst){
            extendedLine.setCompareTermType(termType);
        }
        Collections.sort(lst);
        return lst;
    }

    protected String getPreferred(String definition, String lang) {
        if (definition!=null) {
            String id=getId(definition);
            if (id==null) {
                id = getDoseData(definition);
                if (id == null) {
                    id = getUnitData(definition);
                }
            }
            if (id!=null){
                try {
                    Preferreds prefs = hCptPref.get(Long.parseLong(id));
                    if (prefs != null) {
                        if (lang.equals("gb")) {
                            return prefs.getEnGB();
                        }else{
                            return prefs.getEnUS();
                        }
                    }
                }catch(Exception e){
                    System.out.println("Cannot parse id from:" + definition);
                }
                return removeSemtag(definition);
            }
        }
        return removeSemtag(definition);
    }

    protected String getTerm(String definition) {
        if (definition.trim().length()>1 ){
            int pos=definition.indexOf("|");
            if (pos>-1) {
                String term = definition.substring( pos+1);
                if (term.trim().endsWith("|")){
                    term=term.substring(0,term.trim().length()-1);
                }
                return term.trim();
            }
        }
        return definition.trim();
    }

    private String getId(String definition) {
        if (definition!=null) {
            if (definition.trim().length() > 1) {
                int pos = definition.indexOf("|");
                if (pos > -1) {
                    String id = definition.substring(0, pos);
                    return id.trim();
                }
                try{
                    Long.parseLong(definition.trim());
                    return definition.trim();
                }catch (Exception e){
                }
            }
        }
        return null;
    }

    private String getTabuledLine(String line) {
        line = line.replaceAll("\",\"", "\t");
        if (line.startsWith("\"")) {
            line = line.substring(1);
        }
        if (line.endsWith("\"")) {
            line = line.substring(0, line.length() - 1);
        }
        return line;
    }

    private String unCapitalize(String term) {
        if (term.length()>0) {
            return term.substring(0, 1).toLowerCase() + term.substring(1);
        }
        return "";
    }

    private String capitalize(String term) {
        if (term.length()>0) {
            return term.substring(0, 1).toUpperCase() + term.substring(1);
        }
        return "";
    }

    private String removeSemtag(String term) {
        if (term!=null) {
            if (term.trim().endsWith(")")) {
                if (term.lastIndexOf("(") > 0) {
                    return term.substring(0, term.lastIndexOf("(")).trim();
                }
            }
        }
        return term;
    }


    private void getPreferredTermList() throws IOException {


        BufferedReader br = getReader(descriptions);
        br.readLine();

        Long conceptId;
        String line;
        String[] spl;
        HashMap<Long, ConceptTerm> hDescTerm = new HashMap<Long, ConceptTerm>();

        while ((line=br.readLine())!=null){
            spl=line.split("\t",-1);
            conceptId=Long.parseLong(spl[4]);
            if (hCptPref.containsKey(conceptId) && spl[2].equals("1") && spl[6].equals(SYNONYM_TYPE) ){

                ConceptTerm cptTerm=new ConceptTerm(conceptId,spl[7]);
                hDescTerm.put(Long.parseLong(spl[0]),cptTerm);
            }
        }
        br.close();
        br = getReader(language);
        br.readLine();
        Long descriptionId;
        while ((line=br.readLine())!=null){
            spl=line.split("\t",-1);
            descriptionId=Long.parseLong(spl[5]);
            if (hDescTerm.containsKey(descriptionId) && spl[2].equals("1") && spl[6].equals(PREFERRED_ACCEPTABILITY)){
                ConceptTerm cptTerm= hDescTerm.get(descriptionId);
                Preferreds prefs=hCptPref.get(cptTerm.getConceptId());
                if (prefs==null){
                    prefs=new Preferreds();
                }
                if (spl[4].equals(US_REFSETID)) {
                    prefs.setEnUS(cptTerm.getTerm());
                }else{
                    prefs.setEnGB(cptTerm.getTerm());
                }
                hCptPref.put(cptTerm.getConceptId(),prefs);
            }
        }
        br.close();
    }
    class Preferreds{

        public String getEnGB() {
            return enGB;
        }

        public void setEnGB(String enGB) {
            this.enGB = enGB;
        }

        public String getEnUS() {
            return enUS;
        }

        public void setEnUS(String enUS) {
            this.enUS = enUS;
        }

        String enGB;
        String enUS;
    }
    class ConceptTerm {
        Long conceptId;
        String term;

        public ConceptTerm(Long conceptId, String term) {
            this.conceptId = conceptId;
            this.term=term;
        }

        public Long getConceptId() {
            return conceptId;
        }

        public String getTerm() {
            return term;
        }
    }
    private HashMap<String, String> getDescendantDescriptions(TClosure tclos, String descriptions, String parentId) throws NumberFormatException, IOException {
        HashMap<String, String> ret=new HashMap<String,String>();

        HashSet<Long> descendants = tclos.getDescendants(Long.parseLong(parentId));
        BufferedReader br=getReader(descriptions);
        br.readLine();
        String line;
        String[] spl;

        while ((line=br.readLine())!=null){
            spl=line.split("\t",-1);
            Long sourceId=Long.parseLong( spl[4]);
            if (spl[2].equals("1") && descendants.contains(sourceId)){
                ret.put(spl[7].toLowerCase(),sourceId.toString());
            }
        }
        br.close();
        return ret;

    }

    public static BufferedWriter getWriter(String outFile) throws UnsupportedEncodingException, FileNotFoundException {

        FileOutputStream tfos = new FileOutputStream( outFile);
        OutputStreamWriter tfosw = new OutputStreamWriter(tfos,"UTF-8");
        return new BufferedWriter(tfosw);

    }
    public static BufferedReader getReader(String inFile) throws UnsupportedEncodingException, FileNotFoundException {

        FileInputStream rfis = new FileInputStream(inFile);
        InputStreamReader risr = new InputStreamReader(rfis,"UTF-8");
        BufferedReader rbr = new BufferedReader(risr);
        return rbr;

    }

    class ExtendedLineString implements Comparable {

        private final String boss;
        private String compareTermType;
        private String comparableData;

        public void setCompareTermType(String compareTermType) {
            this.compareTermType = compareTermType;
        }

        public String getComparableData() {
            if (compareTermType.equals("us")){
                return getPreferred(boss,"us");
            }
            if (compareTermType.equals("gb")){
                return getPreferred(boss,"gb");
            }
            return comparableData;
        }

        public String getLine() {
            return line;
        }

        String line;
        public ExtendedLineString( String line, String compareTermType){
            this.line=line;
            this.compareTermType=compareTermType;
            String []spl=line.split("\t",-1);
            if (spl.length>BOSS && spl[BOSS]!=null) {
                this.boss=spl[BOSS].trim();
                comparableData = getTerm(spl[BOSS]);
            }else {
                this.boss = "";
                comparableData = "";
            }
        }

        public int compareTo(Object o) {
            ExtendedLineString otherLine=(ExtendedLineString)o;
            return getComparableData().compareTo(otherLine.getComparableData());
        }
    }
}
