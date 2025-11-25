/*****************************************************************
 * File:  CustomIdentityStoreJPAHelper.java Course materials (23W) CST 8277
 * 
 * @author Teddy Yap
 * @author Mike Norman
 * 
 * Updated by:  Group NN
 *   Lucas, Subhechha, David, Abhiram
 *   
 */
package acmecollege.security;

import static acmecollege.utility.MyConstants.PARAM1;
import static acmecollege.utility.MyConstants.PU_NAME;

import java.util.Set;
import java.util.stream.Collectors;

import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import acmecollege.entity.SecurityRole;
import acmecollege.entity.SecurityUser;

@Singleton
public class CustomIdentityStoreJPAHelper {
    
    private static final Logger LOG = LogManager.getLogger();
    
    @PersistenceContext(name = PU_NAME)
    protected EntityManager em;
    
    /**
     * Find a SecurityUser by username
     * @param username the username to search for
     * @return the SecurityUser if found, null otherwise
     */
    public SecurityUser findUserByName(String username) {
        LOG.debug("find a User By the Name={}", username);
        SecurityUser user = null;
        try {
            TypedQuery<SecurityUser> query = em.createNamedQuery(SecurityUser.USER_FOR_OWNING_STUDENT_QUERY, SecurityUser.class);
            query.setParameter(PARAM1, username);
            user = query.getSingleResult();
        } catch (Exception e) {
            LOG.debug("User not found: {}", username);
        }
        return user;
    }
    
    /**
     * Find all role names for a given username
     * @param username the username to search for
     * @return Set of role names
     */
    public Set<String> findRoleNamesForUser(String username) {
        LOG.debug("find Roles for User={}", username);
        Set<String> roleNames = Set.of();
        SecurityUser user = findUserByName(username);
        if (user != null) {
            roleNames = user.getRoles().stream()
                    .map(SecurityRole::getRoleName)
                    .collect(Collectors.toSet());
        }
        return roleNames;
    }
}
