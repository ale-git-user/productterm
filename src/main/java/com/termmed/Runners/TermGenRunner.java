package com.termmed.Runners;

import com.termmed.Data.DataProvider;
import com.termmed.Data.I_dataProvider;
import com.termmed.terms.NewEsTermsGeneratorSDO;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;

public class TermGenRunner {

    public static void main (String[] args){


//        String topConcept=args[0];
//        String rels=args[1];
//        String descriptions=args[2];
//        String language=args[3];
//        String descriptionSource=args[4];
        String topConcept="373873005";
        String rels="C:\\Users\\carlo\\Downloads\\work\\xSnomedCT_InternationalRF2_PREPRODUCTION_20231201T120000Z\\Snapshot\\Terminology\\xsct2_Relationship_Snapshot_INT_20231201.txt";
        String concreteRels="C:\\Users\\carlo\\Downloads\\work\\xSnomedCT_InternationalRF2_PREPRODUCTION_20231201T120000Z\\Snapshot\\Terminology\\xsct2_RelationshipConcreteValues_Snapshot_INT_20231201.txt";
        String descriptions="C:\\Users\\carlo\\Downloads\\TSRP_ES_RELEASE_SNAPSHOT_20240331\\Snapshot\\Terminology\\sct2_Description_SpanishExtensionSnapshot-es_INT_20240331.txt";
//        String language="C:\\Users\\carlo\\Downloads\\TSRP_ES_RELEASE_SNAPSHOT_20220430_2\\Snapshot\\Resources\\der2_cRefset_LanguageSpanishExtensionSnapshotWithPrecomputedDefaults-es_INT_20220430.txt";
        String language="C:\\Users\\carlo\\Downloads\\TSRP_ES_RELEASE_SNAPSHOT_20240331\\Snapshot\\Refset\\Language\\der2_cRefset_LanguageSpanishExtensionSnapshot-es_INT_20240331.txt";
        String descriptionSource="C:\\Users\\carlo\\Downloads\\work\\xSnomedCT_InternationalRF2_PREPRODUCTION_20231201T120000Z\\Snapshot\\Terminology\\xsct2_Description_Snapshot-en_INT_20231201.txt";
        String langRefsetId="450828004";
        String newDescriptionModule="450829007";
        String newDescriptionEffectiveTime="20240331";
        String langCode="es";
        String linkedList=null;
//        String linkedList="C:\\Extensions\\Core\\lists\\FSN_changes_202107-202201.txt";
//        String linkedList="C:\\Extensions\\Spanish\\lists\\new2207.txt";
        String dateFilter="20230731";

        I_dataProvider dp = DataProvider.initFromHierarchy(topConcept, rels, concreteRels, descriptions,language,descriptionSource, dateFilter);
        NewEsTermsGeneratorSDO tg=new NewEsTermsGeneratorSDO(dp,langRefsetId);

        String outputFile="description_changes_20231201.txt";
        String substanceOutputFile="substances_20231201.txt";
        try {
            tg.execute(outputFile,substanceOutputFile, newDescriptionModule, newDescriptionEffectiveTime, langCode);

//            addColumn(outputFile,linkedList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        tg=null;
    }

    private static void addColumn(String outputFile, String linkedList) throws IOException {
        File output=new File(outputFile);
        File listFile=new File(linkedList);
        BufferedReader br=getReader(output);
        BufferedReader brl=getReader(listFile);

        brl.readLine();
        String[] spl;
        String line;

        HashMap<String,String> list=new HashMap<String,String>();
        while ((line=brl.readLine())!=null){
            spl=line.split("\t",-1);
//            list.put(spl[1],spl[0]);
            list.put(spl[0],"Yes");
        }
        brl.close();

        String header=br.readLine();
        BufferedWriter bw=getWriter(new File(output.getParent(),"columnAdded_" + output.getName()));

//        String columnHeader="In_Fsn_Change_list";
        String columnHeader="In_new2207_list";
        bw.append(header);
        bw.append("\t");
        bw.append(columnHeader);
        bw.append("\r\n");

        while((line=br.readLine())!=null){
            spl=line.split("\t",-1);
            bw.append(line);
            bw.append("\t");
            String worklist=list.get(spl[0]);
            if (worklist!=null){
                bw.append(worklist);
            }
            bw.append("\r\n");
        }
        bw.close();
        br.close();


    }

    public static BufferedWriter getWriter(File outFile) throws UnsupportedEncodingException, FileNotFoundException {

        FileOutputStream tfos = new FileOutputStream( outFile);
        OutputStreamWriter tfosw = new OutputStreamWriter(tfos,"UTF-8");
        return new BufferedWriter(tfosw);

    }


    private static BufferedReader getReader(File inFile) throws UnsupportedEncodingException, FileNotFoundException {

        FileInputStream rfis = new FileInputStream(inFile);
        InputStreamReader risr = new InputStreamReader(rfis,"UTF-8");
        BufferedReader rbr = new BufferedReader(risr);
        return rbr;

    }
}
