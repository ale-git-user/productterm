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
        String rels="/Users/ar/Downloads/SnomedCT_InternationalRF2_PRODUCTION_20190731T120000Z/Snapshot/Terminology/sct2_Relationship_Snapshot_INT_20190731.txt";
        String descriptions="/Users/ar/Downloads/Snapshot/Terminology/sct2_Description_SpanishExtensionSnapshot-es_INT_20191031.txt";
        String language="/Users/ar/Downloads/Snapshot/Resources/der2_cRefset_LanguageSpanishExtensionSnapshotWithPrecomputedDefaults-es_INT_20191031.txt";
        String descriptionSource="/Users/ar/Downloads/SnomedCT_InternationalRF2_PRODUCTION_20190731T120000Z/Snapshot/Terminology/sct2_Description_Snapshot-en_INT_20190731.txt";
        String langRefsetId="450828004";
        String newDescriptionModule="450829007";
        String newDescriptionEffectiveTime="20190808";
        String langCode="es";
        String linkedList="/Users/ar/Documents/Extensions/Spanish/worklists_201901.txt";
        String dateFilter="20190731";

        I_dataProvider dp = DataProvider.initFromHierarchy(topConcept, rels, descriptions,language,descriptionSource, dateFilter);
        NewEsTermsGeneratorSDO tg=new NewEsTermsGeneratorSDO(dp,langRefsetId);

        String outputFile="description_changes_201910.txt";
        try {
            tg.execute(outputFile, newDescriptionModule, newDescriptionEffectiveTime, langCode);

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
            list.put(spl[1],spl[0]);
        }
        brl.close();

        String header=br.readLine();
        BufferedWriter bw=getWriter(new File(output.getParent(),"columnAdded_" + output.getName()));

        String columnHeader="In_Fsn_Changes_list";
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
