/**
 * 
 */
package org.wso2.charon3.utils.ldapmanager;

import org.wso2.charon3.core.schema.SCIMConstants.UserSchemaConstants;

/**
 * @author AkshathaKadri
 * 
 * @author ReenaHegde
 */
public enum LdapScimAttrMap {
	//id("uid"), externalId("x500UniqueIdentifier"), userName("mail"), password("userPassword"),givenName("cn"),familyName("sn"),nickName("givenName");
	externalId(LdapIPersonConstants.x500UniqueIdentifier), 
	id(LdapIPersonConstants.uid), 
	userName(LdapIPersonConstants.cn), 
	familyName(LdapIPersonConstants.sn), 
	password(LdapIPersonConstants.userPassword), 
	profileUrl(LdapIPersonConstants.labeledUri),
	userType(LdapIPersonConstants.employeeType), 
	title(LdapIPersonConstants.title), 
	employeeNumber(LdapIPersonConstants.employeeNumber), 
	costCenter(LdapIPersonConstants.costCenter), 
	active(LdapIPersonConstants.loginDisabled),
	
	displayName(LdapIPersonConstants.displayName), 
	nickName(LdapIPersonConstants.postOfficeBox),
	formatted(LdapIPersonConstants.fullName), 
	givenName(LdapIPersonConstants.givenName), 
	middleName(LdapIPersonConstants.initials),
	honorificPrefix(LdapIPersonConstants.pager), 
	honorificSuffix(LdapIPersonConstants.description),
	
	phoneNumbers_work(LdapIPersonConstants.telephoneNumber), 
	phoneNumbers_home(LdapIPersonConstants.homePhone),
	phoneNumbers_mobile(LdapIPersonConstants.facsimileTelephoneNumber),
	emails_work(LdapIPersonConstants.mail), 
	emails_home(LdapIPersonConstants.departmentNumber),
	photos_photo(LdapIPersonConstants.mailstop), 
	photos_thumbnail(LdapIPersonConstants.personalTitle), 
	
	
	resourceType(LdapIPersonConstants.carLicense), 
	created(LdapIPersonConstants.businessCategory) ,
	version(LdapIPersonConstants.revision),
	lastModified(LdapIPersonConstants.company),
	
	addresses_home(LdapIPersonConstants.homePostalAddress),
	addresses(UserSchemaConstants.ADDRESSES),
	streetAddress(LdapIPersonConstants.street),
	locality(LdapIPersonConstants.l), 
	region(LdapIPersonConstants.st), 
	postalCode(LdapIPersonConstants.postalCode), 
	country(LdapIPersonConstants.destinationIndicator), 
	groups(LdapIPersonConstants.groupMembership),
	
	//manager(LdapIPersonConstants.manager),
	preferredLanguage(LdapIPersonConstants.preferredLanguage), 
	locale(LdapIPersonConstants.physicalDeliveryOfficeName),///////////////////???????????????????????????????????????
	organization(LdapIPersonConstants.o), 
	division(LdapIPersonConstants.ou), 
	department(LdapIPersonConstants.organizationalUnitName),
	timezone(LdapIPersonConstants.Timezone),
	ims(LdapIPersonConstants.instantMessagingID), 
	x509Certificates(LdapIPersonConstants.userCertificate), 
	location(LdapIPersonConstants.audio);
	
	private final String value;
	private boolean isSet;
	LdapScimAttrMap(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
	public boolean isSet() {
		return isSet;
	}
	public void setSet(boolean isSet) {
		this.isSet = isSet;
	}
	
	public static LdapScimAttrMap getByLdapAttrName(String ldapAttrName) {
		LdapScimAttrMap returnValue = null;
        for (LdapScimAttrMap attr : LdapScimAttrMap.values()) {
            if (attr.getValue().equalsIgnoreCase(ldapAttrName)) {
            	returnValue = attr;
            }
           
        }
        return returnValue;
    }
}
