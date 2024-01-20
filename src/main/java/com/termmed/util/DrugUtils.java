package com.termmed.util;


import com.termmed.Data.DataProvider;
import com.termmed.model.Concept;
import com.termmed.model.RF2Constants;
import com.termmed.model.Relationship;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class DrugUtils implements RF2Constants {
	
	public static final String CD = "(clinical drug)";
	public static final String MP = "(medicinal product)";
	public static final String MPF = "(medicinal product form)";
	public static final String PRODUCT = "(product)";
	
	static Map<String, Concept> numberConceptMap;
	static Map<String, Concept> doseFormConceptMap;
	static Map<String, Concept> unitOfPresentationConceptMap;
	static Map<String, Concept> unitOfMeasureConceptMap;
	
	public static void setConceptType(Concept c) {
		String semTag = SnomedUtils.deconstructFSN(c.getFsn())[1];
		switch (semTag) {
			case "(medicinal product form)" : c.setConceptType(ConceptType.MEDICINAL_PRODUCT_FORM);
												break;
			case "(product)" : c.setConceptType(ConceptType.PRODUCT);
								break;
			case "(medicinal product)" : c.setConceptType(ConceptType.MEDICINAL_PRODUCT);
										 break;
			case "(clinical drug)" : c.setConceptType(ConceptType.CLINICAL_DRUG);
										break;
			default : c.setConceptType(ConceptType.UNKNOWN);
		}
	}

//	public static Concept getNumberAsConcept(String number)  {
//		if (numberConceptMap == null) {
//			populateNumberConceptMap();
//		}
//		if (!numberConceptMap.containsKey(number)) {
//			System.out.println("Check and possibly need to create concept to represent number: " + number);
//		}
//		return numberConceptMap.get(number);
//	}
//
//	private static void populateNumberConceptMap() {
//		numberConceptMap = new HashMap<>();
//		Concept numberSubHierarchy = GraphLoader.getGraphLoader().getConcept("260299005", false, true); // |Number (qualifier value)|
//		for (Concept number : numberSubHierarchy.getDescendents(NOT_SET)) {
//			String numStr = SnomedUtils.deconstructFSN(number.getFsn())[0].trim();
//			try {
//				Double.parseDouble(numStr);
//				numberConceptMap.put(numStr, number);
//			} catch (Exception e) {}
//		}
//	}
	
	public static void registerNewNumber(String numberStr, Concept number) {
		numberConceptMap.put(numberStr, number);
	}
	
	public static double getConceptAsNumber(Concept number)  {
		String numStr = SnomedUtils.deconstructFSN(number.getFsn())[0];
		return Double.parseDouble(numStr);
	}
	
	public static String getConceptAsNumberStr(Concept number)  {
		String numStr = SnomedUtils.deconstructFSN(number.getFsn())[0];
		double d = Double.parseDouble(numStr);
		return toString(d);
	}

	
	
//	public static Concept findDoseForm(String fsn)  {
//		if (doseFormConceptMap == null) {
//			populateDoseFormConceptMap();
//		}
//		if (!doseFormConceptMap.containsKey(fsn)) {
//			System.out.println("Unable to identify dose form : " + fsn);
//		}
//		return doseFormConceptMap.get(fsn);
//	}
//
//
//	private static void populateDoseFormConceptMap()  {
//		doseFormConceptMap = new HashMap<>();
//		Concept doseFormSubHierarchy = GraphLoader.getGraphLoader().getConcept("736542009", false, true); // |Pharmaceutical dose form (dose form)|
//		for (Concept doseForm : doseFormSubHierarchy.getDescendents(NOT_SET)) {
//			doseFormConceptMap.put(doseForm.getFsn(), doseForm);
//		}
//	}


//	public static Concept findUnitOfPresentation(String fsn)  {
//		if (unitOfPresentationConceptMap == null) {
//			populateUnitOfPresentationConceptMap();
//		}
//		if (!unitOfPresentationConceptMap.containsKey(fsn)) {
//			System.out.println("Unable to identify unit of presentation : " + fsn);
//		}
//		return unitOfPresentationConceptMap.get(fsn);
//	}
//
//	private static void populateUnitOfPresentationConceptMap()  {
//		unitOfPresentationConceptMap = new HashMap<>();
//		Concept unitOfPresentationSubHierarchy = GraphLoader.getGraphLoader().getConcept("732935002", false, true); //|Unit of presentation (unit of presentation)|
//		for (Concept unitOfPresenation : unitOfPresentationSubHierarchy.getDescendents(NOT_SET)) {
//			unitOfPresentationConceptMap.put(unitOfPresenation.getFsn(), unitOfPresenation);
//		}
//	}
	
//	public static Concept findUnitOfMeasure(String unit)  {
//		//Try a straight lookup via the SCTID if we have one
//		try {
//			return GraphLoader.getGraphLoader().getConcept(unit);
//		} catch (Exception e) {}
//
//		if (unitOfMeasureConceptMap == null) {
//			populateUnitOfMeasureConceptMap();
//		}
//		if (!unitOfMeasureConceptMap.containsKey(unit)) {
//			System.out.println("Unable to identify unit of measure: '" + unit + "'");
//		}
//		return unitOfMeasureConceptMap.get(unit);
//	}
//
//	private static void populateUnitOfMeasureConceptMap()  {
//		unitOfMeasureConceptMap = new HashMap<>();
//		//UAT workaround
//		//Concept unitSubHierarchy = GraphLoader.getGraphLoader().getConcept("258666001", false, true); //  |Unit(qualifier value)|
//		Concept unitSubHierarchy = GraphLoader.getGraphLoader().getConcept("767524001", false, true); //  |Unit of measure (qualifier value)|
//		for (Concept unit : unitSubHierarchy.getDescendents(NOT_SET)) {
//			unitOfMeasureConceptMap.put(unit.getFsn(), unit);
//		}
//
//		//We can also measure just 'unit' of a substance
//		unitOfMeasureConceptMap.put(UNIT.getFsn(), UNIT);
//	}

	public static  String getDosageForm(Concept concept, boolean isFSN, String langRefset)  {
		List<Relationship> doseForms = concept.getRelationships(CharacteristicType.STATED_RELATIONSHIP, HAS_MANUFACTURED_DOSE_FORM, ActiveState.ACTIVE);
		if (doseForms.size() == 0) {
			return "NO STATED DOSE FORM DETECTED";
		} else if (doseForms.size() > 1) {
			return "MULTIPLE DOSE FORMS";
		} else {
			//Load full locally cached object since we may be working with some minimally defined thing
			Concept doseForm = DataProvider.get().getConcept(Long.parseLong(doseForms.get(0).getTarget().getConceptId()));
			String doseFormStr;

			try {
				if (isFSN) {

					doseFormStr = SnomedUtils.deconstructFSN(doseForm.getFsn())[0];
				} else {
					doseFormStr = doseForm.getPreferredSynonym(langRefset).getTerm();
				}
			} catch (NullPointerException ex) {
				System.out.println("2-Unable to retrieve translation for:\t" + doseForm.getConceptId() + "\t" + doseForm.getFsnSource());
				doseFormStr = "XXXERRORXXX";

			}
			return SnomedUtils.deCapitalize(doseFormStr);
		}
	}
	
	public static  String getAttributeType(Concept concept, Concept type, boolean isFSN, String langRefset) {
		List<Relationship> rels = concept.getRelationships(CharacteristicType.STATED_RELATIONSHIP, type, ActiveState.ACTIVE);
		Concept value;
		if (rels.size() == 0) {
			return "NO TYPE " + type.getFsn() + "DETECTED";
		} else if (rels.size() > 1) {
			//OK to return a value as long as they're all the same 
			value = rels.get(0).getTarget();
			for (Relationship rel : rels) {
				if (!rel.getTarget().equals(value)) {
					return "MULTIPLE DIFFERENT TYPE" + type.getFsn() + "DETECTED";
				}
			}
		} else {
			value = rels.get(0).getTarget();
		}

		//Load full locally cached object since we may be working with some minimally defined thing
		value = DataProvider.get().getConcept(Long.parseLong(value.getConceptId()));
		String valueStr;
		try {
			if (isFSN) {
				valueStr = SnomedUtils.deconstructFSN(value.getFsn())[0];
			} else {
				valueStr = value.getPreferredSynonym(langRefset).getTerm();
			}
		} catch (NullPointerException ex) {
			System.out.println("3-Unable to retrieve translation for:\t" + value.getConceptId() + "\t" + value.getFsnSource());
			valueStr = "XXXERRORXXX";

		}
		return SnomedUtils.deCapitalize(valueStr);
	}

	public static boolean isModificationOf(Concept specific, Concept general) {
		//Check if the specific concept has a modification attribute of the more general substance
		//and if there is a Modification Of attribute, can also call recursively
		List<Relationship> modifications = specific.getRelationships(CharacteristicType.INFERRED_RELATIONSHIP, IS_MODIFICATION_OF, ActiveState.ACTIVE);
		for (Relationship modification : modifications) {
			if (modification.getTarget().equals(general) || isModificationOf(specific, modification.getTarget())) {
				return true;
			}
		}
		return false;
	}
	

	public static Set<Concept> getIngredients(Concept c, CharacteristicType charType)  {
		Set<Concept> ingredients = new HashSet<>();
		for (Relationship r : c.getRelationships(charType, HAS_ACTIVE_INGRED, ActiveState.ACTIVE)) {
			ingredients.add(r.getTarget());
		}
		for (Relationship r : c.getRelationships(charType, HAS_PRECISE_INGRED, ActiveState.ACTIVE)) {
			ingredients.add(r.getTarget());
		}
		return ingredients;
	}

//	public static Ingredient getIngredientDetails(Concept c, int groupId, CharacteristicType charType)  {
//		//Populate as much data as can be found in this group.
//		Ingredient i = new Ingredient();
//		i.substance = SnomedUtils.getTarget(c, new Concept[] { HAS_ACTIVE_INGRED,  HAS_PRECISE_INGRED}, groupId, charType);
//
//		i.presStrength = SnomedUtils.getTarget(c, new Concept[] { HAS_PRES_STRENGTH_VALUE }, groupId, charType);
//		i.presNumeratorUnit = SnomedUtils.getTarget(c, new Concept[] { HAS_PRES_STRENGTH_UNIT }, groupId, charType);
//		i.presDenomQuantity = SnomedUtils.getTarget(c, new Concept[] {HAS_PRES_STRENGTH_DENOM_VALUE }, groupId, charType);
//		i.presDenomUnit = SnomedUtils.getTarget(c, new Concept[] {HAS_PRES_STRENGTH_DENOM_UNIT }, groupId, charType);
//
//		i.concStrength = SnomedUtils.getTarget(c, new Concept[] { HAS_CONC_STRENGTH_VALUE }, groupId, charType);
//		i.concNumeratorUnit = SnomedUtils.getTarget(c, new Concept[] { HAS_CONC_STRENGTH_UNIT }, groupId, charType);
//		i.concDenomQuantity = SnomedUtils.getTarget(c, new Concept[] {HAS_CONC_STRENGTH_DENOM_VALUE }, groupId, charType);
//		i.concDenomUnit = SnomedUtils.getTarget(c, new Concept[] {HAS_CONC_STRENGTH_DENOM_UNIT }, groupId, charType);
//
//		return i;
//	}
	
	public static String toString(double d)
	{
		d = new BigDecimal(d).setScale(6, RoundingMode.HALF_UP).doubleValue();
		if(d == (long) d)
			return String.format("%d",(long)d);
		else
			return String.format("%s",d);
	}

	public static Concept getBase(Concept c) {
		List<Concept> bases = c.getRelationships(CharacteristicType.STATED_RELATIONSHIP, IS_MODIFICATION_OF, ActiveState.ACTIVE)
				.stream()
				.map(rel -> rel.getTarget())
				.collect(Collectors.toList());
		if (bases.size() == 0) {
			return c;
		} else if (bases.size() > 1) {
			throw new IllegalArgumentException("Concept " + c + " has multiple modification attributes");
		} else if (bases.get(0).equals(c)) {
			throw new IllegalArgumentException("Concept " + c + " is a modification of itself.");
		} else  {
			//Call recursively to follow the transitive nature of modification
			return getBase(bases.get(0));
		}
	}
	
}
