package org.wso2.charon3.utils.ldapmanager;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPCompareAttrNames;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;
public class ClientSideSort{
	public static void main(String[] args)
	{
		String searchBase = "ou=users,o=people";
		String searchFilter = "(displayName=*Jensen)";
		LDAPConnection lc = LdapConnectUtil.getConnection(false);

		try {
			LDAPSearchResults searchResults =
					lc.search(searchBase,LDAPConnection.SCOPE_ONE,searchFilter,new String[] { "cn", "uid", "sn" }, false); // return attrs and values

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
			// print the sorted results
			System.out.println("\n****************************\nSearch results sorted by DN:\n****************************");
			Iterator i = sortedResults.iterator();
			while (i.hasNext()) {
				printEntry((LDAPEntry) i.next());
			}

			/* resort the results an an array using a specific comparator */
			String namesToSortBy[] = { "sn", "uid", "cn" };
			boolean sortAscending[] = { true, false, true };
			LDAPCompareAttrNames myComparator = new LDAPCompareAttrNames(namesToSortBy, sortAscending);
			Object sortedSpecial[] = sortedResults.toArray();
			Arrays.sort(sortedSpecial, myComparator);

			// print the re-sorted results
			System.out.println("\n*****************************************************\n" +
					"Search results sorted by sn, uid(Descending), and cn:\n*****************************************************");

			for (int j = 0; j < sortedSpecial.length; j++) {
				printEntry((LDAPEntry) sortedSpecial[j]);
			}
			lc.disconnect();

		} catch (LDAPException e) {
			System.out.println("Error: " + e.toString());
		} /*catch (UnsupportedEncodingException e) {
			System.out.println("Error: " + e.toString());
		}*/
		System.exit(0);
	}

	/**
	 * 
	 * Prints the DN and attributes in an LDAPEntry to System.out.
	 * 
	 * This method used TreeSet to sort the attributes by name.
	 * 
	 */

	public static void printEntry(LDAPEntry entry) {

		System.out.println(entry.getDN());
		System.out.println("\tAttributes: ");
		LDAPAttributeSet attributeSet = entry.getAttributeSet();
		Set sortedAttributes = new TreeSet(attributeSet);
		Iterator allAttributes = sortedAttributes.iterator();
		while (allAttributes.hasNext()) {
			LDAPAttribute attribute =(LDAPAttribute) allAttributes.next();
			String attributeName = attribute.getName();
			System.out.println("\t\t" + attributeName);
			Enumeration allValues = attribute.getStringValues();
			if (allValues != null) {
				while (allValues.hasMoreElements()) {
					String Value = (String) allValues.nextElement();
					System.out.println("\t\t\t" + Value);
				}
			}
		}
		return;
	}
}
