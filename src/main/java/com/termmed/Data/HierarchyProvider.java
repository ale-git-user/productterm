package com.termmed.Data;

import com.termmed.model.Concept;
import com.termmed.model.Description;
import com.termmed.model.LangRefsetEntry;
import com.termmed.model.Relationship;
import com.termmed.util.TClosure;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;

public class HierarchyProvider implements I_dataProvider{

    private final String releaseDateFilter;
    HashMap<Long, Long> descToCpt;
    HashMap<Long,Concept> concepts;
    HashSet<Long>conceptsToProcess;
    TClosure tClos;
    private HashMap<String,String[]> toGetFromSource;
    HashSet<Long> dateFilterIds;
    boolean nofilter;

    public HierarchyProvider(TClosure tClos, Long topConceptId, String rel, String concreteRels, String description, String language, String descriptionSource,String releaseDateFilter) throws IOException {

        this.tClos=tClos;
        conceptsToProcess=new HashSet<Long>();
        conceptsToProcess.add( topConceptId);
        concepts=new HashMap<Long,Concept>();
        concepts.put(topConceptId, new Concept(topConceptId.toString()));
        descToCpt=new HashMap<Long,Long>();
        toGetFromSource=new HashMap<String, String[]>();
        this.releaseDateFilter=releaseDateFilter;
        if (releaseDateFilter!=null){
            getConceptIdFilter(descriptionSource);
            nofilter=false;
        }else{
            dateFilterIds =new HashSet<Long>();
            nofilter=true;
        }
        processChildren ( topConceptId);

        tClos=null;
        loadRelationships(rel);
        loadConcreteRelationships(concreteRels);
        loadDescriptions(description);
        loadLanguage(language,descriptionSource);
        loadFSNSource(descriptionSource);
        descToCpt=null;
        toGetFromSource=null;
    }

    private void getConceptIdFilter(String descriptionSource) throws IOException {
        System.out.println("Getting ids from release date filter");
        BufferedReader br=getReader(descriptionSource);

        String[] spl;
        String line;
        br.readLine();
        dateFilterIds =new HashSet<Long>();
        while ((line=br.readLine())!=null){
            spl=line.split("\t",-1);
            if (spl[1].compareTo(releaseDateFilter)>0 && spl[2].equals("1") && spl[6].equals(FSN)) {
                Long cid = Long.parseLong(spl[4]);
                dateFilterIds.add(cid);
            }
        }
        br.close();
//        System.out.println("DataFilter size:" + dateFilterIds.size());
    }

    private void loadFSNSource(String descriptionSource) throws IOException {
        BufferedReader br=getReader(descriptionSource);

        String[] spl;
        String line;
        br.readLine();

        while ((line=br.readLine())!=null){
            spl=line.split("\t",-1);
            if (spl[2].equals("1") && spl[6].equals(FSN)) {
                Long cid = Long.parseLong(spl[4]);
                if (conceptsToProcess.contains(cid)){
                    Concept concept = concepts.get(cid);
                    if (concept != null) {
                        concept.setFsnSource(spl[7]);
                    }
                }
            }
        }
        br.close();
    }

    private void loadLanguage(String language, String descriptionSource) throws IOException{
        BufferedReader br=getReader(language);

        String[] spl;
        String line;
        br.readLine();

        while ((line=br.readLine())!=null){
            spl=line.split("\t",-1);
            if (spl[2].equals("1")) {
                Long cid = descToCpt.get(Long.parseLong(spl[5]));
                if (cid!=null) {
                    Concept concept = concepts.get(cid);
                    if (concept != null) {
                        LangRefsetEntry l = LangRefsetEntry.fromRf2(spl);
                        Description des = concept.getDescription(l.getReferencedComponentId());
                        des.addAcceptability(l);
                    }
                }else{
                    toGetFromSource.put(spl[5],spl);
                }
            }
        }
        br.close();

        br=getReader(descriptionSource);

        br.readLine();

        String[] langSpl;
        while ((line=br.readLine())!=null){
            spl=line.split("\t",-1);
            if ( toGetFromSource.containsKey(spl[0])){

                Description des=new Description();
                Description.fillFromRf2(des,spl);

                langSpl=toGetFromSource.get(spl[0]);
                LangRefsetEntry l = LangRefsetEntry.fromRf2(langSpl);
                des.addAcceptability(l);

                Long cid = Long.parseLong(spl[4]);
                Concept concept = concepts.get(cid);
                if (concept != null) {
                    concept.addDescription(des);
                }
            }

        }
        br.close();
    }

    private void loadDescriptions(String descriptions) throws IOException {
        BufferedReader br=getReader(descriptions);

        String[] spl;
        String line;
        br.readLine();

        while ((line=br.readLine())!=null){
            spl=line.split("\t",-1);
            if (spl[2].equals("1")) {

                Long cid = Long.parseLong(spl[4]);
                Concept concept = concepts.get(cid);
                if (concept != null) {
                    Description des=new Description();
                    Description.fillFromRf2(des,spl);
                    concept.addDescription(des);
                    descToCpt.put(Long.parseLong(des.getDescriptionId()),cid);
                }
            }
        }
        br.close();
    }

    private void loadRelationships(String rels) throws IOException {
        BufferedReader br=getReader(rels);

        String[] spl;
        String line;
        br.readLine();

        while ((line=br.readLine())!=null){
            spl=line.split("\t",-1);
            if (spl[2].equals("1") && !spl[7].equals(IS_A.getConceptId())) {

//        if (spl[4].equals("782479008")){
//            boolean bstop=true;
//        }
                Long cid = Long.parseLong(spl[4]);
                Concept concept = concepts.get(cid);
                if (concept != null) {
                    Long type=Long.parseLong(spl[7]);
                    Concept typeConcept;
                    if (!concepts.containsKey(type)){
                        typeConcept=new Concept(spl[7]);
                        concepts.put(type,new Concept(spl[7]));
                    }else{
                        typeConcept=concepts.get(type);
                    }
                    Long destination=Long.parseLong(spl[5]);
                    Concept dest;
                    if (!concepts.containsKey(destination)){
                        dest=new Concept(spl[5]);
                        concepts.put(destination,new Concept(spl[5]));
                    }else{
                        dest=concepts.get(destination);

                    }
                    Relationship rel=new Relationship(concept,typeConcept,dest, null,Integer.parseInt(spl[6]));
                    concept.addRelationship(rel);

                }
            }
        }
        br.close();
    }

    private void loadConcreteRelationships(String rels) throws IOException {
        BufferedReader br=getReader(rels);

        String[] spl;
        String line;
        br.readLine();

        String destination;
        while ((line=br.readLine())!=null){
            spl=line.split("\t",-1);
            if (spl[2].equals("1") && !spl[7].equals(IS_A.getConceptId())) {

//        if (spl[4].equals("782479008")){
//            boolean bstop=true;
//        }
                Long cid = Long.parseLong(spl[4]);
                Concept concept = concepts.get(cid);
                if (concept != null) {
                    Long type=Long.parseLong(spl[7]);
                    Concept typeConcept;
                    if (!concepts.containsKey(type)){
                        typeConcept=new Concept(spl[7]);
                        concepts.put(type,new Concept(spl[7]));
                    }else{
                        typeConcept=concepts.get(type);
                    };
                    if (spl[5].startsWith("#")){
                        destination=spl[5].substring(1);
                    }else if (spl[5].startsWith("\"")) {
                        destination = spl[5].substring(1,spl[5].length()-1);
                    }else{
                        destination=spl[5];
                    }
                    Relationship rel=new Relationship(concept,typeConcept,null, destination,Integer.parseInt(spl[6]));
                    concept.addRelationship(rel);

                }
            }
        }
        br.close();
    }
    public Concept getConcept(Long conceptId) {
        return concepts.get(conceptId);
    }

    private void processChildren( Long parentId ) throws IOException {
        HashSet<Long> children = tClos.getChildren(parentId);
        if (children==null){
            return;
        }
        for (Long child:children){
            if (!concepts.containsKey(child)){
                if (dateFilterIds.contains(child) || nofilter) {
                    conceptsToProcess.add(child);
                }
                concepts.put(child, new Concept(child.toString()));
                processChildren(child);
            }

        }
//        System.out.println("Concepts to review:" + conceptsToProcess.size());
    }

    private BufferedReader getReader(String inFile) throws UnsupportedEncodingException, FileNotFoundException {

        FileInputStream rfis = new FileInputStream(inFile);
        InputStreamReader risr = new InputStreamReader(rfis,"UTF-8");
        BufferedReader rbr = new BufferedReader(risr);
        return rbr;

    }

    public HashSet<Long> getConceptToProcess() {
        return conceptsToProcess;
    }

}
