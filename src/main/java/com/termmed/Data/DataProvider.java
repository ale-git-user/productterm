package com.termmed.Data;

import com.termmed.model.Concept;
import com.termmed.util.TClosure;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class DataProvider {


    private static I_dataProvider currentFile;

    /**
     * Gets the.
     *
     * @return the data provider
     * @throws Exception the exception
     */
    public static I_dataProvider get() {

//        if (currentFile==null){
//            throw new Exception("Current Data Provider was not initialized.");
//        }
        return currentFile;
    }

    public static I_dataProvider initFromHierarchy(String conceptId, String rels, String concreteRels, String descriptions, String language, String descriptionSource, String releaseDateFilter){
        try {
            TClosure tClos=new TClosure(rels);
            currentFile=new HierarchyProvider(tClos, Long.parseLong(conceptId), rels, concreteRels, descriptions, language, descriptionSource, releaseDateFilter);
            return currentFile;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static I_dataProvider initFromModeledConceptList(final HashMap<Long, Concept> conceptList, String descriptions, String language, String descriptionSource, List<String> modules){
        try {
            currentFile=new ListModeledConceptsProvider(conceptList, descriptions, language, descriptionSource, modules);
            return currentFile;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
