package com.termmed.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;

public class TClosure {
	
	HashMap<Long,HashSet<Long>>parentHier;
	HashMap<Long,HashSet<Long>>childrenHier;
	private long ISARELATIONSHIPTYPEID=116680003l;
	private String ROOT_CONCEPT = "138875005";
	String rf2Rels;
	public TClosure(String rf2Rels) throws FileNotFoundException, IOException{
		parentHier=new HashMap<Long,HashSet<Long>>();
		childrenHier=new HashMap<Long,HashSet<Long>>();
		this.rf2Rels=rf2Rels;
		loadIsas();
	}
	private void loadIsas() throws IOException, FileNotFoundException {
		System.out.println("Starting Isas Relationships from: " + rf2Rels);
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(rf2Rels), "UTF8"));
		try {
			String line = br.readLine();
			line = br.readLine(); // Skip header
			int count = 0;
			while (line != null) {
				if (line.isEmpty()) {
					continue;
				}
				String[] columns = line.split("\\t");
				if (Long.parseLong(columns[7])==ISARELATIONSHIPTYPEID 
						&& columns[2].equals("1") 
						&& !columns[4].equals(ROOT_CONCEPT)){
					addRel(Long.parseLong(columns[5]),Long.parseLong(columns[4]));
					
					count++;
					if (count % 100000 == 0) {
						System.out.print(".");
					}
				}
				line = br.readLine();
			}
			System.out.println(".");
			System.out.println("Parent isas Relationships loaded = " + parentHier.size());
			System.out.println("Children isas Relationships loaded = " + childrenHier.size());
		} finally {
			br.close();
		}		
	}
	public void addRel(Long parent, Long child){
		if (parent==child){
			System.out.println("same child and parent: " + child);
			return;
		}
		HashSet<Long> parentList=parentHier.get(child);
		if (parentList==null){
			parentList=new HashSet<Long>();
		}
		parentList.add(parent);
		parentHier.put(child, parentList);
		
		HashSet<Long> childrenList=childrenHier.get(parent);
		if (childrenList==null){
			childrenList=new HashSet<Long>();
		}
		childrenList.add(child);
		childrenHier.put(parent, childrenList);
	}
	
	public boolean isAncestorOf(Long ancestor,Long descendant){
		
		HashSet<Long>parent=parentHier.get(descendant);
		if (parent==null){
			return false;
		}
		if (parent.contains(ancestor)){
			return true;
		}
		for(Long par:parent){
			if (isAncestorOf(ancestor,par)){
				return true;
			}
		}
		return false;
	}
	
	public HashSet<Long> getParent(Long conceptId) {
		return parentHier.get(conceptId);
	}

	public HashSet<Long> getChildren(Long conceptId) {
		return childrenHier.get(conceptId);
	}
	public HashSet<Long> getDescendants(Long conceptId) {
		HashSet<Long> hControl = new HashSet<Long>();
		HashSet<Long> children=childrenHier.get(conceptId);
		if (children==null){
			return hControl;
		}

		for(Long child:children){
			if (!hControl.contains(child)){
				hControl.add(child);
				hControl.addAll(getDescendants( child));
			}
		}				
		return hControl;
	}
	public HashSet<Long> getAncestors(Long conceptId) {
		HashSet<Long> hControl = new HashSet<Long>();
		HashSet<Long> parent=parentHier.get(conceptId);
		if (parent==null){
			return hControl;
		}

		for(Long par:parent){
			if (!hControl.contains(par)){
				hControl.add(par);
				hControl.addAll(getAncestors( par));
			}
		}				
		return hControl;
	}
}
