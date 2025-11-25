/**
 * File:  MembershipCardResource.java Course materials (23W) CST 8277
 *
 * @author Teddy Yap
 * @author Shariar (Shawn) Emami
 * @author (original) Mike Norman
 * 
 * Updated by:  Group NN
 *   Lucas, Subhechha, David, Abhiram
 * 
 */
package acmecollege.rest.resource;

import static acmecollege.utility.MyConstants.ADMIN_ROLE;
import static acmecollege.utility.MyConstants.USER_ROLE;
import static acmecollege.utility.MyConstants.MEMBERSHIP_CARD_RESOURCE_NAME;
import static acmecollege.utility.MyConstants.RESOURCE_PATH_ID_ELEMENT;
import static acmecollege.utility.MyConstants.RESOURCE_PATH_ID_PATH;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.security.enterprise.SecurityContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.soteria.WrappingCallerPrincipal;

import acmecollege.ejb.ACMECollegeService;
import acmecollege.entity.MembershipCard;
import acmecollege.entity.SecurityUser;
import acmecollege.entity.Student;

@Path(MEMBERSHIP_CARD_RESOURCE_NAME)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MembershipCardResource {

    private static final Logger LOG = LogManager.getLogger();

    @EJB
    protected ACMECollegeService service;

    @Inject
    protected SecurityContext sc;

    @GET
    @RolesAllowed({ADMIN_ROLE})
    public Response getAllMembershipCards() {
        LOG.debug("Retrieving all membership cards...");
        List<MembershipCard> membershipCards = service.getAllMembershipCards();
        Response response = Response.ok(membershipCards).build();
        return response;
    }

    @GET
    @RolesAllowed({ADMIN_ROLE, USER_ROLE})
    @Path(RESOURCE_PATH_ID_PATH)
    public Response getMembershipCardById(@PathParam(RESOURCE_PATH_ID_ELEMENT) int cardId) {
        LOG.debug("Retrieving membership card with id = {}", cardId);
        Response response = null;
        MembershipCard membershipCard = service.getMembershipCardById(cardId);
        
        if (membershipCard == null) {
            response = Response.status(Status.NOT_FOUND).build();
        } else if (sc.isCallerInRole(ADMIN_ROLE)) {
            response = Response.ok(membershipCard).build();
        } else if (sc.isCallerInRole(USER_ROLE)) {
            // USER_ROLE can only read their own membership card
            WrappingCallerPrincipal wCallerPrincipal = (WrappingCallerPrincipal) sc.getCallerPrincipal();
            SecurityUser sUser = (SecurityUser) wCallerPrincipal.getWrapped();
            Student student = sUser.getStudent();
            if (student != null && membershipCard.getOwner() != null && 
                student.getId() == membershipCard.getOwner().getId()) {
                response = Response.ok(membershipCard).build();
            } else {
                throw new ForbiddenException("User trying to access resource it does not own");
            }
        } else {
            response = Response.status(Status.BAD_REQUEST).build();
        }
        return response;
    }

    @POST
    @RolesAllowed({ADMIN_ROLE})
    public Response addMembershipCard(MembershipCard newMembershipCard) {
        LOG.debug("Adding a new membership card = {}", newMembershipCard);
        MembershipCard membershipCard = service.persistMembershipCard(newMembershipCard);
        Response response = Response.ok(membershipCard).build();
        return response;
    }

    @PUT
    @RolesAllowed({ADMIN_ROLE})
    @Path(RESOURCE_PATH_ID_PATH)
    public Response updateMembershipCard(@PathParam(RESOURCE_PATH_ID_ELEMENT) int cardId, MembershipCard updatedCard) {
        LOG.debug("Updating membership card with id = {}", cardId);
        MembershipCard membershipCard = service.updateMembershipCardById(cardId, updatedCard);
        Response response = Response.status(membershipCard == null ? Status.NOT_FOUND : Status.OK).entity(membershipCard).build();
        return response;
    }

    @DELETE
    @RolesAllowed({ADMIN_ROLE})
    @Path(RESOURCE_PATH_ID_PATH)
    public Response deleteMembershipCard(@PathParam(RESOURCE_PATH_ID_ELEMENT) int cardId) {
        LOG.debug("Deleting membership card with id = {}", cardId);
        service.deleteMembershipCardById(cardId);
        Response response = Response.noContent().build();
        return response;
    }
}
