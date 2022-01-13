/*L
 *  Copyright Ekagra Software Technologies Ltd.
 *  Copyright SAIC
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/xmihandler/LICENSE.txt for details.
 */


package gov.nih.nci.ncicb.xmiinout.handler.impl;


import gov.nih.nci.ncicb.xmiinout.domain.*;
import gov.nih.nci.ncicb.xmiinout.domain.bean.*;

import java.util.*;

import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;

import org.jaxen.JaxenException;
import org.jaxen.jdom.JDOMXPath;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

/**
 * 
 */
public class EADefaultImpl extends EABaseImpl {

	private static Logger logger = LogManager.getLogger(EADefaultImpl.class.getName());

	protected List<UMLDatatype> doDataTypes(Element modelElt) {
		Namespace ns = Namespace.getNamespace("omg.org/UML1.3");
		Element ownedElement = modelElt.getChild("Namespace.ownedElement", ns);

		List<Element> typeElements = (List<Element>)ownedElement.getChildren("DataType", ns);

		List<UMLDatatype> result = new ArrayList<UMLDatatype>();

		for(Element typeElt : typeElements) {
			result.add(jdomXmiTransformer.toUMLDatatype(typeElt));
		}
		return result;

	}

	protected List<UMLTaggedValue> doTaggedValues(Element elt) {
		Namespace ns = Namespace.getNamespace("omg.org/UML1.3");
		Element modelElement = elt.getChild("ModelElement.taggedValue", ns);

		List<UMLTaggedValue> result = new ArrayList<UMLTaggedValue>();
		if(modelElement == null)
			return result;

		List<Element> tvElements = (List<Element>)modelElement.getChildren("TaggedValue", ns);
		for(Element tvElt : tvElements) {
			UMLTaggedValue tv = jdomXmiTransformer.toUMLTaggedValue(tvElt);
			if(tv != null)
				result.add(tv);
		}

		return result;
	}

	protected  List<UMLAttribute> doAttributes(Element classElement) {
		Namespace ns = Namespace.getNamespace("omg.org/UML1.3");
		Element featureElement = classElement.getChild("Classifier.feature", ns);

		List<UMLAttribute> result = new ArrayList<UMLAttribute>();
		if(featureElement == null)
			return result;

		List<Element> attElements = (List<Element>)featureElement.getChildren("Attribute", ns);

		for(Element attElt : attElements) {
			UMLAttributeBean umlAtt = jdomXmiTransformer.toUMLAttribute(attElt, ns);

			Collection<UMLTaggedValue> taggedValues = doTaggedValues(attElt);
			for(UMLTaggedValue tv : taggedValues) {
				umlAtt.addTaggedValue(tv);
			}

			result.add(umlAtt);
		}

		return result;

	}

	protected  List<UMLOperation> doOperations(Element classElement) {
		Namespace ns = Namespace.getNamespace("omg.org/UML1.3");
		Element featureElement = classElement.getChild("Classifier.feature", ns);

		List<UMLOperation> result = new ArrayList<UMLOperation>();
		if(featureElement == null)
			return result;

		List<Element> attElements = (List<Element>)featureElement.getChildren("Operation", ns);

		for(Element attElt : attElements) {
			UMLOperationBean umlAtt = jdomXmiTransformer.toUMLOperation(attElt, ns);

			Collection<UMLTaggedValue> taggedValues = doTaggedValues(attElt);
			for(UMLTaggedValue tv : taggedValues) {
				umlAtt.addTaggedValue(tv);
			}

			result.add(umlAtt);
		}

		return result;

	}
	
	public List<UMLDependency> doDependencies(Element modelElement) throws JaxenException {
		String xpath = "//*[local-name()='Dependency']";
		Namespace ns = Namespace.getNamespace("omg.org/UML1.3");

		JDOMXPath path = new JDOMXPath(xpath);
		List<Element> depElts = path.selectNodes(rootElement);

		List<UMLDependency> result = new ArrayList<UMLDependency>();

		for(Element depElt : depElts) {
			UMLDependencyEnd client = idClassMap.get(depElt.getAttribute("client").getValue());
			UMLDependencyEnd supplier = idClassMap.get(depElt.getAttribute("supplier").getValue());

			if(client == null) {
				client = idInterfaceMap.get(depElt.getAttribute("client").getValue());
				
				if(client == null) {
					logger.info("Can't find client for dependency: " + depElt.getAttribute("xmi.id") + " -- ignoring");
					continue;
				}
			}
			if(supplier == null) {
				supplier = idInterfaceMap.get(depElt.getAttribute("supplier").getValue());
				
				if(supplier == null) {
					logger.info("Can't find supplier for dependency: " + depElt.getAttribute("xmi.id") + " -- ignoring");
					continue;
				}
			}


			Attribute nameAtt = depElt.getAttribute("name");
			String depName = null;
			if(nameAtt != null)
				depName = nameAtt.getValue();

			Attribute visAtt = depElt.getAttribute("visibility");
			UMLVisibility visibility = null;
			if(visAtt != null) {
				visibility = new UMLVisibilityBean(visAtt.getValue());
			}

			String stereotype = null;
			List<Element> elts = (List<Element>) depElt.getChildren("ModelElement.stereotype", ns);
			if (elts.size() > 0) {
				Element modelStElt = elts.get(0);
				List<Element> stElts = (List<Element>) modelStElt.getChildren("Stereotype", ns);
				if (stElts.size() > 0) {
					Element stElt = stElts.get(0);
					stereotype = stElt.getAttribute("name").getValue();
					//logger.debug("Dependency Stereotype:  " + stereotype);
				}
			}      

			result.add(new UMLDependencyBean(depElt, depName, visibility, client, supplier, stereotype));
		}    

		return result;

	}


	public List<UMLGeneralization> doGeneralizations(Element modelElement) throws JaxenException {
		String xpath = "//*[local-name()='Generalization']";

		JDOMXPath path = new JDOMXPath(xpath);
		List<Element> genElts = path.selectNodes(rootElement);

		List<UMLGeneralization> result = new ArrayList<UMLGeneralization>();

		for(Element genElt : genElts) {
			String subtypeId = genElt.getAttribute("subtype").getValue();
			String supertypeId = genElt.getAttribute("supertype").getValue();
			
			UMLClassBean subClass = idClassMap.get(subtypeId);
			UMLClassBean superClass = idClassMap.get(supertypeId);
			
			if (subClass != null && superClass != null){
				result.add(new UMLGeneralizationBean(genElt, superClass, subClass));

			} else {
				UMLInterfaceBean subInterface = idInterfaceMap.get(subtypeId);
				UMLInterfaceBean superInterface = idInterfaceMap.get(supertypeId);
				result.add(new UMLGeneralizationBean(genElt, superInterface, subInterface));
			}
		}    

		return result;

	}

	public List<UMLAssociation> doAssociations(Element modelElement) throws JaxenException {
		Namespace ns = Namespace.getNamespace("omg.org/UML1.3");
		String xpath = "//*[local-name()='Association']";

		JDOMXPath path = new JDOMXPath(xpath);
		List<Element> assocElts = path.selectNodes(rootElement);

		List<UMLAssociation> result = new ArrayList<UMLAssociation>();

		for(Element assocElt : assocElts) {

			Element connectionElement = assocElt.getChild("Association.connection", ns);

			if(connectionElement == null)
				continue;

			List<Element> endElements = (List<Element>)connectionElement.getChildren("AssociationEnd", ns);

			UMLAssociationEndBean srcEnd = null, targetEnd = null;

			for(Element endElt : endElements) {
				UMLClassBean endClass = idClassMap.get(endElt.getAttribute("type").getValue());

				int low = 0, high = 0;
				org.jdom.Attribute multAtt = endElt.getAttribute("multiplicity");

				if(multAtt != null) {
					String multiplicity = multAtt.getValue();
					String[] multiplicities = multiplicity.split("\\.\\.");
					low = multiplicities[0].equals("*")?-1:Integer.valueOf(multiplicities[0]);
					if(multiplicities.length > 1)
						high = multiplicities[1].equals("*")?-1:Integer.valueOf(multiplicities[1]);
					else high = low;
				}

				boolean navigable = Boolean.valueOf(endElt.getAttribute("isNavigable").getValue());

				org.jdom.Attribute nameAtt = endElt.getAttribute("name");
				String name = nameAtt != null?nameAtt.getValue():"";

				UMLAssociationEndBean endBean = new UMLAssociationEndBean
				(endElt,
						endClass,
						name,
						low,
						high,
						navigable);

				if(srcEnd == null)
					srcEnd = endBean;
				else
					targetEnd = endBean;

				Collection<UMLTaggedValue> taggedValues = doTaggedValues(endElt);
				for(UMLTaggedValue tv : taggedValues) {
					endBean.addTaggedValue(tv);
				}

			}

			List<UMLAssociationEnd> endBeans = new ArrayList<UMLAssociationEnd>();
			endBeans.add(srcEnd);
			endBeans.add(targetEnd);

			if(srcEnd.getUMLElement() == null || targetEnd.getUMLElement() == null) {
				logger.info("Can't find end class for Association: " + assocElt.getAttribute("xmi.id") + " -- only associations to classes are supported -- ignoring");
				continue;
			}


			Attribute nameAtt = assocElt.getAttribute("name");
			String assocRoleName = null;
			if(nameAtt != null)
				assocRoleName = nameAtt.getValue();


			UMLAssociationBean assoc = new UMLAssociationBean(assocElt, assocRoleName, endBeans);

			Collection<UMLTaggedValue> taggedValues = doTaggedValues(assocElt);
			for(UMLTaggedValue tv : taggedValues) {
				assoc.addTaggedValue(tv);
			}

			result.add(assoc);
		}    

		return result;

	}



}