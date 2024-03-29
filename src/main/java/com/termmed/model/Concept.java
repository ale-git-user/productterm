package com.termmed.model;


import com.termmed.util.SnomedUtils;

import java.util.*;
import java.util.stream.Collectors;

public class Concept implements RF2Constants, Comparable<Concept>  {

	private String effectiveTime;
	private String moduleId;
	private boolean active = true;
	private String conceptId;
	private String fsn;
	private DefinitionStatus definitionStatus;
	private String preferredSynonym;
	private List<Description> descriptions = new ArrayList<Description>();
	private List<Relationship> relationships = new ArrayList<Relationship>();
	private boolean isLeafStated;
	private boolean isLeafInferred;
	private InactivationIndicator inactivationIndicator;
	
	private boolean isLoaded = false;
	private int originalFileLineNumber;
	private ConceptType conceptType;
	private List<String> assertionFailures = new ArrayList<String>();
	private String assignedAuthor;
	private String reviewer;
	boolean isModified = false; //indicates if has been modified in current processing run
	private String deletionEffectiveTime;
	private boolean isDeleted = false;
	private int depth = NOT_SET;
	private boolean isDirty = false;
	
	Collection<RelationshipGroup> statedRelationshipGroups;
	Collection<RelationshipGroup> inferredRelationshipGroups;

	private String fsnSource;
	private Description newFSNDescription;
	private Description newPreferredDescription;
	private Description newSyn;

	public String getReviewer() {
		return reviewer;
	}

	public void setReviewer(String reviewer) {
		this.reviewer = reviewer;
	}

	List<Concept> statedParents = new ArrayList<Concept>();
	List<Concept> inferredParents = new ArrayList<Concept>();
	List<Concept> statedChildren = new ArrayList<Concept>();
	List<Concept> inferredChildren = new ArrayList<Concept>();
	
	public Concept(String conceptId) {
		this.conceptId = conceptId;
		
		//default values
		this.definitionStatus = DefinitionStatus.PRIMITIVE;
	}
	
	public Concept(String conceptId, String fsn) {
		this(conceptId);
		this.fsn = fsn;
	}

	public Concept(String conceptId, int originalFileLineNumber) {
		this(conceptId);
		this.originalFileLineNumber = originalFileLineNumber;
	}
	
	public static Concept withDefaults (String conceptId) {
		Concept c = new Concept(conceptId);
		c.setModuleId(SCTID_CORE_MODULE);
		c.setActive(true);
		c.setDefinitionStatus(DefinitionStatus.PRIMITIVE);
		return c;
	}

	public String getEffectiveTime() {
		return effectiveTime;
	}

	public void setEffectiveTime(String effectiveTime) {
		this.effectiveTime = effectiveTime;
	}

	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		if (this.moduleId != null && !this.moduleId.equals(moduleId)) {
			setDirty();
			this.effectiveTime = null;
		}
		this.moduleId = moduleId;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean newActiveState) {
		this.active = newActiveState;
	}

	public String getConceptId() {
		return conceptId;
	}

	public void setConceptId(String conceptId) {
		this.conceptId = conceptId;
	}

	public String getFsn() {
		return fsn;
	}

	public void setFsn(String fsn) {
		this.fsn = fsn;
	}

	public DefinitionStatus getDefinitionStatus() {
		return definitionStatus;
	}

	public void setDefinitionStatus(DefinitionStatus definitionStatus) {
		this.definitionStatus = definitionStatus;
	}

	public String getPreferredSynonym() {
		if (preferredSynonym == null) {
			return getPreferredSynonym(SP_LANG_REFSET).getTerm();
		}
		return preferredSynonym;
	}
	
	public Description getPreferredSynonym(String refsetId) {
		List<Description> pts = getDescriptions(refsetId, Acceptability.PREFERRED, DescriptionType.SYNONYM, ActiveState.ACTIVE);
		return pts.size() == 0 ? null : pts.get(0);
	}

	public void setPreferredSynonym(String preferredSynonym) {
		this.preferredSynonym = preferredSynonym;
	}

	public List<Description> getDescriptions() {
		return descriptions;
	}

	public void setDescriptions(List<Description> descriptions) {
		this.descriptions = descriptions;
	}

	public List<Relationship> getRelationships() {
		return relationships;
	}
	
	public List<Relationship> getRelationships(CharacteristicType characteristicType, ActiveState state, String effectiveTime) {
		List<Relationship> matches = new ArrayList<Relationship>();
		for (Relationship r : relationships) {
			if (effectiveTime == null || r.getEffectiveTime().equals(effectiveTime)) {
				if (characteristicType.equals(CharacteristicType.ALL) || r.getCharacteristicType().equals(characteristicType)) {
					if (state.equals(ActiveState.BOTH) || (state.equals(ActiveState.ACTIVE) && r.isActive()) ||
							(state.equals(ActiveState.INACTIVE) && !r.isActive())) {
						matches.add(r);
					}
				}
			}
		}
		return matches;
	}
	
	public List<Relationship> getRelationships(CharacteristicType characteristicType, ActiveState activeState) {
		return getRelationships(characteristicType, activeState, null);
	}
	
	//Gets relationships that match the triple + group
	public List<Relationship> getRelationships(Relationship r) {
		if (r.getTarget() != null) {
			return getRelationships(r.getCharacteristicType(), r.getType(), r.getTarget(), r.getGroupId(), ActiveState.ACTIVE);
		}else{
			return getRelationships(r.getCharacteristicType(), r.getType(), r.getValue(), r.getGroupId(), ActiveState.ACTIVE);
		}
	}
	
	public List<Relationship> getRelationships(Relationship r, ActiveState activeState) {
		return getRelationships(r.getCharacteristicType(), r.getType(), r.getTarget(), r.getGroupId(), activeState);
	}
	
	public List<Relationship> getRelationships(CharacteristicType characteristicType, Concept type, ActiveState activeState) {
		List<Relationship> potentialMatches = getRelationships(characteristicType, activeState);
		List<Relationship> matches = new ArrayList<Relationship>();
		for (Relationship r : potentialMatches) {
			if (r.getType().equals(type)) {
				matches.add(r);
			}
		}
		return matches;
	}

	public List<Relationship> getRelationships(CharacteristicType charType, Concept[] targets, ActiveState state) {
		List<Relationship> matchingRels = new ArrayList<>();
		for (Concept target : targets) {
			matchingRels.addAll(getRelationships(charType, target, state));
		}
		return matchingRels;
	}

	public List<Relationship> getRelationships(CharacteristicType characteristicType, Concept type, Concept target, int groupId, ActiveState activeState) {
		List<Relationship> potentialMatches = getRelationships(characteristicType, type, target, activeState);
		List<Relationship> matches = new ArrayList<Relationship>();
		for (Relationship r : potentialMatches) {
			if (r.getGroupId() == groupId) {
				matches.add(r);
			}
		}
		return matches;
	}
	
	public List<Relationship> getRelationships(CharacteristicType characteristicType, Concept type, Concept target, ActiveState activeState) {
		List<Relationship> potentialMatches = getRelationships(characteristicType, type, activeState);
		List<Relationship> matches = new ArrayList<Relationship>();
		for (Relationship r : potentialMatches) {
			if (r.getTarget()!=null) {
				if (r.getTarget().equals(target)) {
					matches.add(r);
				}
			}
		}
		return matches;
	}
	public List<Relationship> getRelationships(CharacteristicType characteristicType, Concept type, String target, int groupId, ActiveState activeState) {
		List<Relationship> potentialMatches = getRelationships(characteristicType, type, target, activeState);
		List<Relationship> matches = new ArrayList<Relationship>();
		for (Relationship r : potentialMatches) {
			if (r.getGroupId() == groupId) {
				matches.add(r);
			}
		}
		return matches;
	}

	public List<Relationship> getRelationships(CharacteristicType characteristicType, Concept type, String target, ActiveState activeState) {
		List<Relationship> potentialMatches = getRelationships(characteristicType, type, activeState);
		List<Relationship> matches = new ArrayList<Relationship>();
		for (Relationship r : potentialMatches) {
			if (r.getValue()!=null) {
				if (r.getValue().equals(target)) {
					matches.add(r);
				}
			}
		}
		return matches;
	}
	public List<Relationship> getRelationships(CharacteristicType characteristicType, Concept type, int groupId) {
		List<Relationship> potentialMatches = getRelationships(characteristicType, type, ActiveState.ACTIVE);
		List<Relationship> matches = new ArrayList<Relationship>();
		for (Relationship r : potentialMatches) {
			if (r.getGroupId() == groupId) {
				matches.add(r);
			}
		}
		return matches;
	}

	public Relationship getRelationship(String id) {
		for (Relationship r : relationships) {
			if (r.getRelationshipId().equals(id)) {
				return r;
			}
		}
		return null;
	}

	public void setRelationships(List<Relationship> relationships) {
		this.relationships = relationships;
	}
	
	public void removeRelationship(Relationship r) {
		if (r.getEffectiveTime() != null) {
			throw new IllegalArgumentException("Attempt to deleted published relationship " + r);
		}
		this.relationships.remove(r);
	}

	public boolean isIsLeafStated() {
		return isLeafStated;
	}

	public void setIsLeafStated(boolean isLeafStated) {
		this.isLeafStated = isLeafStated;
	}

	public boolean isIsLeafInferred() {
		return isLeafInferred;
	}

	public void setIsLeafInferred(boolean isLeafInferred) {
		this.isLeafInferred = isLeafInferred;
	}
	
	public boolean isLeaf (CharacteristicType c) {
		if (c.equals(CharacteristicType.STATED_RELATIONSHIP)) {
			return statedChildren.size() == 0;
		} else {
			return inferredChildren.size() == 0;
		}
	}

	@Override
	public String toString() {
		return conceptId + " |" + this.fsn + "|";
	}

	public String toExpression(CharacteristicType charType) {
		String expression = getParents(charType).stream().map(p -> p.toString())
							.collect(Collectors.joining (" + \n"));
		expression += " : \n";
		//Add any ungrouped attributes
		boolean isFirstGroup = true;
		for (RelationshipGroup group : getRelationshipGroups (charType)) {
			if (isFirstGroup) {
				isFirstGroup = false;
			} else {
				expression += ",\n  ";
			}
			expression += group.isGrouped() ? "{" : "";
			expression += group.getRelationships().stream().map(p -> p.toString())
					.collect(Collectors.joining (",\n  "));
			expression += group.isGrouped() ? "}" : "";
		}
		return expression;
	}

	@Override
	public int hashCode() {
		if (conceptId != null)
			return conceptId.hashCode();
		
		//Where a conceptId does not exist, hash the FSN
		if (fsn !=null && !fsn.trim().isEmpty()) {
			return fsn.hashCode();
		}
		
		//Where we don't have either, hash the expression
		return toExpression(CharacteristicType.STATED_RELATIONSHIP).hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Concept)) {
			return false;
		}
		Concept rhs = ((Concept) other);
		//If both concepts have Ids, compare those
		if (this.conceptId != null && rhs.conceptId != null) {
			return (this.conceptId.compareTo(rhs.conceptId) == 0);
		}
		
		//Otherwise, compare FSNs or expressions
		if (this.fsn != null && !this.fsn.isEmpty() && rhs.fsn != null && !rhs.fsn.isEmpty()) {
			return (this.fsn.equals(rhs.fsn));
		}
		String thisExpression = this.toExpression(CharacteristicType.STATED_RELATIONSHIP);
		String rhsExpression = rhs.toExpression(CharacteristicType.STATED_RELATIONSHIP);
		return thisExpression.equals(rhsExpression);
	}

	public void addRelationship(Concept type, Concept target) {
		Relationship r = new Relationship();
		r.setActive(true);
		r.setGroupId(0);
		r.setCharacteristicType(CharacteristicType.STATED_RELATIONSHIP);
		r.setSourceId(this.getConceptId());
		r.setType(type);
		r.setTarget(target);
		r.setModifier(Modifier.EXISTENTIAL);
		relationships.add(r);
		//Reset our cache of relationship groups
		statedRelationshipGroups = null;
	}

	public void addRelationship(Concept type, String target) {
		Relationship r = new Relationship();
		r.setActive(true);
		r.setGroupId(0);
		r.setCharacteristicType(CharacteristicType.STATED_RELATIONSHIP);
		r.setSourceId(this.getConceptId());
		r.setType(type);
		r.setValue(target);
		r.setModifier(Modifier.EXISTENTIAL);
		relationships.add(r);
		//Reset our cache of relationship groups
		statedRelationshipGroups = null;
	}
	public boolean isLoaded() {
		return isLoaded;
	}

	public void setLoaded(boolean isLoaded) {
		this.isLoaded = isLoaded;
	}

	public int getOriginalFileLineNumber() {
		return originalFileLineNumber;
	}
	
	public void addRelationship(Relationship r) {
		addRelationship(r, false);
	}
	
	public void addRelationship(Relationship r, boolean replaceTripleMatch) {
		//Do we already had a relationship with this id?  Replace if so.
		//Actually since delta files from the TS could have different SCTIDs
		//Null out the ID temporarily to force a triple + groupId comparison
		String id = r.getRelationshipId();
		if (replaceTripleMatch) {
			r.setRelationshipId(null);
		}
		if (relationships.contains(r)) {
			//Might match more than one if we have historical overlapping triples
			
			//Special case were we receive conflicting rows for the same triple in a delta.
			//keep the active row in that case.
			if (replaceTripleMatch && r.getEffectiveTime() == null && !r.isActive()) {
				for (Relationship match : getRelationships(r)) {
					if (match.isActive() && match.getEffectiveTime() == null) {
						System.out.println ("Ignoring inactivation in " + this + " between already received active " + match + " and incoming inactive " + r);
						return;
					}
				}
			}
			
			relationships.removeAll(Collections.singleton(r));
		}
		r.setRelationshipId(id);
		relationships.add(r);
		
		//Reset our cache of relationship groups, recalculate next time it's requested
		if (r.getCharacteristicType().equals(CharacteristicType.STATED_RELATIONSHIP)) {
			statedRelationshipGroups = null;
		} else {
			inferredRelationshipGroups = null;
		}
	}
	
	public void addChild(CharacteristicType charType, Concept c) {
		getChildren(charType).add(c);
	}
	
	public void removeChild(CharacteristicType charType, Concept c) {
		getChildren(charType).remove(c);
	}
	
	public void addParent(CharacteristicType charType, Concept p) {
		getParents(charType).add(p);
	}
	
	public void removeParent(CharacteristicType charType, Concept p) {
		getParents(charType).remove(p);
	}

	public ConceptType getConceptType() {
		return conceptType;
	}

	public void setConceptType(ConceptType conceptType) {
		this.conceptType = conceptType;
	}
	
	public void setConceptType(String conceptTypeStr) {
		if (conceptTypeStr.contains("Strength")) {
			this.setConceptType(ConceptType.PRODUCT_STRENGTH);
		} else if (conceptTypeStr.contains("Entity")) {
			this.setConceptType(ConceptType.MEDICINAL_ENTITY);
		} else if (conceptTypeStr.contains("Form")) {
			this.setConceptType(ConceptType.MEDICINAL_PRODUCT_FORM);
		} else if (conceptTypeStr.contains("Grouper")) {
			this.setConceptType(ConceptType.GROUPER);
		} else {
			this.setConceptType(ConceptType.UNKNOWN);
		}
	}
	
	public Set<Concept> getDescendents(int depth)  {
		return getDescendents(depth, CharacteristicType.INFERRED_RELATIONSHIP, ActiveState.ACTIVE);
	}
	
	public Set<Concept> getDescendents(int depth, CharacteristicType characteristicType, ActiveState activeState)  {
		return getDescendents(depth, characteristicType, activeState, false);
	}
	
	public Set<Concept> getDescendents(int depth, CharacteristicType characteristicType, ActiveState activeState, boolean includeSelf)  {
		//Inactive children actually make no sense.  They wouldn't have relationships to be in the 
		//hierarchy in the first place?!
		Set<Concept> allDescendents = new HashSet<Concept>();
		this.populateAllDescendents(allDescendents, depth, characteristicType, activeState);
		if (includeSelf) {
			allDescendents.add(this);
		}
		return allDescendents;
	}
	
	private void populateAllDescendents(Set<Concept> descendents, int depth, CharacteristicType characteristicType, ActiveState activeState)  {
		for (Concept thisChild : getChildren(characteristicType)) {
			if (activeState.equals(ActiveState.BOTH) || thisChild.active == SnomedUtils.translateActive(activeState)) {
				descendents.add(thisChild);
				if (depth == NOT_SET || depth > 1) {
					int newDepth = depth == NOT_SET ? NOT_SET : depth - 1;
					thisChild.populateAllDescendents(descendents, newDepth, characteristicType, activeState);
				}
			}
		}
	}
	
	public Set<Concept> getAncestors(int depth)  {
		return getAncestors(depth, CharacteristicType.INFERRED_RELATIONSHIP, ActiveState.ACTIVE, false);
	}
	
	public Set<Concept> getAncestors(int depth, CharacteristicType characteristicType, ActiveState activeState, boolean includeSelf)  {
		Set<Concept> allAncestors = new HashSet<Concept>();
		this.populateAllAncestors(allAncestors, depth, characteristicType, activeState);
		if (includeSelf) {
			allAncestors.add(this);
		}
		return allAncestors;
	}
	
	private void populateAllAncestors(Set<Concept> ancestors, int depth, CharacteristicType characteristicType, ActiveState activeState)  {
		for (Concept thisParent : getParents(characteristicType)) {
			if (activeState.equals(ActiveState.BOTH) || thisParent.active == SnomedUtils.translateActive(activeState)) {
				ancestors.add(thisParent);
				if (depth == NOT_SET || depth > 1) {
					int newDepth = depth == NOT_SET ? NOT_SET : depth - 1;
					thisParent.populateAllAncestors(ancestors, newDepth, characteristicType, activeState);
				}
			}
		}
	}
	
	public List<Concept> getChildren(CharacteristicType characteristicType) {
		switch (characteristicType) {
			case STATED_RELATIONSHIP : return statedChildren;
			case INFERRED_RELATIONSHIP : return inferredChildren;
			default:
		}
		return null;
	}

	//A preferred description can be preferred in either dialect, but if we're looking for an acceptable one, 
	//then it must not also be preferred in the other dialect
	public List<Description> getDescriptions(Acceptability acceptability, DescriptionType descriptionType, ActiveState activeState)  {
		List<Description> matchingDescriptions = new ArrayList<Description>();
		for (Description thisDescription : getDescriptions(activeState)) {
			//Is this description of the right type?
			if ( descriptionType == null || thisDescription.getType().equals(descriptionType)) {
				//Are we working with JSON representation and acceptability map, or an RF2 representation
				//with language refset entries?
				if (thisDescription.getAcceptabilityMap() != null) {
					if ( acceptability.equals(Acceptability.BOTH) || thisDescription.getAcceptabilityMap().containsValue(acceptability)) {
						if (acceptability.equals(Acceptability.BOTH)) {
							matchingDescriptions.add(thisDescription);
						} else if (acceptability.equals(Acceptability.PREFERRED) || !thisDescription.getAcceptabilityMap().containsValue(Acceptability.PREFERRED)) {
							matchingDescriptions.add(thisDescription);
						}
					}
				} else if (!thisDescription.getLangRefsetEntries().isEmpty()) {
					boolean match = false;
					boolean preferredFound = false;
					for (LangRefsetEntry l : thisDescription.getLangRefsetEntries(ActiveState.ACTIVE)) {
						if (acceptability.equals(Acceptability.BOTH) || 
							acceptability.equals(SnomedUtils.translateAcceptability(l.getAcceptabilityId()))) {
							match = true;
						} 
						
						if (l.getAcceptabilityId().equals(SCTID_PREFERRED_TERM)) {
							preferredFound = true;
						}
					}
					//Did we find one, and if it's acceptable, did we also not find another preferred
					if (match) {
						if (acceptability.equals(Acceptability.ACCEPTABLE)) {
							if (!preferredFound) {
								matchingDescriptions.add(thisDescription);
							}
						} else {
							matchingDescriptions.add(thisDescription);
						}
					}
				} else {
					System.out.println(thisDescription + " is active with no Acceptability map or Language Refset entries (since " + thisDescription.getEffectiveTime() + ").");
				}
			}
		}
		return matchingDescriptions;
	}
	
	public List<Description> getDescriptions(String langRefsetId, Acceptability targetAcceptability, DescriptionType descriptionType, ActiveState active)  {
		//Get the matching terms, and then pick the ones that have the appropriate Acceptability for the specified Refset
		List<Description> matchingDescriptions = new ArrayList<Description>();
		for (Description d : getDescriptions(targetAcceptability, descriptionType, active)) {
			//We might have this acceptability either from a Map (JSON) or Langrefset entry (RF2)
			Acceptability acceptability = d.getAcceptability(langRefsetId);
			if (targetAcceptability == Acceptability.BOTH || (acceptability!= null && acceptability.equals(targetAcceptability))) {
				//Need to check the Acceptability because the first function might match on some other language
				matchingDescriptions.add(d);
			}
		}
		return matchingDescriptions;
	}
	
	public List<Description> getDescriptions(ActiveState a) {
		List<Description> results = new ArrayList<Description>();
		for (Description d : descriptions) {
			if (SnomedUtils.descriptionHasActiveState(d, a)) {
					results.add(d);
			}
		}
		return results;
	}
	

	public Description getDescription(String descriptionId) {
		for (Description d : descriptions) {
			if (d.getDescriptionId().equals(descriptionId)) {
				return d;
			}
		}
		return null;
	}
	
	public void addDescription(Description d) {
		addDescription(d, false); //Don't allow duplicates by default
	}
	
	public void addDescription(Description d, boolean allowDuplicateTerms) {
		//Do we already have a description with this SCTID?
		if (!allowDuplicateTerms && descriptions.contains(d)) {
			descriptions.remove(d);
		}
		
		descriptions.add(d);
		if (d.isActive() && d.getType().equals(DescriptionType.FSN)) {
			this.setFsn(d.getTerm());
		}
	}
	
	public void removeDescription (Description d) {
		descriptions.remove(d);
	}

	public List<Concept> getParents(CharacteristicType characteristicType) {
		//Concepts loaded from TS would not get these arrays populated.  Populate.
		List<Concept> parents = null;
		switch (characteristicType) {
			case STATED_RELATIONSHIP : parents = statedParents;
										break;
			case INFERRED_RELATIONSHIP: parents = inferredParents;
										break;
			default: throw new IllegalArgumentException("Cannot have " + characteristicType + " parents.");
		}
		
		if (parents == null || parents.size() == 0) {
			if (parents == null) {
				parents = new ArrayList<>();
				if (characteristicType.equals(CharacteristicType.STATED_RELATIONSHIP)) {
					statedParents = parents;
				} else {
					inferredParents = parents;
				}
			}
			populateParents(parents, characteristicType);
		}
		return parents;
	}
	
	private void populateParents(List<Concept> parents, CharacteristicType characteristicType) {
		parents.clear();
		for (Relationship parentRel : getRelationships(characteristicType, IS_A, ActiveState.ACTIVE)) {
			parents.add(parentRel.getTarget());
		}
	}

	public List<String>getAssertionFailures() {
		return assertionFailures;
	}
	
	public void addAssertionFailure(String failure) {
		assertionFailures.add(failure);
	}

	public String getAssignedAuthor() {
		return assignedAuthor;
	}

	public void setAssignedAuthor(String assignedAuthor) {
		this.assignedAuthor = assignedAuthor;
	}

	public Description getFSNDescription() {
		if (descriptions == null) {
			String err = "Concept " + conceptId + " |" + getFsn() + "| has no descriptions";
			throw new IllegalArgumentException(err);
		}
		for (Description d : descriptions) {
			if (d.isActive() && d.getType().equals(DescriptionType.FSN)) {
				return d;
			}
		}
		return null;
	}
	
	public List<Description> getSynonyms(Acceptability Acceptability) {
		List<Description> synonyms = new ArrayList<Description>();
		for (Description d : descriptions) {
			if (d.isActive() && d.getAcceptabilityMap().values().contains(Acceptability) && d.getType().equals(DescriptionType.SYNONYM)) {
				synonyms.add(d);
			}
		}
		return synonyms;
	}

	public boolean hasTerm(String term, String langCode) {
		boolean hasTerm = false;
		for (Description d : descriptions) {
			if (d.getTerm().equals(term) && d.getLang().equals(langCode)) {
				hasTerm = true;
				break;
			}
		}
		return hasTerm;
	}
	
	public Description findTerm(String term) {
		return findTerm (term, null, false, true);
	}
	
	public Description findTerm(String term , String lang) {
		return findTerm (term, lang, false, true);
	}

	public Description findTerm(String term , String lang, boolean caseInsensitive, boolean includeInactive) {
		//First look for a match in the active terms, then try inactive
		for (Description d : getDescriptions(ActiveState.ACTIVE)) {
			if ((lang == null || lang.equals(d.getLang()))) {
				if (caseInsensitive) {
					term = term.toLowerCase();
					String desc = d.getTerm().toLowerCase();
					if (term.equals(desc)) {
						return d;
					}
				} else if (d.getTerm().equals(term)) {
						return d;
				}
			}
		}
		
		if (includeInactive) {
			for (Description d : getDescriptions(ActiveState.INACTIVE)) {
				if (d.getTerm().equals(term) && 
						(lang == null || lang.equals(d.getLang()))) {
					return d;
				}
			}
		}
		return null;
	}

	public void setModified() {
		isModified = true;
	}
	
	public boolean isModified() {
		return isModified;
	}
	
	public int getDepth() {
		return depth;
	}
	
	public void setDepth(int depth) {
		// We'll maintain the shortest possible path, so don't allow depth to increase
		if (this.depth == NOT_SET || depth < this.depth) {
			this.depth = depth;
		}
	}

	@Override
	public int compareTo(Concept c) {
		return getConceptId().compareTo(c.getConceptId());
	}

	public boolean isDeleted() {
		return isDeleted;
	}

	public void delete(String deletionEffectiveTime) {
		this.isDeleted = true;
		this.deletionEffectiveTime = deletionEffectiveTime;
	}

	public InactivationIndicator getInactivationIndicator() {
		return inactivationIndicator;
	}

	public void setInactivationIndicator(InactivationIndicator inactivationIndicator) {
		this.inactivationIndicator = inactivationIndicator;
	}

	
	public static Concept fillFromRf2(Concept c, String[] lineItems) {
		c.setActive(lineItems[CON_IDX_ACTIVE].equals("1"));
		c.setEffectiveTime(lineItems[CON_IDX_EFFECTIVETIME]);
		c.setModuleId(lineItems[CON_IDX_MODULID]);
		c.setDefinitionStatus(SnomedUtils.translateDefnStatus(lineItems[CON_IDX_DEFINITIONSTATUSID]));
		return c;
	}

	public List<Concept> getSiblings(CharacteristicType cType) {
		List<Concept> siblings = new ArrayList<Concept>();
		//Get all the immediate children of the immediate parents
		for (Concept thisParent : getParents(cType)) {
			siblings.addAll(thisParent.getChildren(cType));
		}
		return siblings;
	}

	public String getId() {
		return conceptId;
	}
	
	public void setId(String id) {
		conceptId = id;
	}

	public boolean isDirty() {
		return isDirty;
	}

	public void setDirty() {
		this.isDirty = true;
	}

	
	public Concept clone() {
		return clone(null, false);
	}
	
	public Concept cloneWithIds() {
		return clone(null, true);
	}
	
	public Concept clone(String sctid) {
		return clone(sctid, false);
	}
	
	private Concept clone(String sctid, boolean keepIds) {
		Concept clone = new Concept(keepIds?conceptId:sctid, getFsn());
		clone.setEffectiveTime(keepIds?effectiveTime:null);
		clone.setActive(active);
		clone.setDefinitionStatus(getDefinitionStatus());
		clone.setModuleId(getModuleId());
		clone.setConceptType(conceptType);
		
		//Copy all descriptions
		for (Description d : getDescriptions(ActiveState.ACTIVE)) {
			//We need to null out the conceptId since the clone is a new concept
			Description dClone = d.clone(keepIds?d.getDescriptionId():null);
			dClone.setConceptId(keepIds?conceptId:null);
			dClone.setEffectiveTime(keepIds?d.getEffectiveTime():null);
			clone.addDescription(dClone);

		}
		
		//Copy all stated relationships, or in the case of an exact clone (keepIds = true) also inferred
		List<Relationship> selectedRelationships = keepIds ? relationships : getRelationships(CharacteristicType.STATED_RELATIONSHIP, ActiveState.ACTIVE);
		for (Relationship r : selectedRelationships) {
			//We need to null out the sourceId since the clone is a new concept
			Relationship rClone = r.clone(keepIds?r.getRelationshipId():null);
			rClone.setEffectiveTime(keepIds?r.getEffectiveTime():null);
			rClone.setSourceId(null);
			clone.addRelationship(rClone);
		}
		
		//Copy Parent/Child arrays
		clone.inferredChildren = inferredChildren == null? new ArrayList<>() : new ArrayList<>(inferredChildren);
		clone.statedChildren = statedChildren == null? new ArrayList<>() : new ArrayList<>(statedChildren);
		clone.inferredParents = inferredParents == null? new ArrayList<>() : new ArrayList<>(inferredParents);
		clone.statedParents = statedParents == null? new ArrayList<>() : new ArrayList<>(statedParents);
		

		return clone;
	}

	public ComponentType getComponentType() {
		return ComponentType.CONCEPT;
	}
	
	public RelationshipGroup getRelationshipGroup(CharacteristicType characteristicType, long groupId ) {
		for (RelationshipGroup g : getRelationshipGroups(characteristicType)) {
			if (g.getGroupId() == groupId) {
				return g;
			}
		}
		return null;
	}
	
	public Collection<RelationshipGroup> getRelationshipGroups(CharacteristicType characteristicType) {
		//Include group 0 by default
		return getRelationshipGroups(characteristicType, ActiveState.ACTIVE, true);
	}
	
	/**
	 * Relationship groups will not include IS A relationships
	 */
	public Collection<RelationshipGroup> getRelationshipGroups(CharacteristicType characteristicType, ActiveState activeState, boolean includeGroup0) {
		Collection<RelationshipGroup> relationshipGroups = characteristicType.equals(CharacteristicType.STATED_RELATIONSHIP) ? statedRelationshipGroups : inferredRelationshipGroups;
		if (relationshipGroups == null) {
			Map<Integer, RelationshipGroup> groups = new HashMap<>();
			for (Relationship r : getRelationships(characteristicType, activeState)) {
				if (r.getType().equals(IS_A) || (!includeGroup0 && r.getGroupId() == 0)) {
					continue;
				}
				//Do we know about this Relationship Group yet?
				RelationshipGroup group = groups.get(r.getGroupId());
				if (group == null) {
					group = new RelationshipGroup(r.getGroupId() , r);
					groups.put(r.getGroupId(), group);
				} else {
					group.getRelationships().add(r);
				}
			}
			relationshipGroups = groups.values();
			if (characteristicType.equals(CharacteristicType.STATED_RELATIONSHIP)) {
				statedRelationshipGroups = relationshipGroups;
			} else {
				inferredRelationshipGroups = relationshipGroups;
			}
		}
		return relationshipGroups;
	}

	public void addRelationshipGroup(RelationshipGroup group) {
		for (Relationship r : group.getRelationships()) {
			addRelationship(r);
		}
		//Force recalculation
		statedRelationshipGroups = null;
		inferredRelationshipGroups = null;
	}

	public String getFsnSource() {
		return fsnSource;
	}

    public void setFsnSource(String fsnSource) {
        this.fsnSource = fsnSource;
    }

	public Description getNewFSNDescription() {
		return newFSNDescription;
	}

	public void setNewFSNDescription(Description replacement) {
		this.newFSNDescription=replacement;
	}

	public void setNewPreferredDescription(Description newPreferredDescription) {
		this.newPreferredDescription = newPreferredDescription;
	}

	public Description getNewPreferredDescription() {
		return newPreferredDescription;
	}

	public void setNewSyn(Description newSyn) {
		this.newSyn = newSyn;
	}

	public Description getNewSyn() {
		return newSyn;
	}
}
