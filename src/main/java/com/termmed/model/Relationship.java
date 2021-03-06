
package com.termmed.model;

public class Relationship implements RF2Constants, Comparable<Relationship> {

	private String effectiveTime;
	private String moduleId;
	private Boolean active = null;
	private String relationshipId;
	private Concept type;
	private Concept target;
	private String sourceId;
	private int groupId;
	private CharacteristicType characteristicType;
	private Modifier modifier;
	private Boolean released;

	private Concept source;
	
	private boolean dirty = false;
	
	private boolean isDeleted = false;
	private String deletionEffectiveTime;
	
	public static final String[] rf2Header = new String[] {"id","effectiveTime","active","moduleId","sourceId","destinationId",
															"relationshipGroup","typeId","characteristicTypeId","modifierId"};

	public Relationship() {
	}

	public Relationship(Concept source, Concept type, Concept target, int groupId) {
		this.type = type;
		this.target = target;
		this.source = source;
		if (source != null) {
			this.sourceId = source.getConceptId();
		}
		this.groupId = groupId;
		
		//Default values
		this.active = true;
		this.characteristicType = CharacteristicType.STATED_RELATIONSHIP;
		this.modifier = Modifier.EXISTENTIAL;
		this.moduleId = SCTID_CORE_MODULE;
	}

	public Relationship(Concept type, Concept value) {
		this.type = type;
		this.target = value;
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
		if (this.active != null && !this.active == newActiveState) {
			this.effectiveTime = null;
			setDirty();
		}
		this.active = newActiveState;
	}

	public String getRelationshipId() {
		return relationshipId;
	}

	public void setRelationshipId(String relationshipId) {
		this.relationshipId = relationshipId;
	}

	public Concept getType() {
		return type;
	}

	public void setType(Concept type) {
		this.type = type;
	}

	public Concept getTarget() {
		return target;
	}

	public void setTarget(Concept target) {
		this.target = target;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public CharacteristicType getCharacteristicType() {
		return characteristicType;
	}

	public void setCharacteristicType(CharacteristicType characteristicType) {
		this.characteristicType = characteristicType;
	}

	public Modifier getModifier() {
		return modifier;
	}

	public void setModifier(Modifier modifier) {
		this.modifier = modifier;
	}

	public String toShortString() {
		return "[S: " + sourceId + ", T: " + type.getConceptId() + ", D: " + target.getConceptId() + "]";
	}
	
	public String toLongString() {
		String charType = characteristicType.equals(CharacteristicType.STATED_RELATIONSHIP)?"S":"I";
		String activeIndicator = this.isActive()?"":"*";
		return "[" + activeIndicator +  charType + groupId + "] " + source + ": "+ type + " -> " + target;
	}
	
	@Override
	public String toString() {
		String charType = characteristicType.equals(CharacteristicType.STATED_RELATIONSHIP)?"S":"I";
		String activeIndicator = this.isActive()?"":"*";
		return "[" + activeIndicator +  charType + groupId + "] " + type + " -> " + target;
	}

	@Override
	public int hashCode() {
		return  toString().hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof Relationship) == false) {
			return false;
		}
		Relationship rhs = ((Relationship) other);
		
		//Must be of the same characteristic type
		if (!this.getCharacteristicType().equals(rhs.characteristicType)) {
			return false;
		}
		
		//If both sides have an SCTID, then compare that
		if (this.getRelationshipId() != null && rhs.getRelationshipId() != null) {
			return this.getRelationshipId().equals(rhs.getRelationshipId());
		}
		//Otherwise compare type / target / group 
		return (this.type.equals(rhs.type) && this.target.equals(rhs.target) && this.groupId == rhs.groupId);
	}
	
	public Relationship clone(String newSCTID) {
		Relationship clone = new Relationship();
		clone.modifier = this.modifier;
		clone.groupId = this.groupId;
		clone.relationshipId = newSCTID; 
		clone.moduleId = this.moduleId;
		clone.target = this.target;
		clone.active = this.active;
		clone.effectiveTime = null; //New relationship is unpublished
		clone.type = this.type;
		clone.sourceId = this.sourceId;
		clone.source = this.source;
		clone.characteristicType = this.characteristicType;
		clone.dirty = true;
		return clone;
	}

	@Override
	//Sort on source id, type id, target id, group id
	public int compareTo(Relationship other) {
		if (!this.sourceId.equals(other.getSourceId())) {
			return sourceId.compareTo(other.getSourceId());
		} else {
			if (!this.getType().getConceptId().equals(other.getType().getConceptId())) {
				return this.getType().getConceptId().compareTo(other.getType().getConceptId());
			} else {
				if (!this.getTarget().getConceptId().equals(other.getTarget().getConceptId())) {
					return this.getTarget().getConceptId().compareTo(other.getTarget().getConceptId());
				} else {
					if (this.getGroupId() != other.getGroupId()) {
						return ((Integer)this.getGroupId()).compareTo(other.getGroupId());
					} else {
						return 0;  //Equal in all four values
					}
				}
			}
		}
	}

	public boolean isDirty() {
		return dirty;
	}
	
	public void setDirty() {
		dirty = true;
	}
	
	public void setClean() {
		dirty = false;
	}

	public Concept getSource() {
		return source;
	}

	public void setSource(Concept source) {
		this.source = source;
		if (source != null) {
			this.sourceId = source.getConceptId();
		}
	}

	public boolean isDeleted() {
		return isDeleted;
	}

	public void delete(String deletionEffectiveTime) {
		this.isDeleted = true;
		this.deletionEffectiveTime = deletionEffectiveTime;
	}

	public String getId() {
		return relationshipId;
	}

	public ComponentType getComponentType() {
		switch (characteristicType) {
			case STATED_RELATIONSHIP : return ComponentType.STATED_RELATIONSHIP;
			case INFERRED_RELATIONSHIP : return ComponentType.RELATIONSHIP;
		default:
			return ComponentType.STATED_RELATIONSHIP;
		}
	}

	
	public Boolean isReleased() {
		//If we don't know if it's been released, fall back to the presence of an effectiveTime
		if (released == null) {
			return !(effectiveTime == null || effectiveTime.isEmpty());
		}
		return released;
	}

	public void setReleased(Boolean released) {
		this.released = released;
	}

	public boolean equalsTypeValue(Relationship rhs) {
		return this.type.equals(rhs.type) && this.target.equals(rhs.target);
	}
}
