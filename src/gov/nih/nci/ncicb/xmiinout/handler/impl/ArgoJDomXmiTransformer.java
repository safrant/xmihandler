/*L
 *  Copyright Ekagra Software Technologies Ltd.
 *  Copyright SAIC
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/xmihandler/LICENSE.txt for details.
 */

package gov.nih.nci.ncicb.xmiinout.handler.impl;

import gov.nih.nci.ncicb.xmiinout.domain.UMLAbstractModifier;
import gov.nih.nci.ncicb.xmiinout.domain.UMLDatatype;
import gov.nih.nci.ncicb.xmiinout.domain.UMLTaggedValue;
import gov.nih.nci.ncicb.xmiinout.domain.UMLVisibility;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLAbstractModifierBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLAttributeBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLClassBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLFinalModifierBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLInterfaceBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLOperationBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLOperationParameterBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLStaticModifierBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLStereotypeDefinitionBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLSynchronizedModifierBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLTagDefinitionBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLTaggedValueBean;
import gov.nih.nci.ncicb.xmiinout.domain.bean.UMLVisibilityBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.Namespace;

public class ArgoJDomXmiTransformer extends JDomXmiTransformer {
	
  private static Logger logger = LogManager.getLogger(ArgoJDomXmiTransformer.class
                                                  .getName());
  
   private Map<String, UMLTagDefinitionBean> tagDefinitionsByNameMap = new HashMap<String, UMLTagDefinitionBean>();
   private Map<String, UMLTagDefinitionBean> tagDefinitionsByXmiIdMap = new HashMap<String, UMLTagDefinitionBean>();
  
  private  Map<String, UMLStereotypeDefinitionBean> stereotypeDefinitionsByName = new HashMap<String, UMLStereotypeDefinitionBean>();
  private  Map<String, UMLStereotypeDefinitionBean> stereotypeDefinitionsByXmiId = new HashMap<String, UMLStereotypeDefinitionBean>();	
  
  
  public  void addTagDefinition(UMLTagDefinitionBean tagDefinition) {
    tagDefinitionsByXmiIdMap.put(((UMLTagDefinitionBean) tagDefinition).getXmiId(),
                                 tagDefinition);
    tagDefinitionsByNameMap.put(((UMLTagDefinitionBean) tagDefinition).getName(),
				tagDefinition);		
  }
  
   void addStereotypeDefinition(UMLStereotypeDefinitionBean typeDef) {
    stereotypeDefinitionsByXmiId.put(typeDef.getModelId(), typeDef);
    stereotypeDefinitionsByName.put(typeDef.getName(), typeDef);
  }
	
  public  UMLStereotypeDefinitionBean getStereotypeDefinition(String stereotype) {
    UMLStereotypeDefinitionBean defBean = stereotypeDefinitionsByXmiId.get(stereotype);
		
    if (defBean == null){
      defBean = stereotypeDefinitionsByName.get(stereotype);
    }
		
    return defBean;
  }	
		
   String getStereotypeName(Element stElt) {
    String stereotypeId = stElt.getAttribute("xmi.idref").getValue();
		
    UMLStereotypeDefinitionBean typeDef =  stereotypeDefinitionsByXmiId.get(stereotypeId);
    if (typeDef != null) {
      return typeDef.getName();
    }
    return null;

  }
	
   UMLClassBean toUMLClass(Element classElement, Namespace ns) {

	Attribute isAbstractAtt = classElement.getAttribute("isAbstract");
	String abstractModifier = isAbstractAtt != null ? isAbstractAtt.getValue()
			: "false";
	UMLAbstractModifier umlAbstractModifier = new UMLAbstractModifierBean(abstractModifier);
		
    Attribute visibilityAtt = classElement.getAttribute("visibility");
    String visibility = visibilityAtt != null ? visibilityAtt.getValue()
      : null;
    UMLVisibility umlVis = new UMLVisibilityBean(visibility);

    String stereotypeName = null;
			
    List<Element> elts = (List<Element>) classElement.getChildren(
      "ModelElement.stereotype", ns);
		
    if (elts.size() > 0) {
      Element modelStElt = elts.get(0);
      List<Element> stElts = (List<Element>) modelStElt.getChildren(
        "Stereotype", ns);
      if (stElts.size() > 0) {
        Element stElt = stElts.get(0);
        stereotypeName = getStereotypeName(stElts.get(0));
      }
    }

    UMLClassBean clazz = new UMLClassBean(classElement, classElement
                                          .getAttribute("name").getValue(), umlAbstractModifier, umlVis, stereotypeName);

    clazz.setModelId(classElement.getAttribute("xmi.id").getValue());

    addDatatype(clazz);
    return clazz;
  }
	
   UMLStereotypeDefinitionBean toUMLStereotypeDefinition(Element typeElt) {
    String xmiId = typeElt.getAttribute("xmi.id").getValue();

    Attribute nameAtt = typeElt.getAttribute("name");
    String name = null;
    if (nameAtt != null)
      name = nameAtt.getValue();
    else
      name = "";

    UMLStereotypeDefinitionBean result = new UMLStereotypeDefinitionBean(typeElt, xmiId, name);
    result.setModelId(xmiId);
    return result;
  }

   UMLInterfaceBean toUMLInterface(Element interfaceElement, Namespace ns) {

	   String stereotypeName = "interface";

	   UMLInterfaceBean interfaze = new UMLInterfaceBean(interfaceElement, interfaceElement
			   .getAttribute("name").getValue(), stereotypeName);

	   interfaze.setModelId(interfaceElement.getAttribute("xmi.id").getValue());

	   addDatatype(interfaze);
	   return interfaze;
   }
   
   UMLAttributeBean toUMLAttribute(Element attElement, Namespace ns) {
    Attribute visibilityAtt = attElement.getAttribute("visibility");
    String visibility = visibilityAtt != null ? visibilityAtt.getValue()
      : null;
    UMLVisibility umlVis = new UMLVisibilityBean(visibility);

    UMLDatatype datatype = null;

    logger.debug("***attElement.getAttribute('name'): " + attElement.getAttribute("name"));
		
    Element sfTypeElement = attElement.getChild("StructuralFeature.type", ns);		
    logger.debug("sfTypeElement: " + sfTypeElement);
		
    if (sfTypeElement != null) {
      Element classifElt = sfTypeElement.getChild("DataType", ns);
      logger.debug("classifElt: " + classifElt);
			
      if (classifElt == null) {
        classifElt = sfTypeElement.getChild("Class", ns);
        logger.debug("classifElt: " + classifElt);
      }
      if (classifElt != null) {
        Attribute typeAtt = classifElt.getAttribute("xmi.idref");
        if (typeAtt != null) {
          String typeId = typeAtt.getValue();
          logger.debug("*** typeId: " + typeId);
          datatype = datatypes.get(typeId);
        }
      }
    }

    UMLAttributeBean att = new UMLAttributeBean(attElement, attElement
                                                .getAttribute("name").getValue(), datatype, umlVis);

    // maybe we haven't discovered the datatype yet.
    if (datatype == null) {
      logger.debug("*** datatype is null; will try to discover later");
      attWithMissingDatatypes.add(att);
    }

    return att;
  }	

	 UMLOperationBean toUMLOperation(Element operationElement, Namespace ns) {

		 	String stereotypeName = null;
		 	logger.debug("Parsing operationElement: "+operationElement.getAttributeValue("name"));
			Attribute visibilityAtt = operationElement.getAttribute("visibility");
			String visibility = visibilityAtt != null ? visibilityAtt.getValue()
					: null;
			UMLVisibility umlVis = new UMLVisibilityBean(visibility);

			logger.debug("***attElement.getAttribute('name'): " + operationElement.getAttribute("name"));


			Attribute abstractAtt = operationElement.getAttribute("isAbstract");
			Attribute synchAtt = operationElement.getAttribute("concurrency");
			Attribute finalAtt = operationElement.getAttribute("isLeaf");
			Attribute staticAtt = operationElement.getAttribute("ownerScope");
			
			
			List<Element> elts = (List<Element>) operationElement.getChildren(
					"ModelElement.stereotype", ns);
			
			if (elts.size() > 0) {
				Element modelStElt = elts.get(0);
				List<Element> stElts = (List<Element>) modelStElt.getChildren(
						"Stereotype", ns);
				if (stElts.size() > 0) {
					Element stElt = stElts.get(0);
					stereotypeName = getStereotypeName(stElts.get(0));
				}
			}
			
			Element sfTypeElement = operationElement.getChild("BehavioralFeature.parameter", ns);		
			logger.debug("BehavioralFeature.parameter: " + sfTypeElement);
			Attribute specification = operationElement.getAttribute("specification");

			UMLOperationBean operation = new UMLOperationBean(operationElement, operationElement
					.getAttribute("name").getValue(), stereotypeName, specification==null?null:specification.getValue(), umlVis);

			if(staticAtt != null && staticAtt.getValue() != null && staticAtt.getValue().equals("classifier"))
				operation.setStaticModifier(new UMLStaticModifierBean("true"));
			if(abstractAtt != null && abstractAtt.getValue() != null && abstractAtt.getValue().equals("true"))
				operation.setAbstractModifier(new UMLAbstractModifierBean("true"));
			if(synchAtt != null && synchAtt.getValue() != null && synchAtt.getValue().equals("guarded"))
				operation.setSynchronizedModifier(new UMLSynchronizedModifierBean("true"));
			if(finalAtt != null && finalAtt.getValue() != null && finalAtt.getValue().equals("true"))
				operation.setFinalModifier(new UMLFinalModifierBean("true"));

			if (sfTypeElement != null) {
				List<Element> paramElements = (List<Element>) sfTypeElement.getChildren(
						"Parameter", ns);
				if(paramElements != null && paramElements.size() > 0)
				{
					for(Element paramElt : paramElements) {
						UMLOperationParameterBean parameter = toUMLOperationParameter(paramElt, ns);
						operation.addOperationParameter(parameter);
					}
				}
			}

			Element modelElement = operationElement.getChild("ModelElement.taggedValue", ns);
			if(modelElement != null)
			{
				List<Element> tvElements = (List<Element>)modelElement.getChildren("TaggedValue", ns);
				for(Element tvElt : tvElements) {
					UMLTaggedValue tv = toUMLTaggedValue(tvElt);
					if(tv != null)
						operation.addTaggedValue(tv);
				}
			}

			return operation;
		}

	 
	 UMLOperationParameterBean toUMLOperationParameter(Element operationParamElement, Namespace ns) {

			Attribute visibilityAtt = operationParamElement.getAttribute("visibility");
			String visibility = visibilityAtt != null ? visibilityAtt.getValue()
					: null;
			UMLVisibility umlVis = new UMLVisibilityBean(visibility);
			
			Attribute kindAtt = operationParamElement.getAttribute("kind");
			String kind = kindAtt != null ? kindAtt.getValue()
					: null;
			
			UMLDatatype datatype = null;

			logger.debug("***attElement.getAttribute('name'): " + operationParamElement.getAttribute("name"));
			
			Element sfTypeElement = operationParamElement.getChild("Parameter.type", ns);		
			logger.debug("Parameter.type: " + sfTypeElement);
			
			if (sfTypeElement != null) {
				Element classifElt = sfTypeElement.getChild("DataType", ns);
				logger.debug("DataTypeElt: " + classifElt);
				
				if (classifElt != null) {
					Attribute typeAtt = classifElt.getAttribute("xmi.idref");
					if (typeAtt != null) {
						String typeId = typeAtt.getValue();
						logger.debug("*** typeId: " + typeId);
						datatype = datatypes.get(typeId);
					}
				}
			}

			Attribute nameAttr = operationParamElement.getAttribute("name");
			String attrName =  nameAttr != null ? nameAttr.getValue(): null; 
			UMLOperationParameterBean paramBean = new UMLOperationParameterBean(operationParamElement, attrName, umlVis, datatype,  kind);

			// maybe we haven't discovered the datatype yet.
			if (datatype == null) {
				logger.debug("*** datatype is null; will try to discover later");
				paramWithMissingDatatypes.add(paramBean);
			}
			
			Element modelElement = operationParamElement.getChild("ModelElement.taggedValue", ns);
			if(modelElement != null)
			{
				List<Element> tvElements = (List<Element>)modelElement.getChildren("TaggedValue", ns);
				for(Element tvElt : tvElements) {
					UMLTaggedValue tv = toUMLTaggedValue(tvElt);
					if(tv != null)
						paramBean.addTaggedValue(tv);
				}
			}

			return paramBean;
		}
   
  /**
   * Run this after you've processed attributes for attributes that use
   * classes as datatypes.
   */
   void completeAttributes(Namespace ns) {
    for (UMLAttributeBean att : attWithMissingDatatypes) {
      Element attElement = att.getJDomElement();

      Element sfTypeElement = attElement.getChild(
        "StructuralFeature.type", ns);
      if (sfTypeElement != null) {
        Element classifElt = sfTypeElement.getChild("DataType", ns);
        if (classifElt == null) {
          classifElt = sfTypeElement.getChild("Class", ns);
        }
				
        if (classifElt != null) {
          Attribute typeAtt = classifElt.getAttribute("xmi.idref");
          if (typeAtt != null) {
            String typeId = typeAtt.getValue();
            att._setDatatype(datatypes.get(typeId));
          }
        }
      }
    }

  }	

	/**
	 * Run this after you've processed attributes for attributes that use
	 * classes as datatypes.
	 */
	 void completeOperations(Namespace ns) {
		for (UMLOperationParameterBean att : paramWithMissingDatatypes) {
			Element attElement = att.getJDomElement();

			Element sfTypeElement = attElement.getChild(
					"Parameter.type", ns);
			if (sfTypeElement != null) {
				Element classifElt = sfTypeElement.getChild("DataType", ns);
		        if (classifElt == null) {
		            classifElt = sfTypeElement.getChild("Class", ns);
		          }

				if (classifElt != null) {
					Attribute typeAtt = classifElt.getAttribute("xmi.idref");
					if (typeAtt != null) {
						String typeId = typeAtt.getValue();
						att._setDatatype(datatypes.get(typeId));
					}
				}
			}
		}

	}
   
   UMLTagDefinitionBean toUMLTagDefinition(Element tdElement) {
    //
    // EA
    // None
    //
    // ArgoUML
    // <UML:TagDefinition xmi.id="-64--88-1-107-8238f4:1121acba21f:-8000:000000000000317C" name="type" isSpecification="false" tagType="String" />
    //	 
		
    if (tdElement.getAttribute("name") == null) {
      logger.info("tagDefinition missing 'name' attribute, skipping");
      return null;
    }

    UMLTagDefinitionBean td = new UMLTagDefinitionBean(tdElement, tdElement
                                                       .getAttribute("xmi.id").getValue(), tdElement.getAttribute(
                                                         "name").getValue());
    return td;
  }
	
  UMLTaggedValueBean toUMLTaggedValue(Element tvElement, Namespace ns) {
    //
    // EA
    // <UML:TaggedValue tag="myClassTaggedValue" value="test 123"
    // xmi.id="1D2F36D4_E881_44C1_8051_106A39593C26"
    // modelElement="EAID_05D28ABC_F678_4c6a_AA5C_513217EB2E68" />
    //
    // ArgoUML
    // <UML:TaggedValue xmi.id="EAID_E5485B89_9415_42ea_AC45_28D8A2349539_fix_2_fix_0_fix_0"
    // isSpecification="false">
    // <UML:TaggedValue.dataValue>gov.nih.nci.cacoresdk.domain.manytomany.bidirectional.Project.employeeCollection</UML:TaggedValue.dataValue>
    // <UML:TaggedValue.type>
    // <UML:TagDefinition xmi.idref="-64--88-1-107-16925b0:1120c726d7c:-8000:0000000000003170" />
    // </UML:TaggedValue.type>
    // </UML:TaggedValue>
    
    Element dataValueElement = tvElement.getChild("TaggedValue.dataValue",
                                                  ns);
    Element typeElement = tvElement.getChild("TaggedValue.type", ns);
    
    if (typeElement == null) {
      logger.info("taggedValue "
                  + tvElement.getAttribute("xmi.id").getValue()
                  + " missing 'TagDefinition' element, skipping");
      logger.debug("taggedValue "
                   + tvElement.getAttribute("xmi.id").getValue()
                   + " missing 'TagDefinition' element, skipping");
      
      return null;
    }
    
    if (dataValueElement == null) {
      logger.info("taggedValue missing dataValue Element, skipping");
      return null;
    }
    
    Element tagDefinitionElement = typeElement
      .getChild("TagDefinition", ns);
    
//     logger.debug("*** tagDefinition name: "
//                  + tagDefinitionsByXmiIdMap.get(tagDefinitionElement.getAttributeValue("xmi.idref")).getName());		
    
//     logger.debug("*** dataValue: "
//                  + dataValueElement.getText());

    if(tagDefinitionsByXmiIdMap.get(tagDefinitionElement.getAttributeValue("xmi.idref")) == null) {
      logger.info("Cannot find tagDefinition with xmi.idref = " + tagDefinitionElement.getAttributeValue("xmi.idref") + " -- We are skipping the tag value pointing to it.");
      return null;
    }
       
    
    UMLTaggedValueBean tv = new UMLTaggedValueBean(tvElement,
                                                   tagDefinitionsByXmiIdMap.get(tagDefinitionElement.getAttributeValue("xmi.idref")).getName(),
                                                   dataValueElement.getText());
    return tv;
  }

  public  UMLTagDefinitionBean getTagDefinitionByName(String name) {
    return tagDefinitionsByNameMap.get(name);
  }

  public  UMLTagDefinitionBean getTagDefinitionByXmiId(String xmiId) {
    return tagDefinitionsByNameMap.get(xmiId);
  }

  public  UMLStereotypeDefinitionBean addStereotypeDefinition(Element elt, String name) {
    Namespace ns = elt.getNamespace();
    logger.debug("Namespace: " + ns);
		
    Element rootElt = elt.getDocument().getRootElement();
    logger.debug("Root Element name: " + rootElt.getName());
    Element xmiElt = rootElt.getChild("XMI");	
    logger.debug("XMI Element name: " + xmiElt.getName());		
    Element xmiContentElt = xmiElt.getChild("XMI.content");
    logger.debug("XMI Content Element name: " + xmiContentElt.getName());		

	
    List<Element> xmiContentChildren = (List<Element>)xmiContentElt.getChildren();
    logger.debug("xmiContentChildren Elements found: " + xmiContentChildren.size());
	    
    Element modelElt = null;
    for(Element xmiContentChild : xmiContentChildren) {
      logger.debug("xmiContentChild: " + xmiContentChild.getName());
	    	
      if (xmiContentChild.getName().equalsIgnoreCase("Model")){
        modelElt = xmiContentChild;
      }
    }		
	    
    logger.debug("Model Element name: " + modelElt.getName());	    

    Element ownedElement = modelElt.getChild("Namespace.ownedElement", ns);	
    logger.debug("ownedElement name: " + ownedElement.getName());		
		
    String xmiId = java.util.UUID.randomUUID().toString().replace('-','_').toUpperCase();
    UMLStereotypeDefinitionBean stereotypeDefBean = new UMLStereotypeDefinitionBean(ownedElement, xmiId, name); 
		
		
    Element newStereotypeElt = new Element("Stereotype", ns);
		
    newStereotypeElt.setAttribute("xmi.id", xmiId);
    newStereotypeElt.setAttribute("name", name);

    //<UML:Stereotype.baseClass>Dependency</UML:Stereotype.baseClass>
    Element stBaseClassElt = new Element("Stereotype.baseClass", ns);
    stBaseClassElt.setText("Dependency");	
		
    newStereotypeElt.addContent(stBaseClassElt);

    ownedElement.addContent(newStereotypeElt);

    addStereotypeDefinition(stereotypeDefBean);

    return (UMLStereotypeDefinitionBean)stereotypeDefBean;
  }
	
}