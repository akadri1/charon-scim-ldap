/*
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.charon3.utils.ldapmanager;


import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.wso2.charon3.core.exceptions.BadRequestException;
import org.wso2.charon3.core.exceptions.CharonException;
import org.wso2.charon3.core.exceptions.ConflictException;
import org.wso2.charon3.core.exceptions.NotFoundException;
import org.wso2.charon3.core.exceptions.NotImplementedException;
import org.wso2.charon3.core.extensions.UserManager;
import org.wso2.charon3.core.objects.Group;
import org.wso2.charon3.core.objects.User;
import org.wso2.charon3.core.schema.SCIMConstants;
import org.wso2.charon3.core.utils.CopyUtil;
import org.wso2.charon3.core.utils.codeutils.ExpressionNode;
import org.wso2.charon3.core.utils.codeutils.Node;
import org.wso2.charon3.core.utils.codeutils.OperationNode;
import org.wso2.charon3.core.utils.codeutils.SearchRequest;
import org.wso2.charon3.utils.ldapmanager.LdapConstants.GroupConstants;

import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPModification;
import com.novell.ldap.LDAPSearchResults;

/**
 * This is a ldap user store.
 */
public class LdapManager implements UserManager {
	//private static final Logger logger = LoggerFactory.getLogger(LdapManager.class);
	//in memory user manager stores users
	//ConcurrentHashMap<String, User> inMemoryUserList = new ConcurrentHashMap<String, User>();
	//ConcurrentHashMap<String, Group> inMemoryGroupList = new ConcurrentHashMap<String, Group>();

	//private static final String SPACE = " ";
	private static final String LEFT_BRACKET = "(";
	private static final String RIGHT_BRACKET = ")";
	private static final String COLON = ":";

	@Override
	public User createUser(User user, Map<String, Boolean> map)
			throws CharonException, ConflictException, BadRequestException {
		User user1 = null;
		LDAPConnection lc = LdapConnectUtil.getConnection(false);
		LDAPAttributeSet attributeSet = LdapUtil.copyUserToLdap(user);

		String id = attributeSet.getAttribute(LdapScimAttrMap.id.getValue()).getStringValue();
		String dn = "uid="+id+",ou=users,o=people";
		LDAPEntry entry = new LDAPEntry(dn, attributeSet);

		try {
			user1 = (User) CopyUtil.deepCopy(user); 
			lc.add(entry);
			lc.disconnect();
		} catch (LDAPException e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
			throw new ConflictException("User with the id : " + user.getId() + "already exists");
		}
		return user1;
	}

	@Override
	public User getUser(String id, Map<String, Boolean> map)
			throws CharonException, BadRequestException, NotFoundException {

		//Added by Reena Hegde

		LDAPConnection lc = LdapConnectUtil.getConnection(false);
		List<LDAPEntry> userList = new ArrayList<>();
		User user = new User();

		try {
			LDAPEntry searchResult = lc.read("uid="+id+",ou=users,o=people");
			user = LdapUtil.convertLdapToUser(searchResult);
			user.setSchemas();
			lc.disconnect();
		} catch (LDAPException e) {
			throw new NotFoundException("No user with the id : " + id);
		}

		return user;
	}

	@Override
	public void deleteUser(String id)
			throws NotFoundException, CharonException, NotImplementedException, BadRequestException {
		LDAPConnection lc = LdapConnectUtil.getConnection(false);
		try {
			System.out.println("Ldap connection successful, ID: "+id);
			String dn = "uid="+id+",ou=users,o=people";
			lc.delete(dn);
			System.out.print(dn+" Deleted");
			// disconnect with the server
			lc.disconnect();
			return;
		}
		catch (LDAPException e) {
			System.out.println("Error:  " + e.toString());
			throw new NotFoundException("No user with the id : " + id);
		} 
	}

	@Override
	public List<Object> listUsersWithGET(Node rootNode, int startIndex, int count, String sortBy,
			String sortOrder, Map<String, Boolean> requiredAttributes)
					throws CharonException, NotImplementedException, BadRequestException {
		if (sortBy != null || sortOrder != null) {
			throw new NotImplementedException("Sorting is not supported");
		}  else if (startIndex != 1) {
			throw new NotImplementedException("Pagination is not supported");
		} else if (rootNode != null) {
			throw new NotImplementedException("Filtering is not supported");
		} else {
			return listUsers(requiredAttributes);
		}
	}

	private List<Object> listUsers(Map<String, Boolean> requiredAttributes) {
		/*List<Object> userList = new ArrayList<>();
		userList.add(0);
		//first item should contain the number of total results
		for (Map.Entry<String, User> entry : inMemoryUserList.entrySet()) {
			userList.add(entry.getValue());
		}
		userList.set(0, userList.size() - 1);
		try {
			return (List<Object>) CopyUtil.deepCopy(userList);
		} catch (CharonException e) {
			logger.error("Error in listing users");
			return  null;
		}*/

		//Added by Reena Hegde

		List<Object> userList = new ArrayList<>();
		User u = new User();
		try {

			LDAPConnection lc = LdapConnectUtil.getConnection(false);

			LDAPSearchResults searchResults =

					lc.search("ou=users,o=people", // container to search

							LDAPConnection.SCOPE_ONE, // search scope

							"cn=*", // search filter

							null, // "1.1" returns entry name only

							false); // no attributes are returned
			userList.add(0);
			while (searchResults.hasMore()) {

				LDAPEntry nextEntry = null;

				try {

					nextEntry = searchResults.next();
					u = LdapUtil.convertLdapToUserForList(nextEntry);
					userList.add(u);

				}

				catch (LDAPException e) {

					System.out.println("Error: " + e.toString());

					// Exception is thrown, go for next entry

					continue;

				}
				userList.set(0, userList.size() - 1);
				System.out.println("\n" + nextEntry.getDN());

			}

			lc.disconnect();

			return (List<Object>) CopyUtil.deepCopy(userList);
		} catch (CharonException e) {

			return null;
		} catch (LDAPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}

	@Override
	public List<Object> listUsersWithPost(SearchRequest searchRequest, Map<String, Boolean> requiredAttributes)
			throws CharonException, NotImplementedException, BadRequestException {
		return listUsersWithGET(searchRequest.getFilter(), searchRequest.getStartIndex(), searchRequest.getCount(),
				searchRequest.getSortBy(), searchRequest.getSortOder(), requiredAttributes);
	}

	//@Override
	//Not used!
	public User updateUser1(User user, Map<String, Boolean> map)
			throws NotImplementedException, CharonException, BadRequestException, NotFoundException {
		String id = user.getId();
		if (id == null) {
			throw new NotFoundException("No user with the id : " + user.getId());
		}
		LDAPConnection lc = LdapConnectUtil.getConnection(false);
		String dn = "uid="+id+",ou=users,o=people";
		try {
			/*//Delete previous attr and add new ones
				LDAPEntry searchResult = lc.read(dn);		
				User dbUser = new User(); //(User)searchResult;

				List<LDAPModification> modList = LdapUpdateHelper.getModifications(dbUser, LDAPModification.DELETE);
				modList.addAll(LdapUpdateHelper.getModifications(user, LDAPModification.ADD));
				modList.add(new LDAPModification(0, new LDAPAttribute(LdapIPersonConstants.uid,user.getId())));
			 */
			List<LDAPModification> modList = LdapUpdateHelper.getModifications(user, LDAPModification.REPLACE);
			LDAPModification[] mods = new LDAPModification[modList.size()];
			mods = (LDAPModification[]) modList.toArray(mods);

			lc.modify(dn, mods);
			lc.disconnect();
		} catch (LDAPException e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		return (User) CopyUtil.deepCopy(user);
	}

	@Override
	public User updateUser(User user, Map<String, Boolean> map)
			throws NotImplementedException, CharonException, BadRequestException, NotFoundException {
		String id = user.getId();
		if (id == null) {
			throw new NotFoundException("No user with the id : " + user.getId());
		}
		try {
			deleteUser(id);
			createUser(user, map);
		} catch (ConflictException e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		return (User) CopyUtil.deepCopy(user);
	}

	@Override
	public User getMe(String s, Map<String, Boolean> map)
			throws CharonException, BadRequestException, NotFoundException {
		return getUser(s, map);
	}

	@Override
	public User createMe(User user, Map<String, Boolean> map)
			throws CharonException, ConflictException, BadRequestException {
		return createUser(user, map);
	}

	@Override
	public void deleteMe(String s)
			throws NotFoundException, CharonException, NotImplementedException, BadRequestException {
		deleteUser(s);
	}

	@Override
	public User updateMe(User user, Map<String, Boolean> map)
			throws NotImplementedException, CharonException, BadRequestException, NotFoundException {
		return updateUser(user, map);
	}

	@Override
	public Group createGroup(Group group, Map<String, Boolean> map)
			throws CharonException, ConflictException, NotImplementedException, BadRequestException {

		LDAPConnection lc = LdapConnectUtil.getConnection(false);
		LDAPAttributeSet attributeSet = LdapUtil.copyGroupToLdap(group);

		String cn = attributeSet.getAttribute(GroupConstants.cn).getStringValue();
		String dn = GroupConstants.cn+"="+cn+","+LdapConstants.groupContainer;
		LDAPEntry entry = new LDAPEntry(dn, attributeSet);

		try {
			lc.add(entry);
			lc.disconnect();
		} catch (LDAPException e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
			throw new ConflictException("Group with the id : " + cn + "already exists");
		}
		try {
			group = getGroup(cn, map);
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			throw new CharonException(e.getMessage());
		}
		return (Group) CopyUtil.deepCopy(group);
	}

	@Override
	public Group getGroup(String id, Map<String, Boolean> map)
			throws NotImplementedException, BadRequestException, CharonException, NotFoundException {
		Group group = null;
		try {
			LDAPConnection lc = LdapConnectUtil.getConnection(false);
			LDAPEntry entry =lc.read(GroupConstants.cn+"="+id+","+LdapConstants.groupContainer); 
			List<LDAPEntry> memEntry = new ArrayList<>();
			LDAPAttributeSet attributeSet = entry.getAttributeSet();
			if (attributeSet.getAttribute(GroupConstants.member) != null) {
				String[] memIds = attributeSet.getAttribute(GroupConstants.member).getStringValueArray();
				for(String dn :memIds){
					try{
						LDAPEntry userEntry = lc.read(dn);
						memEntry.add(userEntry);
					}catch (Exception e) {
						// TODO: handle exception
						e.printStackTrace();
					}
				}
			}
			group = LdapUtil.copyLdapToGroup(entry, memEntry);
		} catch(Exception e) {
			e.printStackTrace();
		}
		if(group ==null) {
			throw new NotFoundException("No group with the id : " + id);
		} else {
			return (Group) CopyUtil.deepCopy(group);
		}
	}

	@Override
	public void deleteGroup(String id)
			throws NotFoundException, CharonException, NotImplementedException, BadRequestException {
		LDAPConnection lc = LdapConnectUtil.getConnection(false);
		try {
			String dn = GroupConstants.cn+"="+id+","+LdapConstants.groupContainer;
			lc.delete(dn);
			System.out.print(dn+" Deleted");
			lc.disconnect();
			return;
		}
		catch (LDAPException e) {
			System.out.println("Error:  " + e.toString());
			throw new NotFoundException("No Group with the id : " + id);
		} 

	}

	@Override
	public List<Object> listGroupsWithGET(Node rootNode, int startIndex, int count, String sortBy,
			String sortOrder, Map<String, Boolean> requiredAttributes)
					throws CharonException, NotImplementedException, BadRequestException {
		String search = null;
		/*if (startIndex != 1) {
			throw new NotImplementedException("Pagination is not supported");
		} else */if (rootNode != null) {
			search = getLdapSearch(rootNode, search, SCIMConstants.GROUP_CORE_SCHEMA_URI);
		}
		return listGroups(requiredAttributes, search, sortBy, sortOrder, startIndex, count);
	}

	private String getLdapSearch(Node node, String search, String schema) {

		if(node == null)  {
			return search;
		}

		search = getLdapSearch(node.getLeftNode(), search, schema);
		search = getLdapSearch(node.getRightNode(), search, schema);
		//Terminal node
		if(node.getLeftNode() == null && node.getRightNode() == null)  {
			ExpressionNode expNode = (ExpressionNode) node;
			LdapScimOpMap op = LdapScimOpMap.valueOf(expNode.getOperation());
			String attr = expNode.getAttributeValue();
			attr = LdapScimGroupMap.valueOf(attr.split(schema+COLON)[1]).getValue();

			//TODO: construct DN
			//if(attr == LdapScimGroupMap.members.getValue()) {}

			String value = expNode.getValue();
			search += LEFT_BRACKET+attr+op.getValue()+value+RIGHT_BRACKET;
		} else {
			OperationNode opNodes = (OperationNode) node;
			LdapScimOpMap op = LdapScimOpMap.valueOf(opNodes.getOperation());
			search = LEFT_BRACKET+op.getValue() +search+RIGHT_BRACKET;
		}


		return search;
	}
	/*private List<Object> listGroups(Map<String, Boolean> requiredAttributes) throws CharonException, BadRequestException {
		return listGroups(requiredAttributes, null, null, null);
	}*/
	private List<Object> listGroups(Map<String, Boolean> requiredAttributes, String filter, String sortBy, String sortOrder, 
			int startIndex, int count) throws CharonException, BadRequestException {
		List<Object> groupList = new ArrayList<>();
		try {
			LDAPConnection lc = LdapConnectUtil.getConnection(false);
			LDAPSearchResults searchResults =lc.search(LdapConstants.groupContainer, LDAPConnection.SCOPE_ONE, filter, null, false); 
			groupList.add(searchResults.getCount());
			if(sortBy != null) {
				Object sortedSpecial[] = LdapUtil.sort(searchResults, LdapScimGroupMap.valueOf(sortBy).getValue(), sortOrder, startIndex, count);
				for (int j = 0; j < sortedSpecial.length; j++) {
					LDAPEntry nextEntry = (LDAPEntry) sortedSpecial[j];
					groupList.add(parseLdapToGroup(nextEntry));
				}
			} else {
				while (searchResults.hasMore()) {
					LDAPEntry nextEntry = searchResults.next();
					groupList.add(parseLdapToGroup(nextEntry));
				}
			}
			groupList.set(0, groupList.size() - 1);
			return (List<Object>) CopyUtil.deepCopy(groupList);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return  null;
		} catch (LDAPException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return  null;
		}
	}

	private Group parseLdapToGroup(LDAPEntry nextEntry) throws CharonException, BadRequestException, ParseException {
		Group group =new Group();
		LDAPAttributeSet attributeSet = nextEntry.getAttributeSet();
		group.setId(attributeSet.getAttribute(GroupConstants.cn).getStringValue());
		if (attributeSet.getAttribute(GroupConstants.createdDate) != null) {
			group.setCreatedDate(LdapUtil.parseDate(attributeSet.getAttribute(GroupConstants.createdDate).getStringValue()));
		}
		if (attributeSet.getAttribute(GroupConstants.modifiedDate) != null) {
			group.setLastModified(LdapUtil.parseDate(attributeSet.getAttribute(GroupConstants.modifiedDate).getStringValue()));
		}
		if (attributeSet.getAttribute(GroupConstants.location) != null) {
			group.setLocation(attributeSet.getAttribute(GroupConstants.location).getStringValue());
		}
		if (attributeSet.getAttribute(GroupConstants.name) != null) {
			group.setDisplayName(attributeSet.getAttribute(GroupConstants.name).getStringValue());
		}
		if (attributeSet.getAttribute(GroupConstants.member) != null) {
			String[] memIds = attributeSet.getAttribute(GroupConstants.member).getStringValueArray();
			for(String dn :memIds){
				try{
					LDAPConnection lc = LdapConnectUtil.getConnection(false);
					LDAPEntry entry = lc.read(dn);
					LDAPAttributeSet userAttrSet = entry.getAttributeSet();
					String uid, name;
					if (userAttrSet.getAttribute(LdapScimAttrMap.id.getValue()) != null) {
						uid = userAttrSet.getAttribute(LdapScimAttrMap.id.getValue()).getStringValue();
						if (userAttrSet.getAttribute(LdapScimAttrMap.displayName.getValue()) != null) {
							name = userAttrSet.getAttribute(LdapScimAttrMap.displayName.getValue()).getStringValue();
							group.setMember(uid, name);
						}
					}
				}catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		}
		return group;
	}

	@Override
	public Group updateGroup(Group group, Group group1, Map<String, Boolean> map)
			throws NotImplementedException, BadRequestException, CharonException, NotFoundException {
		if (group.getId() == null) {
			throw new NotFoundException("No user with the id : " + group.getId());
		}

		LDAPConnection lc = LdapConnectUtil.getConnection(false);
		String dn = GroupConstants.cn+"="+group.getId()+","+LdapConstants.groupContainer;
		try {
			List<LDAPModification> modList = LdapUpdateHelper.getModifications(group,group1);	
			LDAPModification[] mods = new LDAPModification[modList.size()];
			mods = (LDAPModification[]) modList.toArray(mods);
			lc.modify(dn, mods);
			lc.disconnect();
		}catch (LDAPException e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
			throw new NotFoundException(e.getLDAPErrorMessage());
		}
		return getGroup(group.getId(), map);
	}

	@Override
	public List<Object> listGroupsWithPost(SearchRequest searchRequest, Map<String, Boolean> requiredAttributes)
			throws NotImplementedException, BadRequestException, CharonException {
		return listGroupsWithGET(searchRequest.getFilter(), searchRequest.getStartIndex(), searchRequest.getCount(),
				searchRequest.getSortBy(), searchRequest.getSortOder(), requiredAttributes);
	}
}
