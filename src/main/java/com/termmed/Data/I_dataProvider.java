package com.termmed.Data;

import com.termmed.model.Concept;
import com.termmed.model.RF2Constants;

import java.util.HashSet;

public interface I_dataProvider extends RF2Constants{
    Concept getConcept(Long conceptId);

    HashSet<Long> getConceptToProcess();
}
