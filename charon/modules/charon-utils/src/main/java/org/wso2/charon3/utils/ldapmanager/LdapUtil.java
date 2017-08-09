package org.wso2.charon3.utils.ldapmanager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
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
		try {
			LDAPAttributeSet attributeSet = entry.getAttributeSet();

			ComplexAttribute nameAttribute = createComplexAttribute(SCIMSchemaDefinitions.SCIMUserSchemaDefinition.NAME,
					SCIMConstants.UserSchemaConstants.NAME, user, true);
			if (attributeSet.getAttribute(LdapScimAttrMap.formatted.getValue()) != null) {
				SimpleAttribute formattedNameAttribute = createSimpleSubAttribute(
						SCIMConstants.UserSchemaConstants.FORMATTED_NAME,
						attributeSet.getAttribute(LdapScimAttrMap.formatted.getValue()).getStringValue(),
						SCIMSchemaDefinitions.SCIMUserSchemaDefinition.FORMATTED);
				nameAttribute.setSubAttribute(formattedNameAttribute);
			}

			if (attributeSet.getAttribute(LdapScimAttrMap.familyName.getValue()) != null) {
				SimpleAttribute familyNameAttribute = createSimpleSubAttribute(
						SCIMConstants.UserSchemaConstants.FAMILY_NAME,
						attributeSet.getAttribute(LdapScimAttrMap.familyName.getValue()).getStringValue(),
						SCIMSchemaDefinitions.SCIMUserSchemaDefinition.FAMILY_NAME);
				nameAttribute.setSubAttribute(familyNameAttribute);
			}

			if (attributeSet.getAttribute(LdapScimAttrMap.givenName.getValue()) != null) {
				SimpleAttribute givenNameAttribute = createSimpleSubAttribute(
						SCIMConstants.UserSchemaConstants.GIVEN_NAME,
						attributeSet.getAttribute(LdapScimAttrMap.givenName.getValue()).getStringValue(),
						SCIMSchemaDefinitions.SCIMUserSchemaDefinition.GIVEN_NAME);
				nameAttribute.setSubAttribute(givenNameAttribute);
			}

			if (attributeSet.getAttribute(LdapScimAttrMap.middleName.getValue()) != null) {
				SimpleAttribute middleNameAttribute = createSimpleSubAttribute(
						SCIMConstants.UserSchemaConstants.MIDDLE_NAME,
						attributeSet.getAttribute(LdapScimAttrMap.middleName.getValue()).getStringValue(),
						SCIMSchemaDefinitions.SCIMUserSchemaDefinition.MIDDLE_NAME);
				nameAttribute.setSubAttribute(middleNameAttribute);
			}

			if (attributeSet.getAttribute(LdapScimAttrMap.honorificPrefix.getValue()) != null) {
				SimpleAttribute honoroficPrefixAttribute = createSimpleSubAttribute(
						SCIMConstants.UserSchemaConstants.HONORIFIC_PREFIX,
						attributeSet.getAttribute(LdapScimAttrMap.honorificPrefix.getValue()).getStringValue(),
						SCIMSchemaDefinitions.SCIMUserSchemaDefinition.HONORIFIC_PREFIX);
				nameAttribute.setSubAttribute(honoroficPrefixAttribute);
			}

			if (attributeSet.getAttribute(LdapScimAttrMap.honorificSuffix.getValue()) != null) {
				SimpleAttribute honoroficSuffixAttribute = createSimpleSubAttribute(
						SCIMConstants.UserSchemaConstants.HONORIFIC_SUFFIX,
						attributeSet.getAttribute(LdapScimAttrMap.honorificSuffix.getValue()).getStringValue(),
						SCIMSchemaDefinitions.SCIMUserSchemaDefinition.HONORIFIC_SUFFIX);
				nameAttribute.setSubAttribute(honoroficSuffixAttribute);
			}

			user.setAttribute(nameAttribute);

			if (isAddressSetInLdap(attributeSet)) {
				MultiValuedAttribute addressAttribute = createMultiValuedAttribute(
						SCIMSchemaDefinitions.SCIMUserSchemaDefinition.ADDRESSES,
						SCIMConstants.UserSchemaConstants.ADDRESSES, user);
				if (attributeSet.getAttribute(LdapScimAttrMap.addresses_home.getValue()) != null) {
					String homeAddrcomplexAttributeName = SCIMConstants.UserSchemaConstants.ADDRESSES + "_"
							+ SCIMConstants.UserSchemaConstants.HOME;

					ComplexAttribute homeAddrcomplexAttribute = createComplexAttribute(
							SCIMSchemaDefinitions.SCIMUserSchemaDefinition.ADDRESSES, homeAddrcomplexAttributeName,
							user, false);
					homeAddrcomplexAttribute.setMultiValued(false);

					SimpleAttribute addressType = createSimpleSubAttribute(SCIMConstants.CommonSchemaConstants.TYPE,
							SCIMConstants.UserSchemaConstants.HOME,
							SCIMSchemaDefinitions.SCIMUserSchemaDefinition.ADDRESSES_TYPE);
					homeAddrcomplexAttribute.setSubAttribute(addressType);

					SimpleAttribute addressFormatted = createSimpleSubAttribute(
							SCIMConstants.UserSchemaConstants.FORMATTED_ADDRESS,
							attributeSet.getAttribute(LdapScimAttrMap.addresses_home.getValue()).getStringValue(),
							SCIMSchemaDefinitions.SCIMUserSchemaDefinition.ADDRESSES_FORMATTED);
					homeAddrcomplexAttribute.setSubAttribute(addressFormatted);
					addressAttribute.setAttributeValue(homeAddrcomplexAttribute);
				}

				if (attributeSet.getAttribute(LdapScimAttrMap.streetAddress.getValue()) != null
						|| attributeSet.getAttribute(LdapScimAttrMap.locality.getValue()) != null
						|| attributeSet.getAttribute(LdapScimAttrMap.region.getValue()) != null
						|| attributeSet.getAttribute(LdapScimAttrMap.postalCode.getValue()) != null
						|| attributeSet.getAttribute(LdapScimAttrMap.country.getValue()) != null) {

					String workAddrcomplexAttributeName = SCIMConstants.UserSchemaConstants.ADDRESSES + "_"
							+ SCIMConstants.UserSchemaConstants.WORK;
					ComplexAttribute workAddrcomplexAttribute = createComplexAttribute(
							SCIMSchemaDefinitions.SCIMUserSchemaDefinition.ADDRESSES, workAddrcomplexAttributeName,
							user, false);
					workAddrcomplexAttribute.setMultiValued(false);

					SimpleAttribute addressType = createSimpleSubAttribute(SCIMConstants.CommonSchemaConstants.TYPE,
							SCIMConstants.UserSchemaConstants.WORK,
							SCIMSchemaDefinitions.SCIMUserSchemaDefinition.ADDRESSES_TYPE);
					workAddrcomplexAttribute.setSubAttribute(addressType);

					if (attributeSet.getAttribute(LdapScimAttrMap.streetAddress.getValue()) != null) {
						SimpleAttribute workStAddress = createSimpleSubAttribute(
								SCIMConstants.UserSchemaConstants.STREET_ADDRESS,
								attributeSet.getAttribute(LdapScimAttrMap.streetAddress.getValue()).getStringValue(),
								SCIMSchemaDefinitions.SCIMUserSchemaDefinition.ADDRESSES_STREET_ADDRESS);
						workAddrcomplexAttribute.setSubAttribute(workStAddress);
					}

					if (attributeSet.getAttribute(LdapScimAttrMap.locality.getValue()) != null) {
						SimpleAttribute workLocality = createSimpleSubAttribute(
								SCIMConstants.UserSchemaConstants.LOCALITY,
								attributeSet.getAttribute(LdapScimAttrMap.locality.getValue()).getStringValue(),
								SCIMSchemaDefinitions.SCIMUserSchemaDefinition.ADDRESSES_LOCALITY);
						workAddrcomplexAttribute.setSubAttribute(workLocality);
					}

					if (attributeSet.getAttribute(LdapScimAttrMap.region.getValue()) != null) {
						SimpleAttribute workRegion = createSimpleSubAttribute(SCIMConstants.UserSchemaConstants.REGION,
								attributeSet.getAttribute(LdapScimAttrMap.region.getValue()).getStringValue(),
								SCIMSchemaDefinitions.SCIMUserSchemaDefinition.ADDRESSES_REGION);
						workAddrcomplexAttribute.setSubAttribute(workRegion);
					}

					if (attributeSet.getAttribute(LdapScimAttrMap.postalCode.getValue()) != null) {
						SimpleAttribute workPostalCode = createSimpleSubAttribute(
								SCIMConstants.UserSchemaConstants.POSTAL_CODE,
								attributeSet.getAttribute(LdapScimAttrMap.postalCode.getValue()).getStringValue(),
								SCIMSchemaDefinitions.SCIMUserSchemaDefinition.ADDRESSES_POSTAL_CODE);
						workAddrcomplexAttribute.setSubAttribute(workPostalCode);
					}

					if (attributeSet.getAttribute(LdapScimAttrMap.country.getValue()) != null) {
						SimpleAttribute workCountry = createSimpleSubAttribute(
								SCIMConstants.UserSchemaConstants.COUNTRY,
								attributeSet.getAttribute(LdapScimAttrMap.country.getValue()).getStringValue(),
								SCIMSchemaDefinitions.SCIMUserSchemaDefinition.ADDRESSES_COUNTRY);
						workAddrcomplexAttribute.setSubAttribute(workCountry);
					}

					addressAttribute.setAttributeValue(workAddrcomplexAttribute);
				}
				user.setAttribute(addressAttribute);

			}
			if(isEmailSetInLdap(attributeSet)){
				SimpleAttribute emailType;
				SimpleAttribute emailValue;
				MultiValuedAttribute emailAttribute = createMultiValuedAttribute(
						SCIMSchemaDefinitions.SCIMUserSchemaDefinition.EMAILS,
						SCIMConstants.UserSchemaConstants.EMAILS, user);
				if (attributeSet.getAttribute(LdapScimAttrMap.emails_home.getValue()) != null) {
					String homeEmailcomplexAttributeName = SCIMConstants.UserSchemaConstants.EMAILS + "_"
							+ SCIMConstants.UserSchemaConstants.HOME;
					ComplexAttribute homeEmailcomplexAttribute = createComplexAttribute(
							SCIMSchemaDefinitions.SCIMUserSchemaDefinition.EMAILS, homeEmailcomplexAttributeName,
							user, false);
					homeEmailcomplexAttribute.setMultiValued(false);

					emailType = createSimpleSubAttribute(SCIMConstants.CommonSchemaConstants.TYPE,
							SCIMConstants.UserSchemaConstants.HOME,
							SCIMSchemaDefinitions.SCIMUserSchemaDefinition.EMAIL_TYPE);
					homeEmailcomplexAttribute.setSubAttribute(emailType);

					emailValue = createSimpleSubAttribute(
							SCIMConstants.CommonSchemaConstants.VALUE,
							attributeSet.getAttribute(LdapScimAttrMap.emails_home.getValue()).getStringValue(),
							SCIMSchemaDefinitions.SCIMUserSchemaDefinition.EMAIL_VALUE);
					homeEmailcomplexAttribute.setSubAttribute(emailValue);
					emailAttribute.setAttributeValue(homeEmailcomplexAttribute);
				}

				if (attributeSet.getAttribute(LdapScimAttrMap.emails_work.getValue()) != null) {
					String workEmailcomplexAttributeName = SCIMConstants.UserSchemaConstants.EMAILS + "_"
							+ SCIMConstants.UserSchemaConstants.WORK;
					ComplexAttribute workEmailcomplexAttribute = createComplexAttribute(
							SCIMSchemaDefinitions.SCIMUserSchemaDefinition.EMAILS, workEmailcomplexAttributeName,
							user, false);
					workEmailcomplexAttribute.setMultiValued(false);

					emailType = createSimpleSubAttribute(SCIMConstants.CommonSchemaConstants.TYPE,
							SCIMConstants.UserSchemaConstants.WORK,
							SCIMSchemaDefinitions.SCIMUserSchemaDefinition.EMAIL_TYPE);
					workEmailcomplexAttribute.setSubAttribute(emailType);

					emailValue = createSimpleSubAttribute(
							SCIMConstants.CommonSchemaConstants.VALUE,
							attributeSet.getAttribute(LdapScimAttrMap.emails_work.getValue()).getStringValue(),
							SCIMSchemaDefinitions.SCIMUserSchemaDefinition.EMAIL_VALUE);
					workEmailcomplexAttribute.setSubAttribute(emailValue);
					emailAttribute.setAttributeValue(workEmailcomplexAttribute);
				}
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
			// groupsAttribute.setAttributeValue(groupPropertiesAttribute);
			mvAttribute = (MultiValuedAttribute) DefaultAttributeFactory.createAttribute(schema, mvAttribute);
			// this.attributeList.put(SCIMConstants.UserSchemaConstants.GROUPS,
			// groupsAttribute);
		}
		return mvAttribute;

	}

	private static boolean isNameSetInLdap(LDAPAttributeSet attributeSet) {
		if (attributeSet.getAttribute(LdapScimAttrMap.formatted.getValue()) != null)
			return true;
		else if (attributeSet.getAttribute(LdapScimAttrMap.familyName.getValue()) != null)
			return true;
		else if (attributeSet.getAttribute(LdapScimAttrMap.givenName.getValue()) != null)
			return true;
		else if (attributeSet.getAttribute(LdapScimAttrMap.middleName.getValue()) != null)
			return true;
		else if (attributeSet.getAttribute(LdapScimAttrMap.honorificPrefix.getValue()) != null)
			return true;
		else if (attributeSet.getAttribute(LdapScimAttrMap.honorificSuffix.getValue()) != null)
			return true;
		else
			return false;
	}

	private static boolean isAddressSetInLdap(LDAPAttributeSet attributeSet) {
		if (attributeSet.getAttribute(LdapScimAttrMap.addresses.getValue()) != null)
			return true;
		else if (attributeSet.getAttribute(LdapScimAttrMap.addresses_home.getValue()) != null)
			return true;
		else if (attributeSet.getAttribute(LdapScimAttrMap.streetAddress.getValue()) != null)
			return true;
		else if (attributeSet.getAttribute(LdapScimAttrMap.locality.getValue()) != null)
			return true;
		else if (attributeSet.getAttribute(LdapScimAttrMap.region.getValue()) != null)
			return true;
		else if (attributeSet.getAttribute(LdapScimAttrMap.postalCode.getValue()) != null)
			return true;
		else if (attributeSet.getAttribute(LdapScimAttrMap.country.getValue()) != null)
			return true;
		else
			return false;
	}

	private static boolean isEmailSetInLdap(LDAPAttributeSet attributeSet) {
		if (attributeSet.getAttribute(LdapScimAttrMap.emails_home.getValue()) != null)
			return true;
		else if (attributeSet.getAttribute(LdapScimAttrMap.emails_work.getValue()) != null)
			return true;
		else
			return false;
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
