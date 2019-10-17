package com.termmed.Data;

import com.termmed.model.Concept;
import com.termmed.model.Description;
import com.termmed.model.LangRefsetEntry;
import com.termmed.model.Relationship;
import com.termmed.util.TClosure;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ListModeledConceptsProvider implements I_dataProvider{

    private final List<String> modules;
    HashMap<Long, Long> descToCpt;
    HashMap<Long,Concept> concepts;
    HashSet<Long>conceptsToProcess;
    TClosure tClos;
    private HashMap<String,String[]> toGetFromSource;


    public ListModeledConceptsProvider(HashMap<Long,Concept>conceptList, String description, String language, String descriptionSource, List<String> modules) throws IOException {

        this.tClos=tClos;
        conceptsToProcess=new HashSet<Long>();
        concepts=conceptList;
        descToCpt=new HashMap<Long,Long>();
        toGetFromSource=new HashMap<String, String[]>();
        this.modules=modules;
        tClos=null;
        loadModeledRels();
        loadDescriptions(description);
        loadLanguage(language,descriptionSource);
//        loadFSNSource(descriptionSource);
        descToCpt=null;
        toGetFromSource=null;
    }

    private void loadModeledRels() {
        HashMap<Long,Concept> tempConcepts=new HashMap<>();
        for(Long code:concepts.keySet()){
            conceptsToProcess.add(code);
            Concept concept = concepts.get(code);
            List<Relationship> rels = concept.getRelationships();
            for (Relationship rel:rels){
                tempConcepts.put(Long.parseLong(rel.getType().getConceptId()),rel.getType());
                tempConcepts.put(Long.parseLong(rel.getTarget().getConceptId()),rel.getTarget());
            }
        }
        concepts.putAll(tempConcepts);
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
            if (spl[2].equals("1") && modules.indexOf(spl[3])>-1) {
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
            if (spl[2].equals("1") && modules.indexOf(spl[3])>-1) {

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

        if (spl[4].equals("782479008")){
            boolean bstop=true;
        }
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
                    Relationship rel=new Relationship(concept,typeConcept,dest,Integer.parseInt(spl[6]));
                    concept.addRelationship(rel);

                }
            }
        }
        br.close();
    }

    public Concept getConcept(Long conceptId) {
        return concepts.get(conceptId);
    }

    private void processChildren( Long parentId) throws IOException {
        HashSet<Long> children = tClos.getChildren(parentId);
        if (children==null){
            return;
        }
        for (Long child:children){
            if (!conceptsToProcess.contains(child)){
                conceptsToProcess.add(child);
                concepts.put(child, new Concept(child.toString()));
                processChildren(child);
            }

        }
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
