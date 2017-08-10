package org.wso2.charon3.utils.ldapmanager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.wso2.charon3.core.attributes.Attribute;
import org.wso2.charon3.core.attributes.ComplexAttribute;
import org.wso2.charon3.core.attributes.DefaultAttributeFactory;
import org.wso2.charon3.core.attributes.MultiValuedAttribute;
import org.wso2.charon3.core.attributes.SimpleAttribute;
import org.wso2.charon3.core.exceptions.BadRequestException;
import org.wso2.charon3.core.exceptions.CharonException;
import org.wso2.charon3.core.objects.Group;
import org.wso2.charon3.core.objects.User;
import org.wso2.charon3.core.schema.AttributeSchema;
import org.wso2.charon3.core.schema.SCIMConstants;
import org.wso2.charon3.core.schema.SCIMConstants.UserSchemaConstants;
import org.wso2.charon3.core.schema.SCIMDefinitions;
import org.wso2.charon3.core.schema.SCIMSchemaDefinitions;
import org.wso2.charon3.core.utils.AttributeUtil;
import org.wso2.charon3.utils.ldapmanager.LdapConstants.GroupConstants;
import org.wso2.charon3.utils.ldapmanager.LdapConstants.UserConstants;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPCompareAttrNames;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;

/**
 * 
 * @author AkshathaKadri
 * 
 * @author ReenaHegde
 *
 */

public class LdapUtil {

	public static User convertLdapToUserForList(LDAPEntry entry) {
		User user = new User();
		try {
			LDAPAttributeSet attributeSet = entry.getAttributeSet();
			Date createdDate;
			Date lastModified;
			String createdDateStr;
			String lastModifiedStr;
			SimpleDateFormat ldapFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
			SimpleDateFormat scimFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

			user.setId(attributeSet.getAttribute(LdapScimAttrMap.id.getValue()).getStringValue());
			user.setUserName(attributeSet.getAttribute(LdapScimAttrMap.userName.getValue()).getStringValue());
			if (attributeSet.getAttribute(LdapScimAttrMap.created.getValue()) != null) {
				createdDate = ldapFormat
						.parse(attributeSet.getAttribute(LdapScimAttrMap.created.getValue()).getStringValue());
				createdDateStr = scimFormat.format(createdDate);
				createdDate = AttributeUtil.parseDateTime(createdDateStr);
			} else {
				createdDate = new Date();
			}
			user.setCreatedDate(createdDate);

			if (attributeSet.getAttribute(LdapScimAttrMap.lastModified.getValue()) != null) {
				lastModified = ldapFormat
						.parse(attributeSet.getAttribute(LdapScimAttrMap.lastModified.getValue()).getStringValue());
				lastModifiedStr = scimFormat.format(lastModified);
				lastModified = AttributeUtil.parseDateTime(lastModifiedStr);
			} else {
				lastModified = new Date();
			}
			user.setLastModified(lastModified);

			if (attributeSet.getAttribute(LdapScimAttrMap.location.getValue()) != null) {
				user.setLocation(attributeSet.getAttribute(LdapScimAttrMap.location.getValue()).getStringValue());
			} else {
				user.setLocation("http://localhost:8080/scim/v2/Users/" + user.getId());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return user;

	}


	/**
	 * Converts LDAP Entry object in to SCIM object
	 * @param entry
	 * @return
	 */
	public static User convertLdapToUser(LDAPEntry entry) {

		User user = convertLdapToUserForList(entry);
		LDAPAttribute attribute;
		ComplexAttribute nameAttribute = null;
		ComplexAttribute homeAddrAttribute = null;
		ComplexAttribute workAddrAttribute = null;
		ComplexAttribute homeEmailAttribute = null;
		ComplexAttribute workEmailAttribute = null;
		MultiValuedAttribute addressAttribute = null;
		MultiValuedAttribute emailAttribute = null;
		SimpleAttribute addressFormatted = null;
		SimpleAttribute nameSubAttribute;
		SimpleAttribute addressType = null;
		SimpleAttribute workAddressSubAttribute = null;
		SimpleAttribute emailType = null;
		SimpleAttribute emailValue = null;
		String subAttributeName = "";
		Object value = "";
		AttributeSchema schema = null;
		try {
			LDAPAttributeSet attributeSet = entry.getAttributeSet();

			Iterator<LDAPAttribute> iterator = attributeSet.iterator();
			while (iterator.hasNext()) {
				attribute = iterator.next();
				value = attribute.getStringValue();
				// subAttributeName =
				// LdapScimAttrMap.getByLdapAttrName(attribute.getName()).toString();
				if (attribute.getName()
						.matches(LdapIPersonConstants.fullName + "|" + LdapIPersonConstants.givenName + "|"
								+ LdapIPersonConstants.initials + "|" + LdapIPersonConstants.pager + "|"
								+ LdapIPersonConstants.description)) {
					if(nameAttribute == null){
						nameAttribute = createComplexAttribute(SCIMSchemaDefinitions.SCIMUserSchemaDefinition.NAME,
								SCIMConstants.UserSchemaConstants.NAME, user, true);
					}
					
					if (attribute.getName().equals(LdapIPersonConstants.fullName)) {
						subAttributeName = SCIMConstants.UserSchemaConstants.FORMATTED_NAME;
						schema = SCIMSchemaDefinitions.SCIMUserSchemaDefinition.FORMATTED;
					} else if (attribute.getName().equals(LdapIPersonConstants.givenName)) {
						subAttributeName = SCIMConstants.UserSchemaConstants.GIVEN_NAME;
						schema = SCIMSchemaDefinitions.SCIMUserSchemaDefinition.GIVEN_NAME;
					} else if (attribute.getName().equals(LdapIPersonConstants.initials)) {
						subAttributeName = SCIMConstants.UserSchemaConstants.MIDDLE_NAME;
						schema = SCIMSchemaDefinitions.SCIMUserSchemaDefinition.MIDDLE_NAME;
					} else if (attribute.getName().equals(LdapIPersonConstants.sn)) {
						subAttributeName = SCIMConstants.UserSchemaConstants.FAMILY_NAME;
						schema = SCIMSchemaDefinitions.SCIMUserSchemaDefinition.FAMILY_NAME;
					} else if (attribute.getName().equals(LdapIPersonConstants.pager)) {
						subAttributeName = SCIMConstants.UserSchemaConstants.HONORIFIC_PREFIX;
						schema = SCIMSchemaDefinitions.SCIMUserSchemaDefinition.HONORIFIC_PREFIX;
					} else if (attribute.getName().equals(LdapIPersonConstants.description)) {
						subAttributeName = SCIMConstants.UserSchemaConstants.HONORIFIC_SUFFIX;
						schema = SCIMSchemaDefinitions.SCIMUserSchemaDefinition.HONORIFIC_SUFFIX;
					}
					nameSubAttribute = createSimpleSubAttribute(subAttributeName, value, schema);
					nameAttribute.setSubAttribute(nameSubAttribute);

				}

				if (attribute.getName().matches(LdapIPersonConstants.homePostalAddress + "|" + LdapIPersonConstants.street + "|"
								+ LdapIPersonConstants.l + "|" + LdapIPersonConstants.st + "|"
								+ LdapIPersonConstants.postalCode + "|" + LdapIPersonConstants.destinationIndicator)) {
					if(addressAttribute == null){
						addressAttribute = createMultiValuedAttribute(
								SCIMSchemaDefinitions.SCIMUserSchemaDefinition.ADDRESSES,
								SCIMConstants.UserSchemaConstants.ADDRESSES, user);
					}

					if (attribute.getName().equals(LdapIPersonConstants.homePostalAddress)) {
						subAttributeName = SCIMConstants.UserSchemaConstants.ADDRESSES + "_" + SCIMConstants.UserSchemaConstants.HOME;
						schema = SCIMSchemaDefinitions.SCIMUserSchemaDefinition.ADDRESSES;
						if(homeAddrAttribute == null){
							homeAddrAttribute = createComplexAttribute(schema,subAttributeName, user, false);
							homeAddrAttribute.setMultiValued(false);
						}
						
						addressType = createSimpleSubAttribute(SCIMConstants.CommonSchemaConstants.
								  TYPE, SCIMConstants.UserSchemaConstants.HOME,
								  SCIMSchemaDefinitions.SCIMUserSchemaDefinition.ADDRESSES_TYPE);
						if(!homeAddrAttribute.isSubAttributeExist(SCIMConstants.CommonSchemaConstants.TYPE))
							homeAddrAttribute.setSubAttribute(addressType);
								  
						addressFormatted = createSimpleSubAttribute(SCIMConstants.UserSchemaConstants.FORMATTED_ADDRESS,
								  value,SCIMSchemaDefinitions.SCIMUserSchemaDefinition.ADDRESSES_FORMATTED);
						homeAddrAttribute.setSubAttribute(addressFormatted);
						addressAttribute.setAttributeValue(homeAddrAttribute); 
					} 
					
					else{
						subAttributeName = SCIMConstants.UserSchemaConstants.ADDRESSES + "_" + SCIMConstants.UserSchemaConstants.WORK;
						schema = SCIMSchemaDefinitions.SCIMUserSchemaDefinition.ADDRESSES;
						if(workAddrAttribute == null){
							workAddrAttribute = createComplexAttribute(schema,subAttributeName, user, false);
							workAddrAttribute.setMultiValued(false);
						}
						
						addressType = createSimpleSubAttribute(SCIMConstants.CommonSchemaConstants.
								  TYPE, SCIMConstants.UserSchemaConstants.WORK,
								  SCIMSchemaDefinitions.SCIMUserSchemaDefinition.ADDRESSES_TYPE);
						if(!workAddrAttribute.isSubAttributeExist(SCIMConstants.CommonSchemaConstants.TYPE))
							workAddrAttribute.setSubAttribute(addressType);
								  
						if (attribute.getName().matches(LdapIPersonConstants.street)){
							subAttributeName = SCIMConstants.UserSchemaConstants.STREET_ADDRESS;
							schema = SCIMSchemaDefinitions.SCIMUserSchemaDefinition.ADDRESSES_STREET_ADDRESS;
						}
						else if (attribute.getName().matches(LdapIPersonConstants.l)){
							subAttributeName = SCIMConstants.UserSchemaConstants.LOCALITY;
							schema = SCIMSchemaDefinitions.SCIMUserSchemaDefinition.ADDRESSES_LOCALITY;
						}
						else if (attribute.getName().matches(LdapIPersonConstants.st)){
							subAttributeName = SCIMConstants.UserSchemaConstants.REGION;
							schema = SCIMSchemaDefinitions.SCIMUserSchemaDefinition.ADDRESSES_REGION;
						}
						else if (attribute.getName().matches(LdapIPersonConstants.postalCode)){
							subAttributeName = SCIMConstants.UserSchemaConstants.POSTAL_CODE;
							schema = SCIMSchemaDefinitions.SCIMUserSchemaDefinition.ADDRESSES_POSTAL_CODE;
						}
						else if (attribute.getName().matches(LdapIPersonConstants.destinationIndicator)){
							subAttributeName = SCIMConstants.UserSchemaConstants.COUNTRY;
							schema = SCIMSchemaDefinitions.SCIMUserSchemaDefinition.ADDRESSES_COUNTRY;
						}
						workAddressSubAttribute = createSimpleSubAttribute(subAttributeName, value, schema);
						workAddrAttribute.setSubAttribute(workAddressSubAttribute);
						if(!addressAttribute.getAttributeValues().contains(workAddrAttribute))
							addressAttribute.setAttributeValue(workAddrAttribute);
					} 
				}

				if (attribute.getName().matches(LdapIPersonConstants.mail + "|" + LdapIPersonConstants.departmentNumber)) {
					if(emailAttribute == null){
						emailAttribute = createMultiValuedAttribute(SCIMSchemaDefinitions.SCIMUserSchemaDefinition.EMAILS,
								SCIMConstants.UserSchemaConstants.EMAILS, user);
					}
					if (attribute.getName().equals(LdapIPersonConstants.mail)) {
						subAttributeName = SCIMConstants.UserSchemaConstants.EMAILS + "_" +SCIMConstants.UserSchemaConstants.HOME;
						schema = SCIMSchemaDefinitions.SCIMUserSchemaDefinition.EMAILS;
						if(homeEmailAttribute == null){
							homeEmailAttribute = createComplexAttribute(schema,subAttributeName, user, false);
							homeEmailAttribute.setMultiValued(false);
						}
						
						emailType = createSimpleSubAttribute(SCIMConstants.CommonSchemaConstants.TYPE, 
								SCIMConstants.UserSchemaConstants.HOME, SCIMSchemaDefinitions.SCIMUserSchemaDefinition.EMAIL_TYPE);
						homeEmailAttribute.setSubAttribute(emailType);
								  
						emailValue = createSimpleSubAttribute(SCIMConstants.CommonSchemaConstants.VALUE,
										  value,SCIMSchemaDefinitions.SCIMUserSchemaDefinition.EMAIL_VALUE);
						homeEmailAttribute.setSubAttribute(emailValue);
						if(!emailAttribute.getAttributeValues().contains(homeEmailAttribute))
							emailAttribute.setAttributeValue(homeEmailAttribute);
					} 
					else {
						subAttributeName = SCIMConstants.UserSchemaConstants.EMAILS + "_" +SCIMConstants.UserSchemaConstants.WORK;
						schema = SCIMSchemaDefinitions.SCIMUserSchemaDefinition.EMAILS;
						if(workEmailAttribute == null){
							workEmailAttribute = createComplexAttribute(schema,subAttributeName, user, false);
							workEmailAttribute.setMultiValued(false);
						}
						
						emailType = createSimpleSubAttribute(SCIMConstants.CommonSchemaConstants.TYPE, 
								SCIMConstants.UserSchemaConstants.WORK, SCIMSchemaDefinitions.SCIMUserSchemaDefinition.EMAIL_TYPE);
						workEmailAttribute.setSubAttribute(emailType);
								  
						emailValue = createSimpleSubAttribute(SCIMConstants.CommonSchemaConstants.VALUE,
										  value,SCIMSchemaDefinitions.SCIMUserSchemaDefinition.EMAIL_VALUE);
						workEmailAttribute.setSubAttribute(emailValue);
						if(!emailAttribute.getAttributeValues().contains(workEmailAttribute))
							emailAttribute.setAttributeValue(workEmailAttribute);
					} 

				}
			}
			if (nameAttribute != null) {
				user.setAttribute(nameAttribute);
			}
			if(addressAttribute != null){
				user.setAttribute(addressAttribute);
			}
			if(emailAttribute != null){
				user.setAttribute(emailAttribute);
			}

			

		} catch (Exception e) {
			e.printStackTrace();
		}
		return user;
	}

	public static LDAPAttributeSet copyUserToLdap(User user) {
		LDAPAttributeSet attributeSet = new LDAPAttributeSet();
		attributeSet.add(new LDAPAttribute(LdapConstants.objectclass, LdapConstants.userClass));
		Map<String, Attribute> attributeList = user.getAttributeList();
		for (Attribute attribute : attributeList.values()) {
			if (attribute instanceof SimpleAttribute) {
				attributeSet = addSimpleAttribute(null, attributeSet, attribute);

			} else if (attribute instanceof ComplexAttribute) {
				ComplexAttribute complexAttribute = (ComplexAttribute) attribute;
				Map<String, Attribute> subAttributes = complexAttribute.getSubAttributesList();
				for (Attribute subAttribute : subAttributes.values()) {
					if (subAttribute instanceof SimpleAttribute) {
						attributeSet = addSimpleAttribute(null, attributeSet,
								(Attribute) ((SimpleAttribute) subAttribute));

					} else if (subAttribute instanceof MultiValuedAttribute) {
						if (!subAttribute.getType().equals(SCIMDefinitions.DataType.COMPLEX)) {
							attributeSet = addMultiValuedPrimitiveAttribute(
									((MultiValuedAttribute) subAttribute).getAttributePrimitiveValues(),
									subAttribute.getName(), attributeSet);
						} else {
							List<Attribute> subAttributeList = ((MultiValuedAttribute) (subAttribute))
									.getAttributeValues();

							for (Attribute subValue : subAttributeList) {

								ComplexAttribute complexSubAttribute = (ComplexAttribute) subValue;
								Map<String, Attribute> subSubAttributes = complexSubAttribute.getSubAttributesList();

								for (Attribute subSubAttribute : subSubAttributes.values()) {
									if (subSubAttribute instanceof SimpleAttribute) {

										attributeSet = addSimpleAttribute(null, attributeSet,
												(Attribute) ((SimpleAttribute) subSubAttribute));

									} else if (subSubAttribute instanceof MultiValuedAttribute) {
										attributeSet = addMultiValuedPrimitiveAttribute(
												((MultiValuedAttribute) subSubAttribute).getAttributePrimitiveValues(),
												subSubAttribute.getName(), attributeSet);
									}
								}
							}
						}
					} else if (subAttribute instanceof ComplexAttribute) {
						ComplexAttribute complexSubAttribute = (ComplexAttribute) subAttribute;
						Map<String, Attribute> subSubAttributes = complexSubAttribute.getSubAttributesList();

						for (Attribute subSubAttribute : subSubAttributes.values()) {
							if (subSubAttribute instanceof SimpleAttribute) {
								attributeSet = addSimpleAttribute(null, attributeSet,
										(Attribute) ((SimpleAttribute) subSubAttribute));

							} else if (subSubAttribute instanceof MultiValuedAttribute) {
								attributeSet = addMultiValuedPrimitiveAttribute(
										((MultiValuedAttribute) subSubAttribute).getAttributePrimitiveValues(),
										subSubAttribute.getName(), attributeSet);
							}
						}
					}
				}
			} else if (attribute instanceof MultiValuedAttribute) {
				MultiValuedAttribute multiValuedAttribute = (MultiValuedAttribute) attribute;
				if (multiValuedAttribute.getType().equals(SCIMDefinitions.DataType.COMPLEX)) {
					List<Attribute> subAttributeList = multiValuedAttribute.getAttributeValues();
					for (Attribute subAttribute : subAttributeList) {
						ComplexAttribute complexSubAttribute = (ComplexAttribute) subAttribute;
						Map<String, Attribute> subSubAttributes = complexSubAttribute.getSubAttributesList();
						// If address, check for home address START-------
						if (subAttribute.getURI().equals(UserSchemaConstants.ADDRESSES_URI)) {
							String value = null;
							boolean isHome = false;
							for (Attribute subSubAttribute : subSubAttributes.values()) {
								SimpleAttribute simpleAttribute = (SimpleAttribute) subSubAttribute;
								if (subSubAttribute.getName().equals("type")) {
									// Check if type is "home"
									if (simpleAttribute.getValue().equals(UserSchemaConstants.HOME)) {
										isHome = true;
									} else {
										/*if (LdapScimAttrMap.addresses.isSet()) {
											continue;
										}*/
										break;
									}
								} else if (subSubAttribute.getName().equals("formatted")) {
									value = (String) simpleAttribute.getValue();
								}
							}
							if (isHome) {
								attributeSet.add(new LDAPAttribute(LdapIPersonConstants.homePostalAddress, value));
							} else {
								for (Attribute subSubAttribute : subSubAttributes.values()) {
									if (subSubAttribute.getName().equals(LdapScimAttrMap.streetAddress.name())
											||subSubAttribute.getName().equals(LdapScimAttrMap.locality.name())
											||subSubAttribute.getName().equals(LdapScimAttrMap.region.name())
											||subSubAttribute.getName().equals(LdapScimAttrMap.postalCode.name())
											||subSubAttribute.getName().equals(LdapScimAttrMap.country.name())) {
										attributeSet = addSimpleAttribute(null, attributeSet,
												(Attribute) ((SimpleAttribute) subSubAttribute));
									}
								}
							}
							// If address END-------
						} else {
							String parent = getAttributeName(subAttribute);
							for (Attribute subSubAttribute : subSubAttributes.values()) {
								if (subSubAttribute instanceof SimpleAttribute) {
									if (subSubAttribute.getName().equals("value")) {
										attributeSet = addSimpleAttribute(parent, attributeSet,
												(Attribute) ((SimpleAttribute) subSubAttribute));
									} /*
									 * if (UserSchemaConstants.ADDRESSES.equals(
									 * parent)) { parent=parent+"_"+ }
									 */

								} else if (subSubAttribute instanceof MultiValuedAttribute) {
									attributeSet = addMultiValuedPrimitiveAttribute(
											((MultiValuedAttribute) subSubAttribute).getAttributePrimitiveValues(),
											subSubAttribute.getName(), attributeSet);
								}
							}
						}
					}
				} else {
					List<Object> primitiveValueList = multiValuedAttribute.getAttributePrimitiveValues();
					attributeSet = addMultiValuedPrimitiveAttribute(primitiveValueList, multiValuedAttribute.getName(),
							attributeSet);
				}

			}
		}
		return attributeSet;
	}

	private static LDAPAttributeSet addSimpleAttribute(String parentName, LDAPAttributeSet attributeSet,
			Attribute attribute) {
		SimpleAttribute simpleAttribute = (SimpleAttribute) attribute;
		try {
			LdapScimAttrMap name;
			if (parentName != null) {
				name = LdapScimAttrMap.valueOf(parentName);
			} else {
				name = LdapScimAttrMap.valueOf(simpleAttribute.getName());
			}
			if (name != null && attributeSet != null) {
				attributeSet.add(new LDAPAttribute(name.getValue(), simpleAttribute.getValue().toString()));
			}
		} catch (Exception e) {
			System.out.println("Mapping for '" + simpleAttribute.getName() + "' missing!");
		}
		return attributeSet;
	}

	private static LDAPAttributeSet addMultiValuedPrimitiveAttribute(List<Object> attributePrimitiveValues,
			String attributeName, LDAPAttributeSet attributeSet) {
		try {
			LdapScimAttrMap name = LdapScimAttrMap.valueOf(attributeName);
			if (name != null && attributeSet != null) {
				for (Object item : attributePrimitiveValues) {
					attributeSet.add(new LDAPAttribute(attributeName, (String) item));
				}
			}
		} catch (Exception e) {
			System.out.println("Mapping for '" + attributeName + "' missing!");
		}
		return attributeSet;
	}

	private static String getAttributeName(Attribute subAttribute) {
		String parent = null;
		ComplexAttribute complexSubAttribute = (ComplexAttribute) subAttribute;
		Map<String, Attribute> subSubAttributes = complexSubAttribute.getSubAttributesList();

		switch (subAttribute.getURI()) {
		case UserSchemaConstants.EMAILS_URI:
			parent = UserSchemaConstants.EMAILS;
		case UserSchemaConstants.PHONE_NUMBERS_URI:
			parent = (parent == null) ? UserSchemaConstants.PHONE_NUMBERS : parent;
		case UserSchemaConstants.PHOTOS_URI:
			parent = parent == null ? UserSchemaConstants.PHOTOS : parent;
			for (Attribute subSubAttribute : subSubAttributes.values()) {
				if (subSubAttribute.getName().equals("type")) {
					SimpleAttribute simpleAttribute = (SimpleAttribute) subSubAttribute;
					parent = parent + "_" + simpleAttribute.getValue();
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
			// parent = subAttribute.getName();
			break;
		}
		return parent;
	}

	/**
	 * Creates complex SCIM attribute
	 * 
	 * @param schema
	 * @param compAttr
	 * @throws CharonException
	 * @throws BadRequestException
	 */
	private static ComplexAttribute createComplexAttribute(AttributeSchema schema, String compAttrName, User u,
			Boolean addAttrToUser) throws CharonException, BadRequestException {
		ComplexAttribute complexAttribute = (ComplexAttribute) DefaultAttributeFactory.createAttribute(schema,
				new ComplexAttribute(compAttrName));
		if (!u.getAttributeList().containsKey(compAttrName) && addAttrToUser) {
			u.getAttributeList().put(compAttrName, complexAttribute);
		} else if (addAttrToUser) {
			complexAttribute = (ComplexAttribute) u.getAttribute(compAttrName);
		}
		return complexAttribute;

	}

	/**
	 * Creates simple Attribute for SCIM
	 * 
	 * @param createdDate
	 * @throws CharonException
	 * @throws BadRequestException
	 */
	private static SimpleAttribute createSimpleSubAttribute(String subAttributeName, Object value,
			AttributeSchema schema) throws CharonException, BadRequestException {
		// create the created date attribute as defined in schema.
		SimpleAttribute simpleAttribute = new SimpleAttribute(subAttributeName, value);
		simpleAttribute = (SimpleAttribute) DefaultAttributeFactory.createAttribute(schema, simpleAttribute);

		return simpleAttribute;

	}

	/**
	 * Creates Multi-Valued Attribute for SCIM
	 * @param schema
	 * @param mvAttrName
	 * @param user
	 * @return
	 * @throws CharonException
	 * @throws BadRequestException
	 */
	private static MultiValuedAttribute createMultiValuedAttribute(AttributeSchema schema, String mvAttrName, User user)
			throws CharonException, BadRequestException {
		MultiValuedAttribute mvAttribute;

		if (user.getAttributeList().containsKey(mvAttrName)) {
			mvAttribute = (MultiValuedAttribute) user.getAttributeList().get(mvAttrName);
		} else {
			mvAttribute = new MultiValuedAttribute(mvAttrName);
			mvAttribute = (MultiValuedAttribute) DefaultAttributeFactory.createAttribute(schema, mvAttribute);
		}
		return mvAttribute;

	}
	
	private static boolean isPhoneSetInLdap(LDAPAttributeSet attributeSet) {
		if (attributeSet.getAttribute(LdapScimAttrMap.phoneNumbers_home.getValue()) != null)
			return true;
		else if (attributeSet.getAttribute(LdapScimAttrMap.phoneNumbers_mobile.getValue()) != null)
			return true;
		else if (attributeSet.getAttribute(LdapScimAttrMap.phoneNumbers_work.getValue()) != null)
			return true;
		else
			return false;
	}

	private static boolean isIMSSetInLdap(LDAPAttributeSet attributeSet) {
		if (attributeSet.getAttribute(LdapScimAttrMap.ims.getValue()) != null)
			return true;
		else
			return false;
	}


	public static LDAPAttributeSet copyGroupToLdap(Group group) {
		LDAPAttributeSet attributeSet = new LDAPAttributeSet();
		attributeSet.add(new LDAPAttribute(LdapConstants.objectclass, LdapConstants.groupClass));
		try {
			attributeSet.add(new LDAPAttribute(GroupConstants.cn, group.getId()));
			attributeSet.add(new LDAPAttribute(GroupConstants.name, group.getDisplayName()));
			attributeSet.add(new LDAPAttribute(GroupConstants.location, group.getLocation()));
			attributeSet.add(new LDAPAttribute(GroupConstants.createdDate, group.getCreatedDate().toString()));
			attributeSet.add(new LDAPAttribute(GroupConstants.modifiedDate, group.getLastModified().toString()));
			List<Object> members = group.getMembers();
			String[] groupMembers = new String[members.size()];
			int i=0;
			for(Object id: members){
				String uid = (String) id;
				String dn = UserConstants.uid+ "="+ uid+ "," +LdapConstants.userContainer;
				groupMembers[i++] = dn;
			}
			attributeSet.add(new LDAPAttribute(GroupConstants.member, groupMembers));
		} catch (CharonException e) {
			e.printStackTrace();
		}
		return attributeSet;
	}

	public static Group copyLdapToGroup(LDAPEntry entry, List<LDAPEntry> member) throws CharonException, BadRequestException {
		Group group =new Group();
		group.setSchemas();
		LDAPAttributeSet attributeSet = entry.getAttributeSet();
		group.setId(attributeSet.getAttribute(GroupConstants.cn).getStringValue());
		try {
			if (attributeSet.getAttribute(GroupConstants.createdDate) != null) {
				group.setCreatedDate(LdapUtil.parseDate(attributeSet.getAttribute(GroupConstants.createdDate).getStringValue()));
			}
			if (attributeSet.getAttribute(GroupConstants.modifiedDate) != null) {
				group.setLastModified(LdapUtil.parseDate(attributeSet.getAttribute(GroupConstants.modifiedDate).getStringValue()));
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (attributeSet.getAttribute(GroupConstants.location) != null) {
			group.setLocation(attributeSet.getAttribute(GroupConstants.location).getStringValue());
		}
		if (attributeSet.getAttribute(GroupConstants.name) != null) {
			group.setDisplayName(attributeSet.getAttribute(GroupConstants.name).getStringValue());
		}
		if (attributeSet.getAttribute(GroupConstants.member) != null) {
			for(LDAPEntry memEntry :member){
				LDAPAttributeSet userAttrSet = memEntry.getAttributeSet();
				String uid, name;
				if (userAttrSet.getAttribute(LdapScimAttrMap.id.getValue()) != null) {
					uid = userAttrSet.getAttribute(LdapScimAttrMap.id.getValue()).getStringValue();
					if (userAttrSet.getAttribute(LdapScimAttrMap.displayName.getValue()) != null) {
						name = userAttrSet.getAttribute(LdapScimAttrMap.displayName.getValue()).getStringValue();
						group.setMember(uid, name);
					}
				}
			}
		}
		return group;
	}

	/**
	 * Parses LDAP date "EEE MMM dd HH:mm:ss z yyyy" to Scim date
	 * @param dateStr
	 * @return
	 * @throws CharonException
	 * @throws ParseException
	 */
	public static Date parseDate(String dateStr) throws CharonException, ParseException{
		Date date;
		SimpleDateFormat ldapFormat = new SimpleDateFormat(LdapConstants.LDAP_DATE_FORMAT);
		SimpleDateFormat scimFormat = new SimpleDateFormat(LdapConstants.SCIM_DATE_FORMAT);
		if (dateStr != null) {
			date = ldapFormat.parse(dateStr);
			dateStr = scimFormat.format(date);
			date = AttributeUtil.parseDateTime(dateStr);
			return date;
		}
		return null;
	}

	public static Object[] sort(LDAPSearchResults searchResults, String sortBy, 
			String sortOrder, int startIndex, int count ) throws LDAPException, BadRequestException {
		
		//sortedResults will sort the entries according to the natural ordering of LDAPEntry (by distiguished name).
		TreeSet sortedResults = new TreeSet();
		while (searchResults.hasMore()) {
			try {
				sortedResults.add(searchResults.next());
			} catch (LDAPException e) {
				System.out.println("Error: " + e.toString());
				// Exception is thrown, go for next entry
				continue;
			}
		}

		//String namesToSortBy[] = { "sn", "uid", "cn" };
		//boolean sortAscending[] = { true, false, true };
		String namesToSortBy[] = { sortBy };
		boolean sOrder = sortOrder.equals("descending")?false:true;
		boolean sortAscending[] = { sOrder };
		LDAPCompareAttrNames myComparator = new LDAPCompareAttrNames(namesToSortBy, sortAscending);
		Object sortedSpecial[] = sortedResults.toArray();
		Arrays.sort(sortedSpecial, myComparator);

		if(startIndex>1 || count>0) {
			if(sortedSpecial.length<startIndex+count-1){
				throw new BadRequestException("invalidValue");
			}
			Object paginatedSpecial[] = Arrays.copyOfRange(sortedSpecial, startIndex-1, startIndex+count-1);
			return paginatedSpecial;
		}

		return sortedSpecial;
	}
}
