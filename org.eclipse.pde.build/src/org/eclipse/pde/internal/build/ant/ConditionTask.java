package org.eclipse.pde.internal.core.ant;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.PrintWriter;
/**
 * Represents an Ant condition.
 */
public class ConditionTask {

	protected String property;
	protected String value;
	protected Condition condition;

public ConditionTask(String property, String value, Condition condition) {
	this.property = property;
	this.value = value;
	this.condition = condition;
}

protected void print(AntScript script, int tab) {
	script.printTab(tab);
	script.print("<condition");
	script.printAttribute("property", property, true);
	script.printAttribute("value", value, false);
	script.println(">");
	condition.print(script, ++tab);
	script.printTab(--tab);
	script.println("</condition>");
}
}