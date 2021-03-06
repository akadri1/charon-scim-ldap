package org.wso2.charon3.utils.ldapmanager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.wso2.charon3.core.attributes.Attribute;
import org.wso2.charon3.core.attributes.ComplexAttribute;
import org.wso2.charon3.core.attributes.MultiValuedAttribute;
import org.wso2.charon3.core.attributes.SimpleAttribute;
import org.wso2.charon3.core.exceptions.CharonException;
import org.wso2.charon3.core.objects.Group;
import org.wso2.charon3.core.objects.User;
import org.wso2.charon3.core.schema.SCIMConstants.UserSchemaConstants;
import org.wso2.charon3.core.schema.SCIMDefinitions;
import org.wso2.charon3.utils.ldapmanager.LdapConstants.GroupConstants;
import org.wso2.charon3.utils.ldapmanager.LdapConstants.UserConstants;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPModification;

/**
 * 
 * @author AkshathaKadri
 *
 */
public class LdapUpdateHelper {

	public static List<LDAPModification> getModifications(Group oldGrp, Group group) {
		List<LDAPModification> modList = new ArrayList<LDAPModification>();
		int op = LDAPModification.REPLACE;
		try {
			//modList = addOnlyChanged(op, GroupConstants.cn, group.getId(), oldGrp.getId(), modList);
			modList = addOnlyChanged(op, GroupConstants.name, group.getDisplayName(), oldGrp.getDisplayName(), modList);
			modList = addOnlyChanged(op, GroupConstants.createdDate, group.getCreatedDate().toString(), oldGrp.getCreatedDate().toString(), modList);
			modList = addOnlyChanged(op, GroupConstants.modifiedDate, group.getLastModified().toString(), oldGrp.getLastModified().toString(), modList);
			modList = addOnlyChanged(op, GroupConstants.location, group.getLocation(), oldGrp.getLocation(), modList);

			List<Object> members = group.getMembers();
			Set<String> addMembers = new HashSet<>();
			Set<String> remMembers = new HashSet<>();
			if(members!=null) {
				for(Object id: members){
					String uid = (String) id;
					String dn = UserConstants.uid+ "="+ uid+ "," +LdapConstants.userContainer;
					addMembers.add(dn);
				}
			}
			List<Object> oldGrpMembers = oldGrp.getMembers();
			if(oldGrpMembers!=null) {

				for(Object id: oldGrpMembers){
					String uid = (String) id;
					String dn = UserConstants.uid+ "="+ uid+ "," +LdapConstants.userContainer;
					if(!addMembers.contains(dn)){
						remMembers.add(dn);
					} else {
						addMembers.remove(dn);
					}
				}
			}
			if(remMembers!=null && !remMembers.isEmpty())
				modList.add(new LDAPModification(LDAPModification.DELETE, new LDAPAttribute(GroupConstants.member, remMembers.toArray(new String[remMembers.size()]))));
			if(addMembers!=null && !addMembers.isEmpty())
				modList.add(new LDAPModification(LDAPModification.ADD, new LDAPAttribute(GroupConstants.member, addMembers.toArray(new String[addMembers.size()]))));


		} catch (CharonException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return modList;
	}

	public static List<LDAPModification> getModifications(User user, int mode) {
		List<LDAPModification> modifications = new ArrayList<LDAPModification>();
		Map<String, Attribute> attributeList = user.getAttributeList();

		for (Attribute attribute : attributeList.values()) {

			if (attribute instanceof SimpleAttribute) {
				modifications = addSimpleAttribute(null, attribute, modifications, mode);
			} else if (attribute instanceof ComplexAttribute) {
				ComplexAttribute complexAttribute = (ComplexAttribute) attribute;
				Map<String, Attribute> subAttributes = complexAttribute.getSubAttributesList();

				for (Attribute subAttribute : subAttributes.values()) {

					if (subAttribute instanceof SimpleAttribute) {
						modifications = addSimpleAttribute(null, (Attribute) ((SimpleAttribute) subAttribute), modifications, mode);
					} else if (subAttribute instanceof MultiValuedAttribute) {

						if (!subAttribute.getType().equals(SCIMDefinitions.DataType.COMPLEX)) {
							modifications = addMultiValuedPrimitiveAttribute(((MultiValuedAttribute) subAttribute).getAttributePrimitiveValues(),
									subAttribute.getName(),  modifications, mode);
						} else {
							List<Attribute> subAttributeList  =((MultiValuedAttribute) (subAttribute)).getAttributeValues();

							for (Attribute subValue : subAttributeList) {
								ComplexAttribute complexSubAttribute = (ComplexAttribute) subValue;
								Map<String, Attribute> subSubAttributes = complexSubAttribute.getSubAttributesList();

								for (Attribute subSubAttribute : subSubAttributes.values()) {

									if (subSubAttribute instanceof SimpleAttribute) {
										modifications =  addSimpleAttribute(null,(Attribute) ((SimpleAttribute) subSubAttribute), modifications, mode);
									} else if (subSubAttribute instanceof MultiValuedAttribute) {
										modifications = addMultiValuedPrimitiveAttribute(((MultiValuedAttribute) subSubAttribute).getAttributePrimitiveValues(),
												subSubAttribute.getName(), modifications, mode);
									}
								}
							}
						}
					} else if (subAttribute instanceof ComplexAttribute) {
						ComplexAttribute complexSubAttribute = (ComplexAttribute) subAttribute;
						Map<String, Attribute> subSubAttributes = complexSubAttribute.getSubAttributesList();

						for (Attribute subSubAttribute : subSubAttributes.values()) {

							if (subSubAttribute instanceof SimpleAttribute) {
								modifications = addSimpleAttribute(null,(Attribute) ((SimpleAttribute) subSubAttribute), modifications, mode);

							} else if (subSubAttribute instanceof MultiValuedAttribute) {
								modifications = addMultiValuedPrimitiveAttribute(((MultiValuedAttribute) subSubAttribute).getAttributePrimitiveValues(),
										subSubAttribute.getName(), modifications, mode);
							}
						}
					}
				}
			} else if (attribute instanceof MultiValuedAttribute) {
				MultiValuedAttribute multiValuedAttribute = (MultiValuedAttribute) attribute;

				if (multiValuedAttribute.getType().equals(SCIMDefinitions.DataType.COMPLEX)) {
					List<Attribute> subAttributeList  = multiValuedAttribute.getAttributeValues();
					for (Attribute subAttribute : subAttributeList) {
						ComplexAttribute complexSubAttribute = (ComplexAttribute) subAttribute;
						Map<String, Attribute> subSubAttributes = complexSubAttribute.getSubAttributesList();
						//If address, check for home address START-------
						if(subAttribute.getURI().equals(UserSchemaConstants.ADDRESSES_URI)) {
							String value = null;
							boolean isHome = false;
							for (Attribute subSubAttribute : subSubAttributes.values()) {
								SimpleAttribute simpleAttribute = (SimpleAttribute) subSubAttribute;
								if(subSubAttribute.getName().equals("type")){
									//Check if type is "home"
									if(simpleAttribute.getValue().equals(UserSchemaConstants.HOME)) { 
										isHome = true;
									} else {
										if(LdapScimAttrMap.addresses.isSet()) {
											continue;
										}
										break;
									}
								} else if (subSubAttribute.getName().equals("formatted")) {
									value = (String) simpleAttribute.getValue();
								}
							}
							if(isHome) {
								modifications = LDAPModHelper(mode, LdapIPersonConstants.homePostalAddress,value, modifications);
							}
							continue;
						}
						//If address END-------
						String parent = getAttributeName(subAttribute);

						for (Attribute subSubAttribute : subSubAttributes.values()) {
							if (subSubAttribute instanceof SimpleAttribute) {

								if(subSubAttribute.getName().equals("value")) {
									modifications = addSimpleAttribute(parent, (Attribute) ((SimpleAttribute) subSubAttribute), modifications, mode);
								} 
							} else if (subSubAttribute instanceof MultiValuedAttribute) {
								modifications = addMultiValuedPrimitiveAttribute(((MultiValuedAttribute) subSubAttribute).getAttributePrimitiveValues(),
										subSubAttribute.getName(), modifications, mode);
							}
						}
					}
				} else {
					List<Object> primitiveValueList = multiValuedAttribute.getAttributePrimitiveValues();
					modifications = addMultiValuedPrimitiveAttribute(primitiveValueList,multiValuedAttribute.getName(), modifications, mode);
				}

			}
		}
		return modifications;
	}

	private static List<LDAPModification> addSimpleAttribute (String name, Attribute attribute, List<LDAPModification> modList, int mode) {
		SimpleAttribute simpleAttribute = (SimpleAttribute) attribute;
		try{
			name = name==null?simpleAttribute.getName():name;
			LdapScimAttrMap attr = LdapScimAttrMap.valueOf(name);
			modList = LDAPModHelper(mode, attr.getValue(), simpleAttribute.getValue().toString(), modList);
		} catch (Exception e){
			System.out.println("Mapping for '"+simpleAttribute.getName()+"' missing!");
		}
		return modList;
	}

	private static List<LDAPModification> addMultiValuedPrimitiveAttribute(List<Object> attributePrimitiveValues, 
			String attributeName,List<LDAPModification> modList, int mode) {
		try{
			LdapScimAttrMap name = LdapScimAttrMap.valueOf(attributeName);
			for (Object item  : attributePrimitiveValues) {
				modList = LDAPModHelper(mode, name.getValue(), (String) item, modList);
			}
		} catch (Exception e){
			System.out.println("Mapping for '"+attributeName+"' missing!");
		}
		return modList;
	}

	private static String getAttributeName (Attribute subAttribute) {
		String parent = null;
		ComplexAttribute complexSubAttribute = (ComplexAttribute) subAttribute;
		Map<String, Attribute> subSubAttributes = complexSubAttribute.getSubAttributesList();

		switch (subAttribute.getURI()) {
		case UserSchemaConstants.EMAILS_URI:
			parent = UserSchemaConstants.EMAILS;
		case UserSchemaConstants.PHONE_NUMBERS_URI:
			parent = (parent== null)?UserSchemaConstants.PHONE_NUMBERS:parent;
		case UserSchemaConstants.PHOTOS_URI:
			parent = parent== null?UserSchemaConstants.PHOTOS:parent;
			for (Attribute subSubAttribute : subSubAttributes.values()) {
				if(subSubAttribute.getName().equals("type")){
					SimpleAttribute simpleAttribute = (SimpleAttribute) subSubAttribute;
					parent = parent+"_"+simpleAttribute.getValue();
					break;
				}
			}
			break;
		case UserSchemaConstants.GROUP_URI:
			parent = UserSchemaConstants.GROUPS;
			break;
		case UserSchemaConstants.IMS_URI:
			parent = UserSchemaConstants.IMS;
			break;
		case UserSchemaConstants.X509CERTIFICATES_URI:
			parent = UserSchemaConstants.X509CERTIFICATES;
			break;
		case UserSchemaConstants.ADDRESSES_URI:
			LdapScimAttrMap.valueOf(subAttribute.getName()).setSet(true);
			break;
		}
		return parent;
	}
	private static List<LDAPModification> LDAPModHelper(int op, String key, String value, List<LDAPModification> modList){
		if(value != null && !value.isEmpty()){
			LDAPAttribute attribute = new LDAPAttribute(key, value);
			modList.add(new LDAPModification(op, attribute));
		}
		return modList;
	}
	private static List<LDAPModification> addOnlyChanged(int op, String key, String value, String oldValue, List<LDAPModification> modList){
		if(value != null && !value.isEmpty() && (oldValue==null || !oldValue.equals(value))){
			LDAPAttribute attribute = new LDAPAttribute(key, value);
			modList.add(new LDAPModification(op, attribute));
		}
		return modList;
	}

}
