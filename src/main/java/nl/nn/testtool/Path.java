/*
 * Created on 27-Jan-10
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.nn.testtool;

import java.util.ArrayList;
import java.util.List;

/**
 * @author m00f069
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Path {
	String[] names;
	int[] counts;
	
	protected Path(int length) {
		names = new String[length];
		counts = new int[length];
		for (int i = 0; i < length; i++) {
			counts[i] = 0;
		}
	}
	
	protected void setName(int index, String name) {
		names[index] = name;
	}

	protected void incrementCount(int index) {
		counts[index]++;
	}
	
	public boolean equals(Path path) {
		if (names.length != path.names.length) {
			return false;
		} else {
			for (int i = 0; i < names.length; i++) {
				if (!names[i].equals(path.names[i]) || counts[i] != path.counts[i]) {
					return false;
				}
			}
		}
		return true;
	}
	
	public String toString() {
		StringBuffer stringBuffer = new StringBuffer();
		for (int i = 0; i < names.length; i++) {
			stringBuffer.append("/" + names[i] + "[" + counts[i] + "]");
		}
		return stringBuffer.toString();
	}
}
